// ─── Custom Error Classes ──────────────────────────────────────────────
class AppError extends Error {
  constructor(message, statusCode = 500, code = 'INTERNAL_ERROR') {
    super(message);
    this.statusCode = statusCode;
    this.code = code;
    this.isOperational = true;
  }
}

class ValidationError extends AppError {
  constructor(message, details = []) {
    super(message, 400, 'VALIDATION_ERROR');
    this.details = details;
  }
}

class AuthError extends AppError {
  constructor(message = 'Authentication required') {
    super(message, 401, 'AUTH_ERROR');
  }
}

class RateLimitError extends AppError {
  constructor(retryAfter = 60) {
    super(`Rate limit exceeded. Try again in ${retryAfter} seconds.`, 429, 'RATE_LIMIT');
    this.retryAfter = retryAfter;
  }
}

class AiError extends AppError {
  constructor(message = 'AI service temporarily unavailable') {
    super(message, 502, 'AI_ERROR');
  }
}

// ─── Error Response Formatter ──────────────────────────────────────────
function formatErrorResponse(err) {
  return {
    success: false,
    error: {
      code: err.code || 'INTERNAL_ERROR',
      message: err.isOperational ? err.message : 'An unexpected error occurred',
      ...(err.details && { details: err.details }),
      ...(err.retryAfter && { retryAfter: err.retryAfter })
    }
  };
}

module.exports = { AppError, ValidationError, AuthError, RateLimitError, AiError, formatErrorResponse };
