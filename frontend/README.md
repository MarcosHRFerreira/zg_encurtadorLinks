# Frontend (Angular)

Aplicação Angular que consome a API de encurtamento de URLs. Inclui telas para encurtar URLs e consultar estatísticas/ranking.

## Pré-requisitos
- Node.js LTS
- Yarn

## Instalação
```bash
yarn install
```

## Desenvolvimento
- Iniciar servidor dev em porta customizada (recomendado 4201 ou 4203):
```bash
yarn start --port 4201
# ou
yarn ng serve --port 4203 --host localhost
```
Rotas principais (SPA):
- http://localhost:4201/shorten (ou http://localhost:4203/shorten)
- http://localhost:4201/consultas (ou http://localhost:4203/consultas)

Proxy para o backend:
- Em desenvolvimento, o app usa `proxy.conf.json` para encaminhar chamadas à API do backend em `http://localhost:8080` via prefixo `/api`.
- Ex.: frontend chama `POST /api/shorten`, `GET /api/stats/{code}`, `GET /api/ranking`.

## Geração de tipos a partir do OpenAPI do backend
Gere os tipos TypeScript do backend (precisa do backend rodando em `http://localhost:8080`).
```bash
yarn api:types
```
Resultado: arquivo gerado em `src/app/core/api/types.ts`.

## Variáveis de ambiente e URL da API
- Em builds de produção, a URL da API é injetada no arquivo `public/env.js` via variável `API_BASE_URL` durante o script `vercel-build`.
  - Exemplo local (Windows/PowerShell):
    ```powershell
    $env:API_BASE_URL='http://localhost:8080'; yarn build
    ```
  - Exemplo local (bash/zsh):
    ```bash
    API_BASE_URL=http://localhost:8080 yarn build
    ```
  - No Vercel, configure `API_BASE_URL` em Production/Preview.
- Em desenvolvimento (`yarn start`), o Angular usa `proxy.conf.json` e encaminha `/api/*` para `http://localhost:8080`.

## Deploy no Render (frontend)
- Tipo de serviço recomendado: "Static Site".
- Build Command: use o script que injeta `API_BASE_URL` e executa o build (por exemplo, `yarn vercel-build` ou `yarn build` se já estiver configurado para escrever `public/env.js`).
- Publish Directory: o diretório de saída do Angular (geralmente `dist/frontend` ou `dist/<nome-do-projeto>`; verifique em `angular.json`).
- Variáveis necessárias:
  - `API_BASE_URL`: URL pública do backend no Render (ex.: `https://<seu-backend>.onrender.com`).
- SPA fallback: habilite o fallback de SPA para que rotas como `/shorten` sirvam `index.html` (pode ser feito via configuração de rotas de Static Site nas settings do Render ou servidor estático com fallback).
- Observação de URLs: em produção, o interceptor do frontend remove o prefixo `/api` automaticamente ao compor a URL com `API_BASE_URL`, mapeando `POST /api/shorten` para `POST <API_BASE_URL>/shorten`.

## Backend no Render
- O backend já está preparado para a porta do Render via `server.port: ${PORT:8080}` em `application.yaml`.
- Configure CORS para o domínio do frontend usando variáveis:
  - `CORS_ALLOWED_ORIGINS` (lista separada por vírgula) ou `CORS_ALLOWED_ORIGIN_PATTERNS` (patterns).
  - Ex.: `CORS_ALLOWED_ORIGINS=https://<seu-frontend>.onrender.com` ou `CORS_ALLOWED_ORIGIN_PATTERNS=https://*.onrender.com`.
- Banco de dados: defina `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` com as credenciais do Render PostgreSQL.
- Health check: opcional usar `/actuator/health` nas configurações do serviço.

## Scripts úteis
```bash
yarn start              # ng serve (usa proxy.conf.json e permite --port)
yarn build              # build de produção (usa API_BASE_URL se definido)
yarn test               # testes com Jest
yarn api:types          # gera tipos a partir do OpenAPI do backend
```

## Testes
- Framework: Jest (com jest-preset-angular) + jsdom.
- Rodar testes:
```bash
yarn test
```
- Estrutura recomendada neste repo:
  - Testes de integração dos endpoints HTTP em `test/integration`.
  - Testes de unidade em `test/unit`.
  - Use Arrange-Act-Assert e sinon para stubs/mocks quando necessário.

## Estrutura (parcial)
```
frontend/
├── angular.json
├── package.json
├── proxy.conf.json
├── src/
│   ├── app/
│   │   └── core/
│   │       └── api/
│   │           └── types.ts  # gerado por openapi-typescript
│   ├── index.html
│   ├── main.ts
│   └── styles.scss
└── test/
    ├── integration/
    └── unit/
```

## Dicas
- Após atualizar a API do backend, rode `yarn api:types` para manter os tipos do frontend sincronizados.
- Se o backend não estiver acessível, a geração de tipos falhará. Certifique-se de que está rodando em `http://localhost:8080`.
