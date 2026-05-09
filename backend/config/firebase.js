const admin = require('firebase-admin');

// Initialize Firebase Admin SDK
// In production, set FIREBASE_SERVICE_ACCOUNT env var with the JSON content
// In development, you can place a firebase-service-account.json file
function initializeFirebase() {
  if (admin.apps.length > 0) return admin;

  try {
    const serviceAccount = process.env.FIREBASE_SERVICE_ACCOUNT
      ? JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT)
      : null;

    if (serviceAccount) {
      admin.initializeApp({
        credential: admin.credential.cert(serviceAccount),
        databaseURL: process.env.FIREBASE_DATABASE_URL
      });
    } else {
      // Fallback: try default credentials (works on GCP / Firebase Hosting)
      admin.initializeApp({
        databaseURL: process.env.FIREBASE_DATABASE_URL
      });
    }

    console.log('✅ Firebase Admin initialized');
  } catch (err) {
    console.error('❌ Firebase Admin init failed:', err.message);
    // Don't crash — auth will reject all requests, but server stays up
  }

  return admin;
}

initializeFirebase();

const db = admin.apps.length > 0 ? admin.database() : null;

module.exports = { admin, db };
