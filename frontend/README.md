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
Rotas principais:
- http://localhost:4201/shorten (ou http://localhost:4203/shorten)
- http://localhost:4201/consultas (ou http://localhost:4203/consultas)

Proxy para o backend:
- O app usa `proxy.conf.json` para encaminhar chamadas à API do backend em `http://localhost:8080`.

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
- Em desenvolvimento (`yarn start`), o Angular usa `proxy.conf.json` e encaminha `/shorten`, `/stats`, `/ranking` para `http://localhost:8080`.

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
