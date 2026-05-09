const express = require('express');
const router = express.Router();
const geminiService = require('../services/geminiService');
const { authenticate } = require('../middleware/auth');
const { aiRateLimiter } = require('../middleware/rateLimiter');
const {
  validateCode, validateChat, validateRoadmap, validateExplain,
  handleValidation
} = require('../middleware/validator');

// All AI routes require authentication + rate limiting
router.use(authenticate);
router.use(aiRateLimiter);

// ─── POST /api/ai/analyze ──────────────────────────────────────────────
router.post('/analyze', validateCode, handleValidation, async (req, res, next) => {
  try {
    const { code, language } = req.body;
    const result = await geminiService.analyzeCode(code, language);
    res.json({ success: true, data: { result } });
  } catch (err) { next(err); }
});

// ─── POST /api/ai/explain ──────────────────────────────────────────────
router.post('/explain', validateExplain, handleValidation, async (req, res, next) => {
  try {
    const { code, language, beginnerMode } = req.body;
    const result = await geminiService.explainCode(code, language, beginnerMode);
    res.json({ success: true, data: { result } });
  } catch (err) { next(err); }
});

// ─── POST /api/ai/complexity ───────────────────────────────────────────
router.post('/complexity', validateCode, handleValidation, async (req, res, next) => {
  try {
    const { code, language } = req.body;
    const result = await geminiService.analyzeComplexity(code, language);
    res.json({ success: true, data: { result } });
  } catch (err) { next(err); }
});

// ─── POST /api/ai/tests ───────────────────────────────────────────────
router.post('/tests', validateCode, handleValidation, async (req, res, next) => {
  try {
    const { code, language } = req.body;
    const result = await geminiService.generateTestCases(code, language);
    res.json({ success: true, data: { result } });
  } catch (err) { next(err); }
});

// ─── POST /api/ai/quiz ────────────────────────────────────────────────
router.post('/quiz', validateCode, handleValidation, async (req, res, next) => {
  try {
    const { code, language } = req.body;
    const questions = await geminiService.generateQuiz(code, language);
    res.json({ success: true, data: { questions } });
  } catch (err) { next(err); }
});

// ─── POST /api/ai/roadmap ─────────────────────────────────────────────
router.post('/roadmap', validateRoadmap, handleValidation, async (req, res, next) => {
  try {
    const { topic, level } = req.body;
    const roadmap = await geminiService.generateRoadmap(topic, level);
    res.json({ success: true, data: { roadmap } });
  } catch (err) { next(err); }
});

// ─── POST /api/ai/interview ───────────────────────────────────────────
router.post('/interview', validateCode, handleValidation, async (req, res, next) => {
  try {
    const { code, language } = req.body;
    const result = await geminiService.generateInterviewQuestions(code, language);
    res.json({ success: true, data: { result } });
  } catch (err) { next(err); }
});

// ─── POST /api/ai/chat ────────────────────────────────────────────────
router.post('/chat', validateChat, handleValidation, async (req, res, next) => {
  try {
    const { message, history, currentCode } = req.body;
    const reply = await geminiService.chat(message, history || [], currentCode);
    res.json({ success: true, data: { reply } });
  } catch (err) { next(err); }
});

module.exports = router;
