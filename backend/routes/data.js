const express = require('express');
const router = express.Router();
const { body, param } = require('express-validator');
const { authenticate } = require('../middleware/auth');
const { handleValidation } = require('../middleware/validator');
const { db } = require('../config/firebase');
const { getOrGenerateChallenge } = require('../services/dailyChallengeService');

router.use(authenticate);

// ─── GET /api/data/history ─────────────────────────────────────────────
router.get('/history', async (req, res, next) => {
  try {
    if (!db) return res.json({ success: true, data: [] });

    const snapshot = await db.ref(`users/${req.user.uid}/history`)
      .orderByChild('timestamp')
      .limitToLast(100)
      .once('value');

    const items = [];
    snapshot.forEach(child => { items.push(child.val()); });
    items.reverse(); // newest first

    res.json({ success: true, data: items });
  } catch (err) { next(err); }
});

// ─── POST /api/data/history ────────────────────────────────────────────
router.post('/history',
  [
    body('id').trim().notEmpty(),
    body('code').trim().notEmpty().isLength({ max: 1000 }),
    body('language').trim().notEmpty(),
    body('summary').trim().isLength({ max: 200 }),
    body('hasErrors').isBoolean()
  ],
  handleValidation,
  async (req, res, next) => {
    try {
      if (!db) return res.json({ success: true, data: { message: 'Saved locally' } });

      const item = {
        id: req.body.id,
        code: req.body.code.substring(0, 500),
        language: req.body.language,
        summary: req.body.summary || '',
        timestamp: Date.now(),
        hasErrors: req.body.hasErrors
      };

      await db.ref(`users/${req.user.uid}/history/${item.id}`).set(item);
      res.json({ success: true, data: item });
    } catch (err) { next(err); }
  }
);

// ─── DELETE /api/data/history/:id ──────────────────────────────────────
router.delete('/history/:id',
  [param('id').trim().notEmpty()],
  handleValidation,
  async (req, res, next) => {
    try {
      if (!db) return res.json({ success: true });
      await db.ref(`users/${req.user.uid}/history/${req.params.id}`).remove();
      res.json({ success: true, data: { message: 'Deleted' } });
    } catch (err) { next(err); }
  }
);

// ─── GET /api/data/leaderboard ─────────────────────────────────────────
router.get('/leaderboard', async (req, res, next) => {
  try {
    if (!db) return res.json({ success: true, data: [] });

    const snapshot = await db.ref('leaderboard')
      .orderByChild('score')
      .limitToLast(50)
      .once('value');

    const entries = [];
    snapshot.forEach(child => { entries.push(child.val()); });
    entries.reverse();

    // Add rank
    const ranked = entries.map((entry, index) => ({ ...entry, rank: index + 1 }));
    res.json({ success: true, data: ranked });
  } catch (err) { next(err); }
});

// ─── GET /api/data/challenge ───────────────────────────────────────────
router.get('/challenge', async (req, res, next) => {
  try {
    const challenge = await getOrGenerateChallenge();
    res.json({ success: true, data: challenge });
  } catch (err) { next(err); }
});

module.exports = router;
