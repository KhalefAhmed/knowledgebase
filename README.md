# Knowledgebase

API de recherche augmentée (RAG) avec Micronaut, Ollama et Infinispan. Les
documents et leurs embeddings sont stockés dans Infinispan; les modèles de
chat et d'embeddings s'exécutent localement avec Ollama.

## Démarrer

Prérequis : Docker Compose, Java 25 et les modules Micronaut LangChain4j
`2.2.1-SNAPSHOT` disponibles dans Maven Local.

```bash
docker compose up -d
./gradlew run
```

Au premier démarrage, Ollama télécharge `qwen2.5:3b` et `nomic-embed-text`.
Le premier modèle sert aux réponses et le second aux vecteurs de 768
dimensions. Les données Infinispan et les modèles Ollama persistent dans des
volumes Docker.

Infinispan utilise `admin` / `admin` pour le développement local. Ces valeurs
sont définies dans `docker-compose.yml` et `src/main/resources/application.yml`.

## API

Indexer ou remplacer un document Markdown ou texte :

```bash
curl -X POST http://localhost:8080/api/documents \
  -H 'Content-Type: application/json' \
  -d '{"source":"guide.md","content":"Le contenu du document à indexer."}'
```

Poser une question à la base :

```bash
curl -X POST http://localhost:8080/api/ask \
  -H 'Content-Type: application/json' \
  -d '{"question":"Que contient le guide ?","maxResults":5}'
```

La réponse contient le texte généré et les extraits récupérés avec leurs
sources et scores. `maxResults` est facultatif et limité à 10.

Supprimer un document indexé :

```bash
curl -X DELETE http://localhost:8080/api/documents/guide.md
```

Les documents sont découpés en segments d'au plus 1200 caractères. L'API
accepte des contenus de 500 000 caractères maximum et remplace les anciens
segments portant la même source lors d'un nouvel indexage.
