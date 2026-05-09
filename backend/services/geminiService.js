const axios = require('axios');
const PROMPTS = require('../utils/prompts');
const { AiError } = require('../utils/errors');

// ─── OpenRouter Configuration ───────────────────────────────────────
const OPENROUTER_BASE_URL = 'https://openrouter.ai/api/v1';
const MODEL = 'inclusionai/ling-2.6-1t:free';
const API_KEY = process.env.OPENROUTER_API_KEY;

/**
 * Core function to call the OpenRouter API.
 * Uses the Ling-2.6-1T model (free tier).
 */
async function callAi(systemPrompt, userMessage, history = []) {
  if (!API_KEY || API_KEY === 'sk-or-v1-...') {
    throw new AiError('OpenRouter API key not configured on server. Please check .env file.');
  }

  const messages = [
    { role: 'system', content: systemPrompt }
  ];

  // Add conversation history
  if (history.length > 0) {
    history.slice(-10).forEach(msg => {
      messages.push({
        role: msg.role === 'USER' ? 'user' : 'assistant',
        content: msg.content
      });
    });
  }

  // Add user message
  messages.push({ role: 'user', content: userMessage });

  try {
    const response = await axios.post(
      `${OPENROUTER_BASE_URL}/chat/completions`,
      {
        model: MODEL,
        messages: messages,
        temperature: 0.7,
        max_tokens: 4096,
        top_p: 0.9,
      },
      {
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${API_KEY}`,
          'HTTP-Referer': 'https://github.com/euphoria/aimentor',
          'X-Title': 'AICodingMentor'
        },
        timeout: 90000 // 90 seconds
      }
    );

    if (response.data.error) {
      throw new AiError(`OpenRouter Error: ${response.data.error.message}`);
    }

    const content = response.data?.choices?.[0]?.message?.content;
    if (!content) {
      throw new AiError('AI returned an empty response.');
    }

    return content;
  } catch (err) {
    if (err instanceof AiError) throw err;

    const status = err.response?.status;
    const errorMsg = err.response?.data?.error?.message || err.message;

    if (status === 429) throw new AiError('Rate limit reached on OpenRouter. Try again in a minute.');
    if (status === 401) throw new AiError('Invalid OpenRouter API Key.');

    throw new AiError(`AI Service Error: ${errorMsg}`);
  }
}

// ─── Helpers ────────────────────────────────────────────────────────

function parseJsonResponse(text) {
  let clean = text.trim();
  const jsonBlock = clean.match(/```(?:json)?\s*([\s\S]*?)```/);
  if (jsonBlock) clean = jsonBlock[1].trim();

  try {
    return JSON.parse(clean);
  } catch (e) {
    const startObj = clean.indexOf('{');
    const startArr = clean.indexOf('[');
    const start = (startObj !== -1 && (startArr === -1 || startObj < startArr)) ? startObj : startArr;

    if (start !== -1) {
      const isArray = clean[start] === '[';
      const end = clean.lastIndexOf(isArray ? ']' : '}');
      if (end > start) {
        try {
          return JSON.parse(clean.substring(start, end + 1));
        } catch (_) {}
      }
    }
    throw new AiError('Failed to parse AI response as JSON.');
  }
}

// ─── Public Methods ──────────────────────────────────────────────────

async function analyzeCode(code, language) {
  return await callAi(PROMPTS.analyzeCode, `Code:\n\`\`\`${language}\n${code}\n\`\`\``);
}

async function explainCode(code, language, beginnerMode = false) {
  const level = beginnerMode ? 'beginner' : 'intermediate';
  return await callAi(PROMPTS.explainCode(level), `Explain this ${language} code:\n\`\`\`${language}\n${code}\n\`\`\``);
}

async function analyzeComplexity(code, language) {
  return await callAi(PROMPTS.analyzeComplexity, `Analyze complexity of this ${language} code:\n\`\`\`${language}\n${code}\n\`\`\``);
}

async function generateTestCases(code, language) {
  return await callAi(PROMPTS.generateTestCases(language), `Generate tests for:\n\`\`\`${language}\n${code}\n\`\`\``);
}

async function generateQuiz(code, language) {
  const raw = await callAi(PROMPTS.generateQuiz, `Generate 3 quiz questions for this ${language} code:\n\`\`\`${language}\n${code}\n\`\`\``);
  return parseJsonResponse(raw);
}

async function generateRoadmap(topic, level) {
  const raw = await callAi(PROMPTS.generateRoadmap, `Create a ${level} roadmap for learning ${topic}.`);
  return parseJsonResponse(raw);
}

async function generateInterviewQuestions(code, language) {
  return await callAi(PROMPTS.generateInterviewQuestions, `Interview questions for:\n\`\`\`${language}\n${code}\n\`\`\``);
}

async function chat(message, history = [], currentCode = null) {
  return await callAi(PROMPTS.chat(currentCode), message, history);
}

async function generateDailyChallenge() {
  const raw = await callAi(PROMPTS.dailyChallenge, "Generate a new daily coding challenge.");
  return parseJsonResponse(raw);
}

module.exports = {
  analyzeCode,
  explainCode,
  analyzeComplexity,
  generateTestCases,
  generateQuiz,
  generateRoadmap,
  generateInterviewQuestions,
  chat,
  generateDailyChallenge
};
