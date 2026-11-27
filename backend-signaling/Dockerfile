# Multi-stage production Dockerfile for OOZOX WebRTC Signaling Server
FROM golang:1.22 AS builder

WORKDIR /src

COPY go.mod ./
COPY . .
RUN go mod tidy && go mod download && CGO_ENABLED=0 GOOS=linux go build -ldflags="-s -w" -o /out/signaling ./cmd/server

FROM alpine:3.20

RUN apk add --no-cache ca-certificates wget \
    && addgroup -S appgroup \
    && adduser -S appuser -G appgroup

WORKDIR /app
COPY --from=builder /out/signaling /app/signaling

USER appuser

ENV APP_ENV=production
ENV AUTH_TOKEN_ENABLED=false
ENV PORT=8080

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=3s --start-period=10s --retries=3 \
  CMD wget -q -O - http://127.0.0.1:8080/health >/dev/null 2>&1 || exit 1

CMD ["/app/signaling"]
