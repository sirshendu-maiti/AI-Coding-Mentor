require('dotenv').config();

const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const compression = require('compression');
const morgan = require('morgan');

// ─── Load Middleware & Services ───────────────────────────────────────
let globalRateLimiter;
try {
  const { globalRateLimiter: limiter } = require('./middleware/rateLimiter');
  globalRateLimiter = limiter;
} catch (err) {
  console.warn('⚠️  Rate limiter initialization failed:', err.message);
  globalRateLimiter = (req, res, next) => next();
}

let scheduleChallenge;
try {
  const { scheduleChallenge: schedule } = require('./services/dailyChallengeService');
  scheduleChallenge = schedule;
} catch (err) {
  console.warn('⚠️  Daily challenge service failed:', err.message);
  scheduleChallenge = () => {};
}

const { formatErrorResponse } = require('./utils/errors');

// ─── Routes ──────────────────────────────────────────────────────────
const aiRoutes = require('./routes/ai');
const userRoutes = require('./routes/user');
const dataRoutes = require('./routes/data');

const app = express();
const PORT = process.env.PORT || 8080;

// ─── Security & Parsing ───────────────────────────────────────────────
app.use(helmet());
app.use(cors({
  origin: [
    'https://aicodingmentor-f1077.web.app',
    'https://aicodingmentor-f1077.firebaseapp.com',
    'http://localhost:3000',
    'http://localhost:5173'
  ],
  methods: ['GET', 'POST'],
  allowedHeaders: ['Content-Type', 'Authorization'],
  credentials: true,
}));
app.use(compression());
app.use(express.json({ limit: '1mb' }));
app.use(express.urlencoded({ extended: false }));

// ─── Logging ──────────────────────────────────────────────────────────
if (process.env.NODE_ENV !== 'test') {
  app.use(morgan(process.env.NODE_ENV === 'production' ? 'combined' : 'dev'));
}

// ─── Global Rate Limit ───────────────────────────────────────────────
app.use(globalRateLimiter);

// ─── Health Check ─────────────────────────────────────────────────────
app.get('/api/health', (req, res) => {
  res.json({
    success: true,
    data: {
      status: 'healthy',
      version: '1.0.0',
      timestamp: new Date().toISOString(),
      uptime: Math.floor(process.uptime()) + 's'
    }
  });
});

// ─── Readiness Check ──────────────────────────────────────────────────
app.get('/api/ready', (req, res) => {
  res.json({
    success: true,
    data: {
      ready: true,
      timestamp: new Date().toISOString()
    }
  });
});

// ─── API Routes ───────────────────────────────────────────────────────
app.use('/api/ai', aiRoutes);
app.use('/api/user', userRoutes);
app.use('/api/data', dataRoutes);

// ─── 404 Handler ────────────��─────────────────────────────────────────
app.use((req, res) => {
  res.status(404).json({
    success: false,
    error: { code: 'NOT_FOUND', message: `Route ${req.method} ${req.path} not found` }
  });
});

// ─── Global Error Handler ─────────────────────────────────────────────
app.use((err, req, res, _next) => {
  // Log the full error in development
  if (process.env.NODE_ENV !== 'production') {
    console.error('❌ Error:', err);
  } else {
    console.error('❌ Error:', err.message);
  }

  const statusCode = err.statusCode || err.status || 500;
  const response = err.isOperational
    ? formatErrorResponse(err)
    : { success: false, error: { code: 'INTERNAL_ERROR', message: 'An unexpected error occurred' } };

  res.status(statusCode).json(response);
});

// ─── Start Server ─────────────────────────────────────────────────────
const server = app.listen(PORT, () => {
  console.log(`
╔═══════════════════════════════════════════════╗
║   🚀 AI Coding Mentor Backend                ║
║   Running on port ${PORT}                       ║
║   Environment: ${process.env.NODE_ENV || 'development'}               ║
║   Health: http://localhost:${PORT}/api/health    ║
║   Ready: http://localhost:${PORT}/api/ready      ║
╚═══════════════════════════════════════════════╝
  `);

  // Start daily challenge scheduler (with error handling)
  try {
    scheduleChallenge();
    console.log('✅ Daily challenge scheduler started');
  } catch (err) {
    console.warn('⚠️  Challenge scheduler error:', err.message);
  }
});

// ─── Graceful Shutdown ────────────────────────────────────────────────
function shutdown(signal) {
  console.log(`\n⏹️  ${signal} received. Shutting down gracefully...`);
  server.close(() => {
    console.log('✅ Server closed');
    process.exit(0);
  });
  // Force exit after 10s
  setTimeout(() => { process.exit(1); }, 10000);
}

process.on('SIGTERM', () => shutdown('SIGTERM'));
process.on('SIGINT', () => shutdown('SIGINT'));
process.on('unhandledRejection', (reason) => {
  console.error('⚠️  Unhandled Rejection:', reason);
});
