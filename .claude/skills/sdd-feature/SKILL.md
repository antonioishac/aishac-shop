---
name: sdd-feature
description: Conduz uma funcionalidade do backlog pelo ciclo completo de Spec-Driven Development (spec, modelo de dados, testes, implementação, revisão), com portões de aprovação do usuário. Use ao iniciar qualquer item FEAT do backlog.
argument-hint: "[ID-do-backlog, ex.: F01-040]"
disable-model-invocation: true
---

# Ciclo SDD para $ARGUMENTS

Você é o **Architect-Core**: orquestra os subagentes, não implementa sozinho.

## 0. Preparação
1. Localize `$ARGUMENTS` em `docs/backlog/`. Confirme que é `FEAT`, que as dependências estão `✅` e identifique a fase (diretório `docs/evolucao/<fase>/`).
2. Verifique a **Definition of Ready** (`docs/backlog/README.md`). Itens faltando → resolva nas etapas abaixo.
3. Marque o item como `🔄` no backlog.

## 1. Spec
- Delegue ao `spec-writer`, que usa [templates/spec.md](templates/spec.md) e cria `contract.yaml`.
- Apresente ao usuário um resumo: regras, critérios de aceite, endpoints, perguntas em aberto.
- ⛔ **PORTÃO 1 — aguarde aprovação explícita da spec.** Ajuste até aprovar.

## 2. Modelo de dados (se houver persistência)
- Delegue ao `database-migration-specialist` (skill `modelar-dados`).
- ⛔ **PORTÃO 2 — aguarde aprovação explícita do modelo.**
- Aprovado: o mesmo agente cria migration e entidades.

## 3. Plano e testes
- Escreva `plan.md` a partir de [templates/plan.md](templates/plan.md) e `tasks.md` a partir de [templates/tasks.md](templates/tasks.md).
- Delegue ao `testing-specialist`: testes que falham, mapeados aos critérios de aceite.

## 4. Implementação
- Backend/BFF → `backend-java-engineer`. Frontend → `frontend-react-engineer`.
- Ao final: `./mvnw verify` (e/ou `npm run test && npm run build`) verde.

## 5. Revisão
- Delegue ao `code-reviewer` (contexto limpo). `REPROVADO` → devolva os bloqueantes ao engenheiro e repita.
- Se a arquitetura mudou → `architecture-guardian` (diagrama/ADR).

## 6. Conclusão
- Confira a **Definition of Done** item a item, mostrando a evidência de cada um.
- Marque o item como `✅` no backlog e proponha a mensagem de commit (Conventional Commits, `Refs: $ARGUMENTS`).
- Sugira ao usuário o que **observar** para o `limitacoes.md`, sem preenchê-lo.
