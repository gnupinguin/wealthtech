#!/bin/sh
set -eu

if [ "${POSTGRES_IMPORT_BACKUP_ENABLED:-false}" != "true" ]; then
  echo "Postgres backup import is disabled; skipping backup import."
  return 0 2>/dev/null || exit 0
fi

backup_file="${POSTGRES_IMPORT_BACKUP:-/backups/wealthtech_api_only_clients_documents_document_chunks_20260511062850.sql}"

if [ -z "${backup_file}" ]; then
  echo "No backup file found in /backups; skipping backup import."
  return 0 2>/dev/null || exit 0
fi

if [ ! -f "${backup_file}" ]; then
  echo "Configured backup file does not exist: ${backup_file}" >&2
  exit 1
fi

echo "Importing Postgres backup from ${backup_file}"
psql \
  -v ON_ERROR_STOP=1 \
  --username "${POSTGRES_USER}" \
  --dbname "${POSTGRES_DB}" \
  --file "${backup_file}"
echo "Postgres backup import completed."
