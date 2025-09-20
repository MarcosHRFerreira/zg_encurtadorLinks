# URL Shortener (Spring Boot)

Serviço de encurtamento de URLs com redirecionamento, estatísticas de acessos e documentação via Swagger/OpenAPI.

## Stack
- Java 21
- Spring Boot 3.2.4 (Web, Validation)
- JPA/Hibernate + PostgreSQL
- Flyway (migrações)
- Lombok
- springdoc-openapi-starter-webmvc-ui (Swagger UI)

## Pré-requisitos
- JDK 21 instalado e configurado (JAVA_HOME)
- Maven 3.9+ instalado (mvn no PATH)
- PostgreSQL em execução (ex.: localhost:5432)

## Configuração
A configuração padrão está em `src/main/resources/application.yaml`:

- Banco de dados:
  - URL: `jdbc:postgresql://localhost:5432/encurtadorURL`
  - Usuário: `postgres`
  - Senha: (não exposta)
- JPA/Hibernate: `ddl-auto: update`
- Flyway: habilitado (migrações em `classpath:db/migration`)
- Porta HTTP: `8080`

Ajuste conforme necessário (usuário/senha/host/porta do Postgres).

## Executando a aplicação

- Desenvolvimento (recomendado):
  ```bash
  mvn -q -DskipTests spring-boot:run
  ```
- Build do JAR e execução:
  ```bash
  mvn -q -DskipTests clean package
  java -jar target/url-shortener-0.0.1-SNAPSHOT.jar
  ```

> Dica: Evite executar `javac/java` diretamente sem Maven. Isso ignora o classpath das dependências e pode causar erros como `cannot find symbol SpringApplication`.

## Documentação da API (Swagger)
- Swagger UI: http://localhost:8080/swagger-ui/index.html
- OpenAPI (JSON): http://localhost:8080/v3/api-docs

Se um endpoint “sumir” na UI, verifique o campo "Filter" e limpe o filtro. O endpoint de estatísticas está sob a tag “Estatísticas”.

## Endpoints

### 1) Encurtar URL
- `POST /shorten`
- Body (JSON):
  ```json
  { "url": "https://www.google.com/", "code": "ABCDE" }
  ```
  - `code` é opcional (5 caracteres alfanuméricos). Se omitido, será gerado automaticamente.
- Respostas:
  - 201 Created (ShortenResponse)
  - 400 Bad Request (ValidationErrorResponse)
  - 409 Conflict (ErrorResponse)

Exemplo (cURL):
```bash
curl -i -H "Content-Type: application/json" \
  -d '{"url":"https://www.google.com/","code":"ABCDE"}' \
  http://localhost:8080/shorten
```

### 2) Redirecionamento
- `GET /{code}` (ex.: `/ABCDE`)
- Respostas:
  - 302 Found com header `Location: <URL original>`
  - 404 Not Found (ErrorResponse)

Exemplo (cURL):
```bash
curl -i http://localhost:8080/ABCDE
```

### 3) Estatísticas por código
- `GET /stats/{code}`
- Resposta 200 (StatsResponse):
  ```json
  { "code": "ABCDE", "originalUrl": "https://...", "hits": 42 }
  ```

Exemplo (cURL):
```bash
curl -i http://localhost:8080/stats/ABCDE
```

### 4) Ranking de URLs
- `GET /ranking`
- Resposta 200 (array de `RankingItem`):
  ```json
  [ { "code": "ABCDE", "hits": 42 }, ... ]
  ```

Exemplo (cURL):
```bash
curl -i http://localhost:8080/ranking
```

## Modelos de Resposta de Erro
- `ErrorResponse`: `{ "error": "...", "message": "..." }`
- `ValidationErrorResponse`: `{ "error": "Erro de validação", "details": { "campo": "mensagem" } }`

## Migrações de Banco (Flyway)
- Scripts em `src/main/resources/db/migration` (ex.: `V1__init.sql`).
- Aplicadas automaticamente ao subir a aplicação.

## Estrutura de Pastas (parcial)
```
backend/
├── pom.xml
├── src/main/java/br/corp/shortener/
│   ├── UrlShortenerApplication.java
│   ├── controllers/
│   │   ├── LinkController.java
│   │   └── StatsController.java
│   ├── services/
│   │   └── UrlShortenerService.java
│   ├── entities/
│   ├── repositories/
│   └── dto/
└── src/main/resources/
    ├── application.yaml
    └── db/migration/
```

## Solução de Problemas (Troubleshooting)
- `cannot find symbol SpringApplication`:
  - Execute via Maven (`mvn spring-boot:run`) ou reimporte o projeto como Maven no IDE.
- Swagger UI “Failed to fetch”:
  - Abra a UI servida pelo backend (`/swagger-ui/index.html`), limpe o filtro, desative extensões bloqueadoras, e verifique se o servidor está rodando.
- Porta 8080 em uso:
  - Altere `server.port` no `application.yaml` ou finalize o processo que ocupa a porta.
- Erro de conexão com PostgreSQL:
  - Verifique host/porta/usuário/senha; confirme DB em execução e acessível; ajuste `spring.datasource.*`.

## Licença
Sem licença explícita definida.