import type { NextFunction, Request, Response } from "express";
import { logger } from "../lib/logger";

export interface ApiErrorBody {
  error: {
    code: string;
    message: string;
    statusCode: number;
    details?: unknown;
  };
}

export class AppError extends Error {
  public readonly statusCode: number;
  public readonly code: string;
  public readonly details?: unknown;

  constructor(
    message: string,
    statusCode = 500,
    code = "INTERNAL_SERVER_ERROR",
    details?: unknown,
  ) {
    super(message);
    this.name = "AppError";
    this.statusCode = statusCode;
    this.code = code;
    this.details = details;
  }
}

interface ZodLikeError {
  name: string;
  issues?: Array<{
    path: Array<string | number>;
    message: string;
    code: string;
  }>;
}

function isZodError(err: unknown): err is ZodLikeError {
  return (
    typeof err === "object" &&
    err !== null &&
    (err as { name?: unknown }).name === "ZodError" &&
    Array.isArray((err as { issues?: unknown }).issues)
  );
}

function isJsonSyntaxError(err: unknown): err is SyntaxError & { status: number } {
  return (
    err instanceof SyntaxError &&
    "status" in err &&
    (err as { status?: unknown }).status === 400
  );
}

export function notFoundHandler(
  req: Request,
  _res: Response,
  next: NextFunction,
): void {
  next(
    new AppError(
      `Route ${req.method} ${req.originalUrl} not found`,
      404,
      "NOT_FOUND",
    ),
  );
}

export function errorHandler(
  err: unknown,
  req: Request,
  res: Response,
  next: NextFunction,
): void {
  if (res.headersSent) {
    next(err);
    return;
  }

  let statusCode = 500;
  let code = "INTERNAL_SERVER_ERROR";
  let message = "Internal server error";
  let details: unknown;

  if (err instanceof AppError) {
    statusCode = err.statusCode;
    code = err.code;
    message = err.message;
    details = err.details;
  } else if (isZodError(err)) {
    statusCode = 400;
    code = "VALIDATION_ERROR";
    message = "Request validation failed";
    details = err.issues;
  } else if (isJsonSyntaxError(err)) {
    statusCode = 400;
    code = "INVALID_JSON";
    message = "Malformed JSON request body";
  } else if (err instanceof Error) {
    const maybeStatus =
      (err as { statusCode?: unknown; status?: unknown }).statusCode ??
      (err as { status?: unknown }).status;
    if (typeof maybeStatus === "number" && maybeStatus >= 400 && maybeStatus < 600) {
      statusCode = maybeStatus;
      code = statusCode >= 500 ? "INTERNAL_SERVER_ERROR" : "BAD_REQUEST";
    }
    if (statusCode < 500 || process.env.NODE_ENV !== "production") {
      message = err.message || message;
    }
  }

  if (statusCode >= 500) {
    logger.error(
      { err, method: req.method, url: req.originalUrl, statusCode },
      "Unhandled API error",
    );
  } else {
    logger.warn(
      { code, method: req.method, url: req.originalUrl, statusCode },
      message,
    );
  }

  const body: ApiErrorBody = {
    error: {
      code,
      message,
      statusCode,
      ...(details !== undefined ? { details } : {}),
    },
  };

  res.status(statusCode).json(body);
}
