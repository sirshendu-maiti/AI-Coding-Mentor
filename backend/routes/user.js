const express = require('express');
const router = express.Router();
const { body } = require('express-validator');
const { authenticate } = require('../middleware/auth');
const { handleValidation } = require('../middleware/validator');
const { db } = require('../config/firebase');
const { admin } = require('../config/firebase');

router.use(authenticate);

// ─── GET /api/user/profile ─────────────────────────────────────────────
router.get('/profile', async (req, res, next) => {
  try {
    const userRecord = await admin.auth().getUser(req.user.uid);
    res.json({
      success: true,
      data: {
        uid: userRecord.uid,
        displayName: userRecord.displayName || 'User',
        email: userRecord.email || null,
        createdAt: new Date(userRecord.metadata.creationTime).getTime()
      }
    });
  } catch (err) { next(err); }
});

// ─── PUT /api/user/profile ─────────────────────────────────────────────
router.put('/profile',
  [body('displayName').trim().notEmpty().isLength({ min: 1, max: 50 })],
  handleValidation,
  async (req, res, next) => {
    try {
      const { displayName } = req.body;
      await admin.auth().updateUser(req.user.uid, { displayName });
      res.json({ success: true, data: { message: 'Profile updated' } });
    } catch (err) { next(err); }
  }
);

// ─── GET /api/user/progress ────────────────────────────────────────────
router.get('/progress', async (req, res, next) => {
  try {
    if (!db) {
      return res.json({ success: true, data: { totalQuizzes: 0, correctAnswers: 0, streak: 0, languagesUsed: [] } });
    }
    const snapshot = await db.ref(`users/${req.user.uid}/progress`).once('value');
    const progress = snapshot.val() || { totalQuizzes: 0, correctAnswers: 0, streak: 0, languagesUsed: [] };
    res.json({ success: true, data: progress });
  } catch (err) { next(err); }
});

module.exports = router;
