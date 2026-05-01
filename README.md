# NotionGPT

Chatbot RAG (Retrieval-Augmented Generation) que responde perguntas baseando-se estritamente nos meus resumos de estudo.

## Stack

- Java 21 + Spring Boot 3.5
- [LangChain4J](https://github.com/langchain4j/langchain4j) 1.13.1
- Anthropic Claude (`claude-haiku-4-5`) como modelo de chat
- Cohere `embed-multilingual-light-v3.0` como modelo de embeddings
- PostgreSQL + pgvector como banco vetorial (`PgVectorEmbeddingStore`)
- Docker para o banco de dados

## Arquitetura (Naive RAG)

```
Ingestão (POST /documents)
  MultipartFile → ApacheTikaDocumentParser → DocumentByParagraphSplitter
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

### Variáveis de ambiente

```bash
export ANTHROPIC_API_KEY=sua_chave_aqui
export COHERE_API_KEY=sua_chave_aqui
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

A aplicação sobe em `http://localhost:8080` com interface web para gerenciar documentos e conversar.

## API

### POST /documents

Faz upload e indexa um documento (PDF, DOCX, TXT, MD).

```
Content-Type: multipart/form-data
field: file
```

### GET /documents

Retorna os documentos indexados (paginado).

### DELETE /documents/{id}

Remove um documento pelo ID.

### POST /query

```json
// Requisição
{ "query": "O que é o Mockito?" }

// Resposta
{ "reply": "Mockito é uma biblioteca para criar e configurar mocks/spies em testes automatizados..." }
```
