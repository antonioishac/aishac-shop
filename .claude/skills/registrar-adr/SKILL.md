---
name: registrar-adr
description: Registra uma decisão de arquitetura (ADR) numerada na pasta da fase atual, no formato padrão do projeto. Use ao tomar ou revisar uma decisão técnica relevante.
argument-hint: "[título da decisão]"
---

# Registrar ADR: $ARGUMENTS

1. Identifique a fase atual (`docs/evolucao/<fase>/`) e o próximo número em `adr/` (`ADR-001`, `ADR-002`…; numeração contínua entre fases).
2. Crie `adr/ADR-<nnn>-<titulo-em-kebab-case>.md` a partir de [template.md](template.md).
3. Se a decisão substitui outra, marque a antiga como `Substituída por ADR-<nnn>`.
4. Se a decisão veio do registro de concepção (`D01…D26` em `docs/backlog/README.md`), referencie o código.
5. Atualize o diagrama se a decisão mudar a arquitetura (skill `atualizar-diagrama`).
