const { onRequest } = require("firebase-functions/v2/https");
const { onSchedule } = require("firebase-functions/v2/scheduler");
const { defineSecret } = require("firebase-functions/params");
const { logger } = require("firebase-functions");
const admin = require("firebase-admin");
const express = require("express");
const cors = require("cors");
const helmet = require("helmet");
const compression = require("compression");
const axios = require("axios");

// ─── Firebase Admin ──────────────────────────────────────────────────
admin.initializeApp();
const db = admin.database();

// ─── Secrets ──────────────────────────────────────────────────────────
// Set via: firebase functions:secrets:set OPENROUTER_API_KEY
const openRouterApiKey = defineSecret("OPENROUTER_API_KEY");

// ═══════════════════════════════════════════════════════════════════════
// OPENROUTER SERVICE (Auto-routed Free Model)
// ═══════════════════════════════════════════════════════════════════════

const OPENROUTER_BASE_URL = "https://openrouter.ai/api/v1";
const MODEL = "openrouter/free";

const PROMPTS = {
  analyzeCode: `You are an expert coding mentor and debugger. Analyze code and provide:
1. **Error Detection**: List all syntax and logical errors with line numbers
2. **Explanation**: Explain what the code does in simple terms
3. **Optimized Code**: Provide a corrected and optimized version
4. **Key Improvements**: Bullet points of what was improved
Format your response clearly with sections. Use code blocks for code. Be educational and beginner-friendly.`,

  explainCode: (level) => `You are a patient coding teacher.
Explain code step-by-step as if teaching ${level === "beginner" ? "a complete beginner" : "an intermediate developer"}.
Use simple analogies, numbered steps, and examples. Use emojis where appropriate.`,

  analyzeComplexity: `You are an algorithms expert. Analyze the time and space complexity of code.
Provide: Time Complexity (Big O), Space Complexity (Big O), Best/Worst Case, and Optimization Tips. Be precise and educational.`,

  generateTestCases: (lang) => `You are a QA expert. Generate comprehensive test cases in ${lang} including normal, edge, and error cases. Write actual test code.`,

  generateQuiz: `You are a coding quiz generator. Based on the provided code, generate exactly 3 multiple choice questions.
Return ONLY valid JSON array with no markdown, no backticks:
[{"id":"q1","question":"...","options":[{"label":"A","text":"...","isCorrect":false},{"label":"B","text":"...","isCorrect":true},{"label":"C","text":"...","isCorrect":false},{"label":"D","text":"...","isCorrect":false}],"explanation":"..."}]
Return ONLY the JSON array.`,

  generateRoadmap: `You are an educational architect. Create a structured learning roadmap.
Return ONLY valid JSON with no markdown, no backticks:
{"topic":"...","level":"...","steps":[{"id":"step1","title":"...","description":"...","resources":["..."]}]}
Generate 5-7 steps. Return ONLY the JSON object.`,

  generateInterviewQuestions: `You are a senior software engineer. Generate 5 realistic interview questions covering conceptual understanding, complexity, edge cases, optimization, and related concepts.`,

  chat: (codeCtx) => `You are a friendly expert AI Coding Mentor. Help users understand code, fix bugs, and learn programming. Be conversational and educational. Use code blocks with language tags.${codeCtx ? `\n\nCurrent code context:\n\`\`\`\n${codeCtx}\n\`\`\`` : ""}`,

  dailyChallenge: `You are a coding challenge creator. Return ONLY valid JSON:
{"id":"...","title":"...","description":"...","difficulty":"Easy|Medium|Hard","baseCode":"...","expectedOutput":"..."}`
};

// ─── Input Sanitization (Rule 3 & Rule 🤖: Prompt Injection Prevention) ──
function sanitizeForLLM(input) {
  if (typeof input !== "string") return "";
  // Strip attempts to break out of the prompt context
  return input
    .replace(/```/g, "\u0060\u0060\u0060") // neutralize code fence injections
    .replace(/<\/?script[^>]*>/gi, "")      // strip script tags
    .replace(/\{\{[^}]*\}\}/g, "")          // strip template injections
    .slice(0, 12000);                        // hard cap input length
}

async function callOpenRouter(apiKey, systemPrompt, userMessage, history = [], userId = "unknown") {
  const messages = [];
  messages.push({ role: "system", content: systemPrompt });

  if (history.length > 0) {
    history.slice(-10).forEach((msg) => {
      messages.push({
        role: msg.role === "USER" ? "user" : "assistant",
        content: sanitizeForLLM(msg.content),
      });
    });
  }
  messages.push({ role: "user", content: sanitizeForLLM(userMessage) });

  try {
    const res = await axios.post(
      `${OPENROUTER_BASE_URL}/chat/completions`,
      {
        model: MODEL,
        messages: messages,
        temperature: 0.7,
        max_tokens: 4096,
        top_p: 0.9
      },
      {
        headers: {
          "Content-Type": "application/json",
          "Authorization": `Bearer ${apiKey}`,
          "HTTP-Referer": "https://github.com/euphoria/aimentor",
          "X-Title": "AICodingMentor"
        },
        timeout: 90000
      }
    );

    const text = res.data?.choices?.[0]?.message?.content;
    if (!text) throw new Error("Empty AI response");

    // Rule 🤖: Log token usage per user for abuse detection
    const usage = res.data?.usage;
    if (usage) {
      logger.info("LLM_USAGE", { userId, promptTokens: usage.prompt_tokens, completionTokens: usage.completion_tokens, totalTokens: usage.total_tokens });
    }

    return text;
  } catch (error) {
    // Rule 9: Never leak raw API error details to the client
    const status = error.response?.status;
    if (status === 429) throw new Error("Rate limit exceeded. Please try again later.");
    if (status === 401 || status === 403) throw new Error("AI service authentication error.");
    logger.error("OpenRouter call failed", { status, message: error.message, userId });
    throw new Error("AI service temporarily unavailable.");
  }
}

function parseJsonResponse(text) {
  let clean = text.trim();
  const m = clean.match(/```(?:json)?\s*\n?([\s\S]*?)\n?```/);
  if (m) clean = m[1].trim();
  else clean = clean.replace(/^```json\s*/i, "").replace(/^```\s*/i, "").replace(/\s*```$/i, "").trim();
  try { return JSON.parse(clean); } catch (_) {
    const s1 = clean.indexOf("["), s2 = clean.indexOf("{");
    const s = Math.min(s1 >= 0 ? s1 : Infinity, s2 >= 0 ? s2 : Infinity);
    if (s !== Infinity) {
      const isArr = clean[s] === "[";
      const e = clean.lastIndexOf(isArr ? "]" : "}");
      if (e > s) try { return JSON.parse(clean.substring(s, e + 1)); } catch (__) { }
    }
    throw new Error("AI returned invalid format");
  }
}

// ─── Auth Middleware ──────────────────────────────────────────────────
async function authenticate(req, res, next) {
  const auth = req.headers.authorization;
  if (!auth || !auth.startsWith("Bearer ")) {
    return res.status(401).json({ success: false, error: { code: "AUTH_ERROR", message: "Missing auth token" } });
  }
  try {
    const token = auth.split("Bearer ")[1];
    const decoded = await admin.auth().verifyIdToken(token);
    req.user = { uid: decoded.uid, email: decoded.email };
    next();
  } catch (err) {
    return res.status(401).json({ success: false, error: { code: "AUTH_ERROR", message: "Invalid token" } });
  }
}

// ─── Validation ───────────────────────────────────────────────────────
const { body, validationResult } = require("express-validator");

const LANGUAGES = ["Auto Detect", "Python", "Java", "Kotlin", "JavaScript", "C", "C++", "TypeScript", "Go", "Rust", "Ruby", "Swift", "PHP", "C#"];

const validateCode = [
  body("code").trim().notEmpty().isLength({ max: 10000 }),
  body("language").trim().notEmpty().isIn(LANGUAGES),
];
const validateChat = [
  body("message").trim().notEmpty().isLength({ max: 2000 }),
  body("history").optional({ nullable: true }).isArray({ max: 20 }),
  body("currentCode").optional({ nullable: true }).isString().isLength({ max: 10000 }),
];
const validateRoadmap = [
  body("topic").trim().notEmpty().isLength({ max: 200 }),
  body("level").trim().isIn(["Beginner", "Intermediate", "Advanced"]),
];

function handleValidation(req, res, next) {
  const errors = validationResult(req);
  if (!errors.isEmpty()) {
    return res.status(400).json({
      success: false,
      error: { code: "VALIDATION_ERROR", message: "Invalid input", details: errors.array().map((e) => ({ field: e.path, message: e.msg })) },
    });
  }
  next();
}

// ─── Rate Limiters (Rule 2) ───────────────────────────────────────────
const rateLimit = require("express-rate-limit");

// AI endpoints: 10 requests per minute per user (Rule 2: AI/LLM proxy)
const aiLimiter = rateLimit({
  windowMs: 60 * 1000,
  max: 10,
  keyGenerator: (req) => req.user?.uid || req.ip,
  standardHeaders: true,
  legacyHeaders: false,
  handler: (req, res) => {
    res.set("Retry-After", "60");
    res.status(429).json({ success: false, error: { code: "RATE_LIMIT", message: "Too many AI requests. Please wait 1 minute." } });
  },
});

// Auth/data endpoints: 60 requests per minute per IP (Rule 2: General API)
const generalLimiter = rateLimit({
  windowMs: 60 * 1000,
  max: 60,
  keyGenerator: (req) => req.user?.uid || req.ip,
  standardHeaders: true,
  legacyHeaders: false,
  handler: (req, res) => {
    res.set("Retry-After", "60");
    res.status(429).json({ success: false, error: { code: "RATE_LIMIT", message: "Too many requests. Please slow down." } });
  },
});

// ─── Allowed Origins (Rule 6: No wildcard CORS in production) ─────────
const ALLOWED_ORIGINS = [
  "https://aicodingmentor-f1077.web.app",
  "https://aicodingmentor-f1077.firebaseapp.com",
];

// ─── Express App ──────────────────────────────────────────────────────
const app = express();
app.use(helmet());                              // Rule 7: Security headers
app.use(cors({
  origin: (origin, callback) => {
    // Allow requests with no origin (mobile apps, Postman) and whitelisted origins
    if (!origin || ALLOWED_ORIGINS.includes(origin)) {
      callback(null, true);
    } else {
      callback(new Error("CORS policy: origin not allowed"));
    }
  },
  methods: ["GET", "POST"],
  allowedHeaders: ["Content-Type", "Authorization"],
  credentials: true,
}));
app.use(compression());
app.use(express.json({ limit: "1mb" }));
app.use(generalLimiter);                        // Rule 2: Global rate limit

app.get("/api/health", (req, res) => {
  res.json({ success: true, data: { status: "healthy", version: "2.1.0-openrouter", timestamp: new Date().toISOString() } });
});

app.use("/api/ai", authenticate, aiLimiter);

app.post("/api/ai/analyze", validateCode, handleValidation, async (req, res) => {
  try {
    const result = await callOpenRouter(openRouterApiKey.value(), PROMPTS.analyzeCode, `Language: ${req.body.language}\n\nAnalyze:\n\`\`\`\n${req.body.code}\n\`\`\``, [], req.user?.uid);
    res.json({ success: true, data: { result } });
  } catch (e) { res.status(502).json({ success: false, error: { code: "AI_ERROR", message: e.message } }); }
});

app.post("/api/ai/explain", validateCode, handleValidation, async (req, res) => {
  try {
    const level = req.body.beginnerMode ? "beginner" : "intermediate";
    const result = await callOpenRouter(openRouterApiKey.value(), PROMPTS.explainCode(level), `Explain this ${req.body.language} code:\n\`\`\`\n${req.body.code}\n\`\`\``);
    res.json({ success: true, data: { result } });
  } catch (e) { res.status(502).json({ success: false, error: { code: "AI_ERROR", message: e.message } }); }
});

app.post("/api/ai/complexity", validateCode, handleValidation, async (req, res) => {
  try {
    const result = await callOpenRouter(openRouterApiKey.value(), PROMPTS.analyzeComplexity, `Analyze complexity:\n\`\`\`\n${req.body.code}\n\`\`\``);
    res.json({ success: true, data: { result } });
  } catch (e) { res.status(502).json({ success: false, error: { code: "AI_ERROR", message: e.message } }); }
});

app.post("/api/ai/tests", validateCode, handleValidation, async (req, res) => {
  try {
    const result = await callOpenRouter(openRouterApiKey.value(), PROMPTS.generateTestCases(req.body.language), `Generate tests:\n\`\`\`\n${req.body.code}\n\`\`\``);
    res.json({ success: true, data: { result } });
  } catch (e) { res.status(502).json({ success: false, error: { code: "AI_ERROR", message: e.message } }); }
});

app.post("/api/ai/quiz", validateCode, handleValidation, async (req, res) => {
  try {
    const raw = await callOpenRouter(openRouterApiKey.value(), PROMPTS.generateQuiz, `Quiz for:\n\`\`\`\n${req.body.code}\n\`\`\``);
    const questions = parseJsonResponse(raw);
    res.json({ success: true, data: { questions } });
  } catch (e) { res.status(502).json({ success: false, error: { code: "AI_ERROR", message: e.message } }); }
});

app.post("/api/ai/roadmap", validateRoadmap, handleValidation, async (req, res) => {
  try {
    const raw = await callOpenRouter(openRouterApiKey.value(), PROMPTS.generateRoadmap, `Create a ${req.body.level} roadmap for ${req.body.topic}.`);
    const roadmap = parseJsonResponse(raw);
    res.json({ success: true, data: { roadmap } });
  } catch (e) { res.status(502).json({ success: false, error: { code: "AI_ERROR", message: e.message } }); }
});

app.post("/api/ai/interview", validateCode, handleValidation, async (req, res) => {
  try {
    const result = await callOpenRouter(openRouterApiKey.value(), PROMPTS.generateInterviewQuestions, `Interview questions:\n\`\`\`\n${req.body.code}\n\`\`\``);
    res.json({ success: true, data: { result } });
  } catch (e) { res.status(502).json({ success: false, error: { code: "AI_ERROR", message: e.message } }); }
});

app.post("/api/ai/chat", validateChat, handleValidation, async (req, res) => {
  try {
    const reply = await callOpenRouter(openRouterApiKey.value(), PROMPTS.chat(req.body.currentCode), req.body.message, req.body.history || [], req.user?.uid);
    res.json({ success: true, data: { reply } });
  } catch (e) { res.status(502).json({ success: false, error: { code: "AI_ERROR", message: e.message } }); }
});

// ─── Data & User Routes ──────────────────────────────────────────────
app.use("/api/data", authenticate);
app.get("/api/data/leaderboard", async (req, res) => {
  try {
    const snap = await db.ref("leaderboard").orderByChild("score").limitToLast(50).once("value");
    const entries = []; snap.forEach((c) => entries.push(c.val())); entries.reverse();
    res.json({ success: true, data: entries.map((e, i) => ({ ...e, rank: i + 1 })) });
  } catch (e) {
    logger.error("Leaderboard fetch failed", { message: e.message });
    res.status(500).json({ success: false, error: { code: "DB_ERROR", message: "Failed to load leaderboard." } });
  }
});

app.get("/api/data/challenge", async (req, res) => {
  try {
    const snap = await db.ref("daily_challenge").once("value");
    res.json({ success: true, data: snap.val() });
  } catch (e) {
    logger.error("Challenge fetch failed", { message: e.message });
    res.status(500).json({ success: false, error: { code: "DB_ERROR", message: "Failed to load daily challenge." } });
  }
});

app.use("/api/user", authenticate);
app.get("/api/user/profile", async (req, res) => {
  try {
    const u = await admin.auth().getUser(req.user.uid);
    res.json({ success: true, data: { uid: u.uid, displayName: u.displayName || "User", email: u.email } });
  } catch (e) {
    logger.error("Profile fetch failed", { uid: req.user?.uid, message: e.message });
    res.status(500).json({ success: false, error: { code: "DB_ERROR", message: "Failed to load profile." } });
  }
});

// ─── Export ───────────────────────────────────────────────────────────
exports.api = onRequest({
  secrets: [openRouterApiKey],
  timeoutSeconds: 120,
  memory: "512MiB",
  region: "us-central1",
}, app);
exports.cleanupOldChats = onSchedule({
  schedule: "every 24 hours",
  timeoutSeconds: 300,
  memory: "256MiB",
  region: "us-central1",
}, async () => {
  const sevenDaysAgo = Date.now() - 7 * 24 * 60 * 60 * 1000;
  try {
    const usersSnap = await db.ref("users").once("value");
    const promises = [];
    usersSnap.forEach((userSnap) => {
      const chatsRef = db.ref(`users/${userSnap.key}/chats`);
      promises.push(chatsRef.orderByChild("timestamp").endAt(sevenDaysAgo).once("value").then(old => {
        const updates = {};
        old.forEach(chat => { updates[chat.key] = null; });
        if (Object.keys(updates).length > 0) return chatsRef.update(updates);
      }));
    });
    await Promise.all(promises);
  } catch (err) { console.error("Chat cleanup failed:", err.message); }
});


exports.generateDailyChallenge = onSchedule({
  schedule: "every day 00:00",
  secrets: [openRouterApiKey],
  timeoutSeconds: 120,
  memory: "256MiB",
  region: "us-central1",
}, async () => {
  try {
    const today = new Date().toISOString().split("T")[0].replace(/-/g, "");
    const raw = await callOpenRouter(openRouterApiKey.value(), PROMPTS.dailyChallenge, "Generate a coding challenge.");
    const challenge = parseJsonResponse(raw);
    challenge.id = `challenge_${today}`;
    await db.ref("daily_challenge").set(challenge);
  } catch (err) { console.error("Challenge failed:", err.message); }
});
