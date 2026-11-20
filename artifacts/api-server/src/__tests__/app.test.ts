import express from "express";
import request from "supertest";
import app from "../app";
import { AppError, errorHandler } from "../middleware/errorHandler";

describe("API Server Core Routes", () => {
  describe("GET /api/healthz", () => {
    it("should return 200 OK with status payload", async () => {
      const res = await request(app).get("/api/healthz");

      expect(res.status).toBe(200);
      expect(res.headers["content-type"]).toMatch(/application\/json/);
      expect(res.body).toEqual({ status: "ok" });
    });

    it("should include CORS headers", async () => {
      const res = await request(app).get("/api/healthz");

      expect(res.headers["access-control-allow-origin"]).toBe("*");
    });
  });

  describe("GET /api/openapi.json", () => {
    it("should return generated OpenAPI 3.0 specification with @workspace/api-zod schemas", async () => {
      const res = await request(app).get("/api/openapi.json");

      expect(res.status).toBe(200);
      expect(res.headers["content-type"]).toMatch(/application\/json/);
      expect(res.body.openapi).toBe("3.0.3");
      expect(res.body.paths).toHaveProperty("/healthz");
      expect(res.body.components?.securitySchemes).toHaveProperty("bearerAuth");

      const schemas = res.body.components?.schemas;
      expect(schemas).toHaveProperty("HealthCheckResponse");
      expect(schemas).toHaveProperty("RegisterRequest");
      expect(schemas).toHaveProperty("LoginRequest");
      expect(schemas).toHaveProperty("InvoiceItem");
      expect(schemas).toHaveProperty("CreateInvoiceRequest");
      expect(schemas).toHaveProperty("UpdateInvoiceStatusRequest");
    });
  });

  describe("GET /api/docs", () => {
    it("should serve the Swagger UI HTML page", async () => {
      const res = await request(app).get("/api/docs/");

      expect(res.status).toBe(200);
      expect(res.headers["content-type"]).toMatch(/text\/html/);
      expect(res.text).toContain("OOZOX API Documentation");
      expect(res.text).toContain("swagger-ui");
    });
  });

  describe("Error handling", () => {
    it("should return consistent 404 JSON structure for unknown API paths", async () => {
      const res = await request(app).get("/api/does-not-exist");

      expect(res.status).toBe(404);
      expect(res.headers["content-type"]).toMatch(/application\/json/);
      expect(res.body).toEqual({
        error: {
          code: "NOT_FOUND",
          message: "Route GET /api/does-not-exist not found",
          statusCode: 404,
        },
      });
    });

    it("should return 400 INVALID_JSON for malformed JSON payloads", async () => {
      const res = await request(app)
        .post("/api/healthz")
        .set("Content-Type", "application/json")
        .send("{bad json");

      expect(res.status).toBe(400);
      expect(res.body).toEqual({
        error: {
          code: "INVALID_JSON",
          message: "Malformed JSON request body",
          statusCode: 400,
        },
      });
    });

    it("should format AppError, ZodError, and unexpected errors consistently", async () => {
      const testApp = express();
      testApp.get("/app-error", (_req, _res, next) => {
        next(
          new AppError("Unauthorized access", 401, "UNAUTHORIZED", {
            hint: "Provide a Bearer token",
          }),
        );
      });
      testApp.get("/zod-error", (_req, _res, next) => {
        next({
          name: "ZodError",
          issues: [{ path: ["email"], message: "Invalid email", code: "invalid_string" }],
        });
      });
      testApp.get("/unexpected", () => {
        throw new Error("Database connection lost");
      });
      testApp.use(errorHandler);

      const appErrRes = await request(testApp).get("/app-error");
      expect(appErrRes.status).toBe(401);
      expect(appErrRes.body).toEqual({
        error: {
          code: "UNAUTHORIZED",
          message: "Unauthorized access",
          statusCode: 401,
          details: { hint: "Provide a Bearer token" },
        },
      });

      const zodErrRes = await request(testApp).get("/zod-error");
      expect(zodErrRes.status).toBe(400);
      expect(zodErrRes.body).toEqual({
        error: {
          code: "VALIDATION_ERROR",
          message: "Request validation failed",
          statusCode: 400,
          details: [
            { path: ["email"], message: "Invalid email", code: "invalid_string" },
          ],
        },
      });

      const unexpectedRes = await request(testApp).get("/unexpected");
      expect(unexpectedRes.status).toBe(500);
      expect(unexpectedRes.body.error.code).toBe("INTERNAL_SERVER_ERROR");
      expect(unexpectedRes.body.error.statusCode).toBe(500);
    });
  });
});
