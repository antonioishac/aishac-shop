# Coleção de requisições (`.http`)

Formato do **HTTP Client do IntelliJ** (também lido pelo plugin httpYac no VS Code). Abra um arquivo `.http`, escolha o ambiente **`local`** (`http-client.env.json`) e execute cada bloco.

| Arquivo | Serviço |
|---|---|
| `config-server.http` | Config Server (8888) |
| `customer-service.http` | customer-service (8081) |
| `sales-service.http` | sales-service (8082) |

- Toda feature que cria ou muda endpoint atualiza o arquivo do serviço (Definition of Done).
- Nada de segredos aqui: valores sensíveis vão em `http-client.private.env.json`, que está no `.gitignore`.
