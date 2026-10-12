## O que muda

<!-- Resumo em 1 a 3 frases. Por que esta mudança existe? -->

**Item do backlog:** F01-XXX
**Spec:** `specs/<contexto>/<id>-<nome>/spec.md` (se for FEAT)

## Como testar

<!-- Comandos (task verify:<servico>), requisições do http/ ou passos na tela. -->

## Definition of Done

- [ ] Segue as rules (`.claude/rules/`) e passa no `ArchitectureTest`
- [ ] Testes unitários, de fatia e de integração passando; cobertura mínima atingida (`./mvnw verify`)
- [ ] Endpoints documentados (OpenAPI) e coleção `http/` atualizada
- [ ] Revisão do `code-reviewer` (contexto limpo) sem pendências
- [ ] Diagrama e ADRs atualizados, se a arquitetura mudou
- [ ] `CHANGELOG.md` do serviço atualizado em `[Unreleased]`
- [ ] Status do item atualizado no backlog
- [ ] CI verde; commits no padrão Conventional Commits com `Refs: F01-XXX`

## Observações para revisão

<!-- Decisões tomadas, dúvidas, limitações conhecidas, pontos para docs/evolucao/<fase>/limitacoes.md. -->
