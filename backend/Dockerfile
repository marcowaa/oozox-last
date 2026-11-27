# Multi-stage production Dockerfile for OOZOX Express/MongoDB API
FROM node:20-bookworm-slim AS deps

WORKDIR /usr/src/app
COPY package*.json ./
RUN npm ci --omit=dev

FROM node:20-bookworm-slim AS runner

WORKDIR /usr/src/app

ENV NODE_ENV=production
ENV PORT=5000

COPY --from=deps /usr/src/app/node_modules ./node_modules
COPY package*.json ./
COPY src ./src

RUN chown -R node:node /usr/src/app
USER node

EXPOSE 5000

HEALTHCHECK --interval=15s --timeout=5s --start-period=15s --retries=4 \
  CMD node -e "fetch('http://127.0.0.1:5000/api/health').then(r=>process.exit(r.ok?0:1)).catch(()=>process.exit(1))"

CMD ["node", "src/server.js"]
