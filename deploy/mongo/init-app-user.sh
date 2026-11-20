#!/bin/bash
set -euo pipefail

# Provisions a least-privilege application database user for OOZOX Express API.
# Executed automatically by MongoDB 7.0 on first volume initialization.
mongosh --quiet \
  --username "${MONGO_INITDB_ROOT_USERNAME}" \
  --password "${MONGO_INITDB_ROOT_PASSWORD}" \
  --authenticationDatabase admin <<EOF
const appDb = db.getSiblingDB("${MONGO_INITDB_DATABASE:-oozox_prod_db}");
const existing = appDb.getUser("${MONGO_APP_USER}");
if (!existing) {
  appDb.createUser({
    user: "${MONGO_APP_USER}",
    pwd: "${MONGO_APP_PASSWORD}",
    roles: [
      { role: "readWrite", db: "${MONGO_INITDB_DATABASE:-oozox_prod_db}" }
    ]
  });
}
EOF
