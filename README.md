# NotionGPT

Chatbot RAG (Retrieval-Augmented Generation) que responde perguntas baseando-se estritamente nos meus resumos de estudo exportados do Notion.

## Stack

- Java 21 + Spring Boot 3.5
- [LangChain4J](https://github.com/langchain4j/langchain4j) 1.13.1
- Anthropic Claude (claude-haiku-4-5) como modelo de chat
- BGE Small EN v1.5 (quantizado) como modelo de embeddings
- Banco vetorial em memória (`InMemoryEmbeddingStore`)

## Arquitetura (Naive RAG)

```
Inicialização (ingestão)
  FileSystemDocumentLoader → DocumentByParagraphSplitter → EmbeddingModel → InMemoryEmbeddingStore

Por requisição (retrieval + geração)
  POST /query → embeda a pergunta → EmbeddingStore.search() → monta o prompt → ChatModel → resposta
```

## Configuração

### Pré-requisitos

- Java 21+
- Maven
- Chave de API da Anthropic

### Variável de ambiente

```bash
export ANTHROPIC_API_KEY=sua_chave_aqui
```

### Notas de estudo

Coloque seus arquivos `.md` no diretório configurado em `RagApplication.java`:

```java
FileSystemDocumentLoader.loadDocuments("caminho/para/seus/resumos");
```

### Executar

```bash
mvn spring-boot:run
```

## API

### POST /query

```json
// Requisição
{ "query": "O que é o Mockito?" }

// Resposta
{ "reply": "Mockito é uma biblioteca para criar e configurar mocks/spies em testes automatizados..." }
```