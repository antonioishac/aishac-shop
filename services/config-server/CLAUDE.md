# config-server

Spring Cloud Config Server servindo `config-repo/`.

- Nenhum segredo em `config-repo/`: apenas referências `${VAR}`.
- Perfis: `local` (docker-compose + LocalStack), `dev` (AWS real), `prod` (só configuração, não implantado).
- Arquivos `.properties`. Comuns em `application.properties`; específicos em `<servico>.properties` e `<servico>-<perfil>.properties`.
- Novas propriedades devem existir nos três perfis quando variam por ambiente (skill `configurar-ambiente`).
