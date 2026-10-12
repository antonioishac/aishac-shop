---
name: atualizar-diagrama
description: Cria ou atualiza o diagrama draw.io da fase atual (arquivo .drawio em XML, com páginas C4, sequências, máquina de estados, deployment e ER), mantendo convenções visuais. Use quando a arquitetura, o modelo de dados ou um fluxo mudar.
argument-hint: "[o que mudou]"
---

# Atualizar diagrama da fase

Arquivo: `docs/evolucao/<fase>/aishac-shop.drawio`. Convenções em [conventions.md](conventions.md).

1. Leia o `.drawio` atual e localize a(s) página(s) afetada(s) por: $ARGUMENTS.
2. Edite o XML preservando `id`s existentes (para diffs legíveis) e o layout das demais células.
3. Mantenha **uma página por visão**; não misture níveis C4 na mesma página.
4. Valide que o arquivo é XML bem formado (`xmllint --noout` se disponível, ou parser Python).
5. Liste ao usuário as páginas alteradas e o que mudou em cada uma. Ele abre o arquivo no draw.io (desktop, app.diagrams.net ou extensão do VS Code).
