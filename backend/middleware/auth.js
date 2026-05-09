const { admin } = require('../config/firebase');
const { AuthError } = require('../utils/errors');

/**
 * Middleware: Verify Firebase ID token from Authorization header.
 * Attaches decoded user info to req.user
 */
async function authenticate(req, res, next) {
  try {
    const authHeader = req.headers.authorization;

    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      throw new AuthError('Missing or invalid Authorization header');
    }

    const idToken = authHeader.split('Bearer ')[1];

    if (!idToken || idToken === 'null' || idToken === 'undefined') {
      throw new AuthError('Invalid token');
    }

    // Verify the ID token with Firebase Admin
    const decodedToken = await admin.auth().verifyIdToken(idToken);

    req.user = {
      uid: decodedToken.uid,
      email: decodedToken.email || null,
      name: decodedToken.name || null,
      emailVerified: decodedToken.email_verified || false
    };

    next();
  } catch (err) {
    if (err instanceof AuthError) {
      return res.status(err.statusCode).json({
        success: false,
        error: { code: err.code, message: err.message }
      });
    }

    // Firebase token verification errors
    if (err.code === 'auth/id-token-expired') {
      return res.status(401).json({
        success: false,
        error: { code: 'TOKEN_EXPIRED', message: 'Token expired. Please refresh.' }
      });
    }

    return res.status(401).json({
      success: false,
      error: { code: 'AUTH_ERROR', message: 'Authentication failed' }
    });
  }
}

module.exports = { authenticate };
