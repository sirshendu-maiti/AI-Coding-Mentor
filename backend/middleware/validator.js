const { body, validationResult } = require('express-validator');
const { ValidationError } = require('../utils/errors');

// ─── Allowed programming languages ────────────────────────────────────
const ALLOWED_LANGUAGES = [
  'Auto Detect', 'Python', 'Java', 'Kotlin', 'JavaScript', 'C', 'C++',
  'TypeScript', 'Go', 'Rust', 'Ruby', 'Swift', 'PHP', 'C#'
];

const MAX_CODE_LENGTH = 10000;
const MAX_MESSAGE_LENGTH = 2000;
const MAX_TOPIC_LENGTH = 200;

// ─── Validation chains ────────────────────────────────────────────────

const validateCode = [
  body('code')
    .trim()
    .notEmpty().withMessage('Code is required')
    .isLength({ max: MAX_CODE_LENGTH }).withMessage(`Code must be under ${MAX_CODE_LENGTH} characters`),
  body('language')
    .trim()
    .notEmpty().withMessage('Language is required')
    .isIn(ALLOWED_LANGUAGES).withMessage('Unsupported language')
];

const validateChat = [
  body('message')
    .trim()
    .notEmpty().withMessage('Message is required')
    .isLength({ max: MAX_MESSAGE_LENGTH }).withMessage(`Message must be under ${MAX_MESSAGE_LENGTH} characters`),
  body('history')
    .optional()
    .isArray({ max: 20 }).withMessage('History must be an array with max 20 messages'),
  body('currentCode')
    .optional()
    .isString()
    .isLength({ max: MAX_CODE_LENGTH })
];

const validateRoadmap = [
  body('topic')
    .trim()
    .notEmpty().withMessage('Topic is required')
    .isLength({ max: MAX_TOPIC_LENGTH }).withMessage(`Topic must be under ${MAX_TOPIC_LENGTH} characters`),
  body('level')
    .trim()
    .isIn(['Beginner', 'Intermediate', 'Advanced']).withMessage('Level must be Beginner, Intermediate, or Advanced')
];

const validateExplain = [
  ...validateCode,
  body('beginnerMode')
    .optional()
    .isBoolean().withMessage('beginnerMode must be a boolean')
];

// ─── Validation result handler ─────────────────────────────────────────
function handleValidation(req, res, next) {
  const errors = validationResult(req);
  if (!errors.isEmpty()) {
    const details = errors.array().map(e => ({ field: e.path, message: e.msg }));
    const err = new ValidationError('Invalid input', details);
    return res.status(err.statusCode).json({
      success: false,
      error: { code: err.code, message: err.message, details: err.details }
    });
  }
  next();
}

module.exports = {
  validateCode,
  validateChat,
  validateRoadmap,
  validateExplain,
  handleValidation,
  ALLOWED_LANGUAGES
};
