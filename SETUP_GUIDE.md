# AI Coding Mentor — Setup & Deployment Guide

## 🏗️ Architecture Overview

```
┌─────────────────┐       ┌───────────────────┐       ┌─────────────┐
│  Android App    │──────▸│  Backend Server    │──────▸│  Gemini API │
│  (Kotlin/Compose│       │  (Node.js/Express) │       │  (Google)   │
│   + Firebase)   │       │  + Firebase Admin  │       └─────────────┘
└─────────────────┘       └───────────────────┘
        │                          │
        └──────── Firebase ────────┘
                (Auth + Realtime DB)
```

- **Android App** → Handles UI, authentication, and real-time data via Firebase
- **Backend Server** → Proxies all AI calls, holds the API key securely, validates input, rate limits
- **Gemini API** → Only the backend talks to Gemini (API key never on client)

---

## 📋 Prerequisites

- **Android Studio** Hedgehog (2023.1.1) or later
- **Node.js** 18+ and npm
- **Firebase Project** with Auth + Realtime Database enabled
- **Gemini API Key** from [Google AI Studio](https://aistudio.google.com/app/apikey)

---

## 🔥 Firebase Setup

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Create a project or use `aicodingmentor-f1077`
3. Enable **Authentication** → Sign-in methods:
   - ✅ Email/Password
   - ✅ Anonymous
   - ⬜ Google (optional — see "Google Sign-In" section below)
4. Enable **Realtime Database** → Start in test mode
5. Download `google-services.json` → place in `app/` folder

### Google Sign-In (Optional)
To enable Google Sign-In:
1. Firebase Console → Authentication → Sign-in method → Google → Enable
2. Add your app's **SHA-1 fingerprint** in Project Settings → Your Apps
   ```bash
   # Get SHA-1 from your keystore:
   keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android
   ```
3. Copy the **Web client ID** from Google sign-in config
4. Paste it in `app/src/main/res/values/strings.xml`:
   ```xml
   <string name="default_web_client_id">YOUR_ACTUAL_ID.apps.googleusercontent.com</string>
   ```

> **Note**: If you don't set up Google Sign-In, the Google button will automatically be hidden. Email/Password and Guest login work without it.

---

## 🖥️ Backend Server Setup

### Local Development

```bash
cd backend

# 1. Install dependencies
npm install

# 2. Create your .env file from the template
cp .env.example .env

# 3. Edit .env with your actual values:
#    - GEMINI_API_KEY: from Google AI Studio
#    - FIREBASE_SERVICE_ACCOUNT: JSON from Firebase Console > Project Settings > Service Accounts > Generate New Private Key
#    - FIREBASE_DATABASE_URL: from Firebase Console > Realtime Database (copy the URL)

# 4. Start the server
npm run dev
```

The server will start on `http://localhost:3000`. Test it:
```bash
curl http://localhost:3000/api/health
# → { "success": true, "data": { "status": "healthy", "version": "1.0.0" } }
```

### Environment Variables

| Variable | Required | Description |
|----------|----------|-------------|
| `GEMINI_API_KEY` | ✅ | Google Gemini API key |
| `FIREBASE_SERVICE_ACCOUNT` | ✅ | Full JSON content of Firebase service account (single line) |
| `FIREBASE_DATABASE_URL` | ✅ | Your Realtime Database URL |
| `PORT` | ❌ | Server port (default: 3000) |
| `NODE_ENV` | ❌ | `production` or `development` |
| `RATE_LIMIT_AI_MAX` | ❌ | Max AI requests per user per window (default: 30) |
| `RATE_LIMIT_AI_WINDOW_MINUTES` | ❌ | Rate limit window in minutes (default: 60) |

### Deploy to Production

The backend can be deployed to any Node.js host:

**Render (Recommended — Free Tier):**
1. Push backend code to GitHub
2. Create a new Web Service on [Render](https://render.com)
3. Set root directory to `backend`
4. Set build command: `npm install`
5. Set start command: `node server.js`
6. Add all environment variables from `.env`

**Railway:**
1. Connect GitHub repo
2. Set root directory to `backend`
3. Add environment variables
4. Deploy

After deploying, update the Android app's backend URL in `app/build.gradle`:
```groovy
// In the release buildType:
buildConfigField "String", "BACKEND_URL", "\"https://your-backend.onrender.com\""
```

---

## 📱 Android App Setup

### 1. Clone & Open
```bash
git clone <your-repo-url>
```
Open the project in Android Studio.

### 2. Configure Backend URL
In `app/build.gradle`, the `BACKEND_URL` is already set:
- **Debug**: `http://10.0.2.2:3000` (Android emulator → localhost)
- **Release**: Update to your deployed backend URL

### 3. Build & Run
```bash
./gradlew assembleDebug
```
Or press ▶️ in Android Studio.

---

## 🔒 Security Checklist

- [x] API key removed from Android client — lives only on backend
- [x] Firebase ID token verification on all API routes
- [x] Per-user rate limiting (30 AI requests/hour)
- [x] Input validation (code length, language whitelist)
- [x] HTTP logging disabled in release builds
- [x] ProGuard/R8 enabled for release builds
- [x] Network security config enforces HTTPS in production
- [x] Cleartext only allowed to localhost for development

---

## 📡 API Endpoints

All endpoints (except health) require `Authorization: Bearer <firebase_id_token>`.

### AI Endpoints
| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/ai/analyze` | Debug & analyze code |
| POST | `/api/ai/explain` | Step-by-step explanation |
| POST | `/api/ai/complexity` | Time/space complexity |
| POST | `/api/ai/tests` | Generate test cases |
| POST | `/api/ai/quiz` | Generate quiz (pre-parsed JSON) |
| POST | `/api/ai/roadmap` | Generate learning roadmap |
| POST | `/api/ai/interview` | Interview questions |
| POST | `/api/ai/chat` | Multi-turn chat |

### Data Endpoints
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/data/history` | Get code history |
| POST | `/api/data/history` | Save history item |
| DELETE | `/api/data/history/:id` | Delete history item |
| GET | `/api/data/leaderboard` | Top 50 leaderboard |
| GET | `/api/data/challenge` | Daily challenge |

### User Endpoints
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/user/profile` | Get user profile |
| PUT | `/api/user/profile` | Update display name |
| GET | `/api/user/progress` | Learning progress |

---

## 🐛 Known Issues & Fixes Applied

### v2.0 Fixes
1. **API Key Security** — Removed hardcoded Gemini API key from client
2. **Camera Executor Leak** — Added `DisposableEffect` cleanup
3. **Deprecated LocalLifecycleOwner** — Updated import path
4. **Fragile JSON Parsing** — Quiz/Roadmap now parsed server-side
5. **Google Sign-In Crash** — Button hidden when not configured
6. **Material 3 Theme** — Fixed parent theme compatibility
7. **Keyboard Overlap** — Added `imePadding()` to ChatScreen
8. **Auth Error Persistence** — Errors clear when switching login/register
9. **Password Reset** — Added forgot password flow
10. **Markdown Rendering** — Added numbered lists, inline code support

---

## 📦 Building for Release

```bash
# 1. Update BACKEND_URL in app/build.gradle release block
# 2. Create a signing keystore (if you don't have one)
keytool -genkey -v -keystore release-key.jks -keyalg RSA -keysize 2048 -validity 10000

# 3. Build signed APK
./gradlew assembleRelease

# 4. Or build AAB for Play Store
./gradlew bundleRelease
```
