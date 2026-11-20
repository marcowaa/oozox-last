import { zodToJsonSchema } from "zod-to-json-schema";
import {
  CreateInvoiceRequestSchema,
  HealthCheckResponse,
  InvoiceItemSchema,
  LoginRequestSchema,
  RegisterRequestSchema,
  UpdateInvoiceStatusRequestSchema,
} from "@workspace/api-zod";

function toOpenApiSchema(schema: unknown): Record<string, unknown> {
  const convert = zodToJsonSchema as (
    s: unknown,
    options: { target: string },
  ) => Record<string, unknown>;
  const jsonSchema = convert(schema, { target: "openApi3" });
  const { $schema: _ignored, ...rest } = jsonSchema;
  return rest;
}

export function buildOpenApiSpec(): Record<string, unknown> {
  return {
    openapi: "3.0.3",
    info: {
      title: "OOZOX API Server",
      version: "0.1.0",
      description:
        "API specification automatically generated from @workspace/api-zod schemas.",
    },
    servers: [
      {
        url: "/api",
        description: "Base API path",
      },
    ],
    tags: [
      {
        name: "health",
        description: "Health check operations",
      },
      {
        name: "docs",
        description: "OpenAPI and Swagger UI documentation",
      },
    ],
    paths: {
      "/healthz": {
        get: {
          operationId: "healthCheck",
          tags: ["health"],
          summary: "Health check",
          description: "Returns server health status",
          responses: {
            "200": {
              description: "Healthy",
              content: {
                "application/json": {
                  schema: {
                    $ref: "#/components/schemas/HealthCheckResponse",
                  },
                },
              },
            },
          },
        },
      },
      "/openapi.json": {
        get: {
          operationId: "getOpenApiSpec",
          tags: ["docs"],
          summary: "OpenAPI specification",
          description: "Returns the generated OpenAPI 3.0 JSON specification",
          responses: {
            "200": {
              description: "OpenAPI JSON document",
              content: {
                "application/json": {
                  schema: {
                    type: "object",
                  },
                },
              },
            },
          },
        },
      },
    },
    components: {
      securitySchemes: {
        bearerAuth: {
          type: "http",
          scheme: "bearer",
          bearerFormat: "JWT",
          description: "JWT Bearer token authentication",
        },
      },
      schemas: {
        HealthCheckResponse: toOpenApiSchema(HealthCheckResponse),
        RegisterRequest: toOpenApiSchema(RegisterRequestSchema),
        LoginRequest: toOpenApiSchema(LoginRequestSchema),
        InvoiceItem: toOpenApiSchema(InvoiceItemSchema),
        CreateInvoiceRequest: toOpenApiSchema(CreateInvoiceRequestSchema),
        UpdateInvoiceStatusRequest: toOpenApiSchema(
          UpdateInvoiceStatusRequestSchema,
        ),
      },
    },
  };
}
