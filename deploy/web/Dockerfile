# Multi-stage production Dockerfile for OOZOX Web Portal & Admin Dashboard (React 19 + Vite 7)
# Uses Debian Bookworm (glibc) so pnpm-workspace.yaml linux-x64 native bindings match cleanly.
FROM node:20-bookworm-slim AS builder

WORKDIR /workspace
RUN corepack enable

ARG VITE_API_BASE_URL=https://api.oozox.com
ENV PORT=3000
ENV BASE_PATH=/
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}

COPY package.json pnpm-workspace.yaml pnpm-lock.yaml tsconfig.base.json tsconfig.json ./
COPY artifacts ./artifacts
COPY lib ./lib
COPY scripts ./scripts

# Install including devDependencies (vite, react, tailwindcss are listed under devDependencies)
RUN pnpm install --prod=false --filter @workspace/mockup-sandbox --frozen-lockfile
ENV NODE_ENV=production
RUN pnpm --filter @workspace/mockup-sandbox run build

FROM nginx:1.27-alpine

COPY deploy/web/nginx-spa.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /workspace/artifacts/mockup-sandbox/dist /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=15s --timeout=3s --start-period=5s --retries=3 \
  CMD wget -q -O - http://127.0.0.1/ >/dev/null 2>&1 || exit 1
