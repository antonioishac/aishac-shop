# config-server

Spring Cloud Config Server servindo `config-repo/` no perfil **`native`** (lê do disco; no container, `config-repo/` é montado em `/config-repo` via `CONFIG_REPO_LOCATION`).

- Nenhum segredo em `config-repo/`: apenas referências `${VAR}`. O `ConfigRepoSecretsTest` reprova chaves sensíveis (`password`, `secret`, `token`, ...) com valor literal.
- Perfis: `local` (docker-compose + LocalStack), `dev` (AWS real), `prod` (só configuração, não implantado).
- Arquivos `.properties`, do mais geral ao mais específico: `application.properties` → `application-<perfil>.properties` → `<servico>.properties` → `<servico>-<perfil>.properties`.
- Novas propriedades devem existir nos três perfis quando variam por ambiente (skill `configurar-ambiente`).
- O `ConfigServerIT` sobe o servidor contra o `config-repo/` real e confere cada serviço × perfil: rode `./mvnw verify` depois de mexer no `config-repo/`.
- Rodar fora do Docker: `./mvnw spring-boot:run` (porta 8888). Ex.: `curl localhost:8888/sales-service/local`.
