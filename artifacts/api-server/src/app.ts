import express, { type Express } from "express";
import cors from "cors";
import pinoHttp from "pino-http";
import swaggerUi from "swagger-ui-express";
import router from "./routes";
import { logger } from "./lib/logger";
import { buildOpenApiSpec } from "./lib/openapi";
import { errorHandler, notFoundHandler } from "./middleware/errorHandler";

const app: Express = express();

app.use(
  pinoHttp({
    logger,
    serializers: {
      req(req) {
        return {
          id: req.id,
          method: req.method,
          url: req.url?.split("?")[0],
        };
      },
      res(res) {
        return {
          statusCode: res.statusCode,
        };
      },
    },
  }),
);
app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

const openApiSpec = buildOpenApiSpec();

app.get("/api/openapi.json", (_req, res) => {
  res.json(openApiSpec);
});

app.use(
  "/api/docs",
  swaggerUi.serve,
  swaggerUi.setup(openApiSpec, {
    explorer: true,
    customSiteTitle: "OOZOX API Documentation",
  }),
);

app.use("/api", router);

app.use(notFoundHandler);
app.use(errorHandler);

export default app;
