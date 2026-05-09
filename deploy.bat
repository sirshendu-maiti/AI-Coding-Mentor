@echo off
echo ===================================================
echo   AI Coding Mentor - Firebase Deploy Script
echo ===================================================
echo.

:: Check Firebase CLI
firebase --version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Firebase CLI not found. Install it with:
    echo   npm install -g firebase-tools
    exit /b 1
)

:: Check login
echo [1/5] Checking Firebase login...
firebase login:list >nul 2>&1
if %errorlevel% neq 0 (
    echo Not logged in. Opening browser for login...
    firebase login
)

echo [2/5] Setting the Gemini API key as a secret...
echo (You will be prompted to enter your Gemini API key)
firebase functions:secrets:set GEMINI_API_KEY

echo [3/5] Deploying Cloud Functions...
firebase deploy --only functions

echo [4/5] Deploying Database Rules...
firebase deploy --only database

echo [5/5] Done!
echo.
echo ===================================================
echo   Your API is live at:
echo   https://us-central1-aicodingmentor-f1077.cloudfunctions.net/api
echo.
echo   Test it:
echo   curl https://us-central1-aicodingmentor-f1077.cloudfunctions.net/api/api/health
echo ===================================================
pause
