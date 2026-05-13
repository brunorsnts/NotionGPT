# NotionGPT

Chatbot RAG (Retrieval-Augmented Generation) que responde perguntas baseando-se estritamente nos meus resumos de estudo sincronizados do Notion.

## Stack

- Java 21 + Spring Boot 3.5
- [LangChain4J](https://github.com/langchain4j/langchain4j) 1.13.1
- Anthropic Claude (`claude-haiku-4-5`) como modelo de chat
- Cohere `embed-multilingual-light-v3.0` como modelo de embeddings
- PostgreSQL + pgvector como banco vetorial (`PgVectorEmbeddingStore`)
- Docker para o banco de dados

## Arquitetura (Naive RAG)

```
Ingestão (sync diário com Notion — @Scheduled, meia-noite)
  NotionClient.buscarPaginas() → deduplicação por lastEditedTime
    → NotionClient.buscarBlocos(pageId) → NotionBlockExtractor.extract()
    → DocumentByParagraphSplitter(1500, 200)
    → CohereEmbeddingModel (search_document) → PgVectorEmbeddingStore + PostgreSQL (metadados)

Consulta (POST /query)
  Pergunta → CohereEmbeddingModel (search_query) → EmbeddingStore.search()
    → monta prompt com contexto → Claude → resposta
```

## Configuração

### Pré-requisitos

- Java 21+
- Maven
- Docker
- Chave de API da Anthropic
- Chave de API da Cohere
- Chave de API do Notion

### Variáveis de ambiente

```bash
export ANTHROPIC_API_KEY=sua_chave_aqui
export COHERE_API_KEY=sua_chave_aqui
export NOTION_API_KEY=sua_chave_aqui
export NOTION_DATA_SOURCE_ID=id_do_banco_cursos
export DB_USER_PGVECTOR=seu_usuario
export DB_PASSWORD=sua_senha
```

### Banco de dados

```bash
docker compose up -d
```

### Executar

```bash
mvn spring-boot:run
```

A aplicação sobe em `http://localhost:8080`. O sync com o Notion ocorre automaticamente todo dia à meia-noite.

## API

### GET /documents

Retorna os documentos indexados (paginado).

### DELETE /documents/{id}

Remove um documento pelo ID. Retorna `204 No Content`.

### POST /query

```json
// Requisição
{ "query": "O que é o Mockito?" }

// Resposta
{ "reply": "Mockito é uma biblioteca para criar e configurar mocks/spies em testes automatizados..." }
```
