# Knowledgebase

Retrieval-augmented generation (RAG) API built with Micronaut, Ollama, and
Infinispan. Documents and their embeddings are stored in Infinispan, while
the chat and embedding models run locally with Ollama.

## Getting started

Prerequisites: Docker Compose, Java 25, and the Micronaut LangChain4j
`2.2.1-SNAPSHOT` modules available in the local Maven repository.

```bash
docker compose up -d
./gradlew run
```

On first startup, Ollama downloads `qwen2.5:3b` and `nomic-embed-text`.
The first model generates responses, while the second produces 768-dimensional
vectors. Infinispan data and Ollama models are persisted in Docker volumes.

Infinispan uses `admin` / `admin` for local development. These values are
defined in `docker-compose.yml` and `src/main/resources/application.yml`.

## API

Index or replace a Markdown or plain-text document:

```bash
curl -X POST http://localhost:8080/api/documents \
  -H 'Content-Type: application/json' \
  -d '{"source":"guide.md","content":"The document content to index."}'
```

Ask the knowledge base a question:

```bash
curl -X POST http://localhost:8080/api/ask \
  -H 'Content-Type: application/json' \
  -d '{"question":"What does the guide contain?","maxResults":5}'
```

The response contains the generated text and the retrieved excerpts, including
their sources and scores. `maxResults` is optional and limited to 10.

Delete an indexed document:

```bash
curl -X DELETE http://localhost:8080/api/documents/guide.md
```

Documents are split into segments of up to 1,200 characters. The API accepts
content of up to 500,000 characters and replaces existing segments with the
same source when a document is re-indexed.
