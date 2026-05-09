# 🤖 AI Coding Mentor

An intelligent Android app that helps developers learn, debug, and master programming — powered by AI.

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-Material3-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Firebase](https://img.shields.io/badge/Firebase-Cloud_Functions-FFCA28?logo=firebase&logoColor=black)](https://firebase.google.com)

---

## ✨ Features

| Feature | Description |
|---------|-------------|
| 🐛 **Code Debugger** | Paste your code and get instant error detection, explanations, and optimized fixes |
| 📖 **Code Explainer** | Step-by-step explanations in beginner or intermediate mode |
| ⏱️ **Complexity Analyzer** | Big O time & space complexity analysis with optimization tips |
| 🧪 **Test Generator** | Auto-generate unit tests with normal, edge, and error cases |
| 💬 **AI Chat** | Multi-turn conversational coding mentor with code context awareness |
| 🧠 **Quiz Generator** | AI-generated multiple choice quizzes based on your code |
| 🗺️ **Learning Roadmaps** | Structured learning paths for any programming topic |
| 🎤 **Interview Prep** | Realistic interview questions generated from your code |
| 🏆 **Daily Challenges** | New coding challenge every day with a global leaderboard |
| 📷 **OCR Code Scanner** | Scan code from images using your phone camera |
| 📊 **Learning Progress** | Track your quizzes, analyses, and growth over time |

## 🏗️ Architecture

```
┌─────────────────────────┐
│   Android App (Kotlin)  │
│   Jetpack Compose + M3  │
│   MVVM Architecture     │
└──────────┬──────────────┘
           │ HTTPS + Firebase Auth Token
           ▼
┌─────────────────────────┐
│  Firebase Cloud Functions│
│  Express.js + Helmet    │
│  Rate Limiting + CORS   │
│  Input Sanitization     │
└──────────┬──────────────┘
           │
     ┌─────┴─────┐
     ▼           ▼
┌─────────┐ ┌──────────┐
│OpenRouter│ │ Firebase │
│   AI    │ │ Realtime │
│  (LLM)  │ │    DB    │
└─────────┘ └──────────┘
```

## 🛡️ Security

This project follows a strict **12-rule security framework**:

- ✅ **Secrets** — All API keys in Firebase Secrets Manager, never in code
- ✅ **Rate Limiting** — 10 req/min per user on AI endpoints, 60 req/min globally
- ✅ **Input Validation** — Server-side validation with `express-validator`
- ✅ **Auth** — Firebase Authentication with ID token verification
- ✅ **CORS** — Explicit origin whitelist (no wildcards)
- ✅ **Security Headers** — `helmet` middleware (CSP, HSTS, X-Frame-Options)
- ✅ **Error Handling** — Generic messages to client, detailed server-side logging
- ✅ **Prompt Injection Defense** — LLM input sanitization before every AI call
- ✅ **Token Budgets** — `max_tokens: 4096` cap + per-user usage logging

## 🚀 Getting Started

### Prerequisites

- Android Studio Hedgehog or newer
- Node.js 22+
- Firebase CLI (`npm install -g firebase-tools`)
- An [OpenRouter](https://openrouter.ai) account (free)

### 1. Clone the repo

```bash
git clone https://github.com/sirshendu-maiti/AI-Coding-Mentor.git
cd AI-Coding-Mentor
```

### 2. Firebase Setup

```bash
# Login to Firebase
firebase login

# Set your OpenRouter API key securely
firebase functions:secrets:set OPENROUTER_API_KEY

# Deploy Cloud Functions
cd functions && npm install
firebase deploy --only functions

# Deploy database rules
firebase deploy --only database
```

### 3. Android Setup

1. Open the project in Android Studio
2. Place your `google-services.json` in `app/`
3. Update `BACKEND_URL` in `app/build.gradle` with your deployed function URL
4. Sync Gradle and run on a physical device or emulator

## 📁 Project Structure

```
├── app/                          # Android application
│   └── src/main/java/.../
│       ├── data/
│       │   ├── api/              # Retrofit client & API service
│       │   ├── model/            # Data models
│       │   └── repository/       # AI, Auth, Database repositories
│       ├── ui/
│       │   ├── components/       # Reusable Compose components
│       │   ├── screens/          # All app screens
│       │   └── theme/            # Material 3 theming
│       ├── viewmodel/            # MainViewModel (MVVM)
│       └── utils/                # OCR utilities
├── functions/                    # Firebase Cloud Functions (production)
│   └── index.js                  # Express app with all AI endpoints
├── backend/                      # Standalone Express server (dev/alt)
├── database.rules.json           # Firebase Realtime Database security rules
└── firebase.json                 # Firebase project configuration
```

## 🔧 Tech Stack

| Layer | Technology |
|-------|-----------|
| **Frontend** | Kotlin, Jetpack Compose, Material 3 |
| **Architecture** | MVVM with StateFlow |
| **Backend** | Firebase Cloud Functions, Express.js |
| **AI** | OpenRouter (auto-routed free models) |
| **Auth** | Firebase Authentication (Email, Google, Anonymous) |
| **Database** | Firebase Realtime Database |
| **Networking** | Retrofit 2 + OkHttp |
| **Security** | Helmet, express-rate-limit, express-validator |
| **OCR** | Google ML Kit Text Recognition |

## 🌐 Supported Languages

Python • Java • Kotlin • JavaScript • TypeScript • C • C++ • Go • Rust • Ruby • Swift • PHP • C#

## 📄 License

This project is for educational purposes. Feel free to fork and build upon it.

---

<p align="center">
  Built with ❤️ by <a href="https://github.com/sirshendu-maiti">Sirshendu Maiti</a>
</p>
