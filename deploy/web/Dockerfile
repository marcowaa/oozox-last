# Multi-stage production Dockerfile for OOZOX Web Portal & Admin Dashboard
# Build context is the repository root. Source directory is parameterized via WEB_PORTAL_DIR
# so Phase 4 migration to `web-portal/` requires only updating the ARG/compose value.
FROM node:20-bookworm-slim AS builder

WORKDIR /workspace
RUN corepack enable

ARG VITE_API_BASE_URL=https://api.oozox.com
ARG WEB_PORTAL_DIR=artifacts/mockup-sandbox
ARG WEB_WORKSPACE_FILTER=@workspace/mockup-sandbox
ENV PORT=3000
ENV BASE_PATH=/
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}

COPY package.json pnpm-workspace.yaml pnpm-lock.yaml tsconfig.base.json tsconfig.json ./
COPY ${WEB_PORTAL_DIR} ./${WEB_PORTAL_DIR}
COPY lib ./lib
COPY scripts ./scripts

RUN pnpm install --prod=false --filter "${WEB_WORKSPACE_FILTER}" --frozen-lockfile
ENV NODE_ENV=production
RUN pnpm --filter "${WEB_WORKSPACE_FILTER}" run build

FROM nginx:1.27-alpine

ARG WEB_PORTAL_DIR=artifacts/mockup-sandbox

COPY deploy/web/nginx-spa.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /workspace/${WEB_PORTAL_DIR}/dist /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=15s --timeout=3s --start-period=5s --retries=3 \
  CMD wget -q -O - http://127.0.0.1/ >/dev/null 2>&1 || exit 1
