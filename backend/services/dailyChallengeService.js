const { db } = require('../config/firebase');
const geminiService = require('./geminiService');

// ─── Daily Challenge Service ───────────────────────────────────────────
// Generates and caches a new challenge every 24 hours

const DEFAULT_CHALLENGE = {
  id: 'default',
  title: 'Reverse a String',
  description: 'Write a function that reverses a given string without using built-in reverse functions.',
  difficulty: 'Easy',
  baseCode: 'fun reverseString(s: String): String {\n    // Your code here\n}',
  expectedOutput: 'The function should return the input string reversed'
};

async function getOrGenerateChallenge() {
  if (!db) return DEFAULT_CHALLENGE;

  try {
    const snapshot = await db.ref('daily_challenge').once('value');
    const existing = snapshot.val();

    // Check if challenge exists and is from today
    if (existing && existing.id) {
      const today = new Date().toISOString().split('T')[0].replace(/-/g, '');
      if (existing.id === `challenge_${today}`) {
        return existing;
      }
    }

    // Generate new challenge
    try {
      const newChallenge = await geminiService.generateDailyChallenge();
      await db.ref('daily_challenge').set(newChallenge);
      console.log('✅ New daily challenge generated:', newChallenge.title);
      return newChallenge;
    } catch (genErr) {
      console.error('⚠️ Failed to generate challenge, using default:', genErr.message);
      return existing || DEFAULT_CHALLENGE;
    }
  } catch (err) {
    console.error('⚠️ Daily challenge fetch error:', err.message);
    return DEFAULT_CHALLENGE;
  }
}

// Auto-generate at server startup
function scheduleChallenge() {
  // Generate on startup
  setTimeout(() => {
    getOrGenerateChallenge().catch(console.error);
  }, 5000); // 5s delay to let Firebase init

  // Regenerate every 24 hours
  setInterval(() => {
    getOrGenerateChallenge().catch(console.error);
  }, 24 * 60 * 60 * 1000);
}

module.exports = { getOrGenerateChallenge, scheduleChallenge, DEFAULT_CHALLENGE };
