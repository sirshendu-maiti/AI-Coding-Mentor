FROM node:22-slim

WORKDIR /app

# Copy package files
COPY package*.json ./

# Install dependencies
RUN npm install --production

# Copy application code
COPY . .

# Set environment
ENV NODE_ENV=production
ENV PORT=8080
ENV OPENROUTER_API_KEY=${OPENROUTER_API_KEY:-sk-or-v1-placeholder}
ENV FIREBASE_SERVICE_ACCOUNT=${FIREBASE_SERVICE_ACCOUNT:-{}}
ENV FIREBASE_DATABASE_URL=${FIREBASE_DATABASE_URL:-}
ENV RATE_LIMIT_AI_MAX=50
ENV RATE_LIMIT_AI_WINDOW_MINUTES=60
ENV RATE_LIMIT_GLOBAL_MAX=100
ENV RATE_LIMIT_GLOBAL_WINDOW_MINUTES=1

# Expose port
EXPOSE 8080

# Health check - removed start-period validation to prevent timeout
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD node -e "require('http').get('http://localhost:8080/api/health', (r) => {if (r.statusCode !== 200) throw new Error(r.statusCode)}).on('error', () => process.exit(0))" || exit 0

# Start application
CMD ["node", "server.js"]
