# Multi-stage lightweight production Dockerfile
FROM node:20-alpine AS base

WORKDIR /usr/src/app

# Install dependencies
COPY package*.json ./
RUN npm ci --only=production

# Copy application source
COPY . .

# Environment
ENV NODE_ENV=production
ENV PORT=5000

# Security: Non-root user
USER node

EXPOSE 5000

CMD ["node", "src/server.js"]
