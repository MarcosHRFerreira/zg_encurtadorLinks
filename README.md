# URL Shortener – Monorepo (Backend + Frontend)

Monorepo com serviço de encurtamento de URLs (Spring Boot) e frontend (Angular). Inclui redirecionamento por código curto, estatísticas, ranking e documentação OpenAPI/Swagger.

## Estrutura do projeto
```
zg/
├── backend/           # API Spring Boot
├── frontend/          # App Angular
├── docker-compose.yml # Subir DB + backend
```

## Pré-requisitos
- Java 21 (JAVA_HOME configurado) e Maven 3.9+
- Node.js e Yarn
- Docker e Docker Compose (opcional, porém recomendado para subir tudo rápido)

## Quick Start

### Opção A) Docker Compose (recomendado)
Sobe PostgreSQL + Backend com um comando.

```bash
docker compose up -d --build
```
- Backend: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui/index.html
- Health: http://localhost:8080/actuator/health/readiness

Para parar:
```bash
docker compose down
```

Em paralelo, rode o frontend (ver seção Frontend abaixo).

### Opção B) Desenvolvimento local
- Backend (Maven):
  ```bash
  cd backend
  mvn -q -DskipTests spring-boot:run
  # Swagger: http://localhost:8080/swagger-ui/index.html
  ```
- Frontend (Yarn):
  ```bash
  cd frontend
  yarn install
  # (Opcional) gerar tipos a partir do OpenAPI do backend (precisa do backend rodando)
  yarn api:types
  # Iniciar em uma das portas disponíveis
  yarn start --port 4201   # disponível em http://localhost:4201/
  # ou
  yarn ng serve --port 4203 --host localhost  # disponível em http://localhost:4203/
  ```

Rotas principais do app:
- http://localhost:4201/shorten (ou http://localhost:4203/shorten)
- http://localhost:4201/consultas (ou http://localhost:4203/consultas)

O frontend utiliza proxy para o backend em http://localhost:8080 (arquivo frontend/proxy.conf.json).

## Scripts úteis (Frontend)
Dentro de `frontend/`:
- `yarn start` – inicia o servidor de desenvolvimento (porta padrão do Angular; use `--port` para customizar)
- `yarn build` – build de produção
- `yarn test` – executa testes (Jest)
- `yarn api:types` – gera tipos TypeScript a partir do OpenAPI do backend em `src/app/core/api/types.ts`

## Fluxo de desenvolvimento recomendado
1) Suba o backend (Docker Compose ou `mvn spring-boot:run`).
2) Acesse o Swagger para confirmar a API: http://localhost:8080/swagger-ui/index.html
3) No frontend, rode `yarn api:types` para sincronizar os tipos a partir do OpenAPI.
4) Inicie o frontend com `yarn start --port 4201` (ou 4203).
5) Desenvolva e valide nas rotas “shorten” e “consultas”.

## Testes
- Backend: `cd backend && mvn test` (perfil de testes usa H2, Jacoco em `target/site/jacoco/index.html`).
- Frontend: `cd frontend && yarn test` (Jest + jest-preset-angular + jsdom).

## Variáveis de ambiente e CORS

- Backend:
  - CORS_ALLOWED_ORIGINS: lista de origens permitidas separadas por vírgula. Exemplo (dev/prod): https://zg-encurtador-links.vercel.app,http://localhost:4200,http://localhost:4201,http://localhost:4203
  - CORS_ALLOWED_ORIGIN_PATTERNS: padrões curinga opcionais (ex.: https://*.vercel.app)
  - PORT: porta definida pelo provedor (a app usa server.port=${PORT:8080})
- Frontend:
  - Em build de produção, a URL da API é injetada via public/env.js usando API_BASE_URL.
  - Exemplo local: PowerShell (Windows) -> $env:API_BASE_URL='http://localhost:8080'; yarn build
    - bash/zsh (Linux/macOS) -> API_BASE_URL=http://localhost:8080 yarn build
  - No Vercel, defina API_BASE_URL nos ambientes Production/Preview.

## Documentação detalhada
- Backend: consulte [backend/README.md](backend/README.md)
- Frontend: consulte [frontend/README.md](frontend/README.md)