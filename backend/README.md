# URL Shortener – Backend (Spring Boot)

Serviço de encurtamento de URLs com redirecionamento via código curto, estatísticas de acessos, ranking e documentação OpenAPI/Swagger.

## Stack
- Java 21
- Spring Boot 3.2.4 (Web, Validation, Actuator)
- Spring Data JPA + PostgreSQL
- Flyway (migrações)
- springdoc-openapi-starter-webmvc-ui (Swagger UI)
- H2 (perfil de testes)

## Pré-requisitos
- JDK 21 instalado (JAVA_HOME configurado)
- Maven 3.9+ no PATH
- Banco PostgreSQL rodando (se não usar Docker Compose)
- Opcional: Docker e Docker Compose (para subir DB + app com um comando)

## Configuração
A configuração principal está em `src/main/resources/application.yaml`.

- Banco de dados (valores padrão – podem ser sobrescritos por variáveis de ambiente):
  - URL: `${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/encurtadorURL}`
  - Usuário: `${SPRING_DATASOURCE_USERNAME:postgres}`
  - Senha: `${SPRING_DATASOURCE_PASSWORD:admin}`
- JPA/Hibernate:
  - `ddl-auto: update`
  - `database-platform: org.hibernate.dialect.PostgreSQLDialect`
- Flyway:
  - `enabled: true`, `baseline-on-migrate: true`, `baseline-version: 0`
  - `locations: classpath:db/migration`
  - Convenção: prefixo `V`, separador `__`, sufixo `.sql`
- Porta HTTP: `8080`
- Actuator/Health:
  - Endpoints expostos: `health,info,metrics`
  - Probes de saúde habilitados (readiness)

Override via ambiente (exemplos):
```
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/url_shortener
SPRING_DATASOURCE_USERNAME=url_shortener
SPRING_DATASOURCE_PASSWORD=url_shortener
SPRING_JPA_HIBERNATE_DDL_AUTO=update
SERVER_PORT=8080
```

## Subindo com Docker Compose (recomendado)
O projeto possui um `docker-compose.yml` na raiz que sobe Postgres e o backend.

- Subir serviços:
```bash
docker compose up -d --build
```
- Serviços e portas:
  - PostgreSQL: `5432` (DB/USER/PASS: `url_shortener`)
  - Backend: `8080`
- Healthcheck do backend: `GET /actuator/health/readiness`
- Parar serviços:
```bash
docker compose down
```
- Volume de dados do Postgres: `db_data`

## Executando localmente (sem Docker)
- Desenvolvimento:
```bash
mvn -q -DskipTests spring-boot:run
```
- Build do JAR e execução:
```bash
mvn -q -DskipTests clean package
java -jar target/url-shortener-0.0.1-SNAPSHOT.jar
```
Dica: execute sempre via Maven. Rodar `java`/`javac` sem Maven ignora o classpath gerenciado e tende a causar erros.

## Documentação da API (Swagger)
- Swagger UI: http://localhost:8080/swagger-ui/index.html
- OpenAPI (JSON): http://localhost:8080/v3/api-docs

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
- Exemplo (cURL):
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
- Exemplo (cURL):
```bash
curl -i http://localhost:8080/ABCDE
```

### 3) Estatísticas por código
- `GET /stats/{code}`
- 200 OK (StatsResponse):
```json
{ "code": "ABCDE", "originalUrl": "https://...", "hits": 42 }
```
- Exemplo (cURL):
```bash
curl -i http://localhost:8080/stats/ABCDE
```

### 4) Ranking de URLs
- `GET /ranking`
- 200 OK (array de `RankingItem`):
```json
[ { "code": "ABCDE", "hits": 42 }, { "code": "1xJOx", "hits": 17 } ]
```
- Exemplo (cURL):
```bash
curl -i http://localhost:8080/ranking
```

## Modelos de erro
- `ErrorResponse`: `{ "error": "...", "message": "..." }`
- `ValidationErrorResponse`: `{ "error": "Erro de validação", "details": { "campo": "mensagem" } }`

## Migrações de Banco (Flyway)
- Scripts em `src/main/resources/db/migration` (ex.: `V1__init.sql`).
- Aplicadas automaticamente ao iniciar a aplicação.

## Health e Observabilidade
- `GET /actuator/health` e `GET /actuator/health/readiness`
- `GET /actuator/info` e `GET /actuator/metrics`

## Testes e Cobertura
- Perfil de testes usa H2 em memória (arquivo `src/test/resources/application-test.yaml`).
- Executar testes:
```bash
mvn test
```
- Relatório Jacoco: `target/site/jacoco/index.html`

## Integração com o Frontend
- O frontend (Angular) utiliza proxy para o backend em `http://localhost:8080` (`frontend/proxy.conf.json`).
- É possível (no frontend) gerar tipos TypeScript a partir do OpenAPI do backend:
  - Script: `yarn api:types`
  - Endpoint usado: `http://localhost:8080/v3/api-docs`

## Estrutura (parcial)
```
backend/
├── pom.xml
├── Dockerfile
├── src/main/java/br/corp/shortener/
│   ├── UrlShortenerApplication.java
│   ├── controllers/
│   ├── services/
│   ├── entities/
│   ├── repositories/
│   └── dto/
└── src/main/resources/
    ├── application.yaml
    └── db/migration/
```

## Troubleshooting
- Porta 8080 em uso:
  - Ajuste `server.port` no `application.yaml` ou finalize o processo que ocupa a porta.
- Erro de conexão com PostgreSQL:
  - Verifique host/porta/usuário/senha; confirme se o DB está ativo; ajuste `spring.datasource.*`.
- Swagger UI “Failed to fetch”:
  - Acesse a UI do backend, limpe filtros, desative extensões bloqueadoras e garanta que o backend está no ar.
- Erros de build/execução por dependências ausentes:
  - Use Maven (`mvn spring-boot:run` ou `mvn package`) e evite rodar classes diretamente.

## Licença
Sem licença explícita definida.