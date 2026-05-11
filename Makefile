COMPOSE ?= docker-compose
POSTGRES_BACKUP ?= /backups/wealthtech_api_only_clients_documents_document_chunks.sql

.PHONY: build up up-fresh-with-backup down

build:
	$(COMPOSE) build

up:
	$(COMPOSE) up -d

up-fresh-with-backup:
	$(COMPOSE) down -v
	POSTGRES_IMPORT_BACKUP_ENABLED=true POSTGRES_IMPORT_BACKUP=$(POSTGRES_BACKUP) $(COMPOSE) up

down:
	$(COMPOSE) down
