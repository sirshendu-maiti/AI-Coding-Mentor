const rateLimit = require('express-rate-limit');

// ─── Per-user AI rate limiter (keyed by Firebase UID) ──────────────────
const aiRateLimiter = rateLimit({
  windowMs: (parseInt(process.env.RATE_LIMIT_AI_WINDOW_MINUTES) || 60) * 60 * 1000,
  max: parseInt(process.env.RATE_LIMIT_AI_MAX) || 30,
  keyGenerator: (req) => req.user?.uid || req.ip,
  standardHeaders: true,
  legacyHeaders: false,
  message: {
    success: false,
    error: {
      code: 'RATE_LIMIT',
      message: 'AI request limit exceeded. Please try again later.',
      retryAfter: parseInt(process.env.RATE_LIMIT_AI_WINDOW_MINUTES) || 60
    }
  }
});

// ─── Global rate limiter (keyed by IP) ─────────────────────────────────
const globalRateLimiter = rateLimit({
  windowMs: (parseInt(process.env.RATE_LIMIT_GLOBAL_WINDOW_MINUTES) || 1) * 60 * 1000,
  max: parseInt(process.env.RATE_LIMIT_GLOBAL_MAX) || 100,
  standardHeaders: true,
  legacyHeaders: false,
  message: {
    success: false,
    error: {
      code: 'RATE_LIMIT',
      message: 'Too many requests. Please slow down.'
    }
  }
});

module.exports = { aiRateLimiter, globalRateLimiter };
