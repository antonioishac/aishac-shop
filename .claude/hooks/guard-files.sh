#!/usr/bin/env bash
# PreToolUse (Edit|Write|MultiEdit|NotebookEdit)
# Bloqueia:
#   1. edição de migration Flyway já commitada (checksum do Flyway quebraria);
#   2. escrita em arquivos .env (credenciais são do usuário; .env.example é permitido).
set -uo pipefail

input=$(cat)
source "$(dirname "${BASH_SOURCE[0]}")/lib/json.sh"
file=$(json_get "$input" tool_input.file_path tool_input.notebook_path) || { echo "Bloqueado: instale jq ou Python para os hooks validarem arquivos." >&2; exit 2; }
[[ -z "$file" ]] && exit 0

project_dir="${CLAUDE_PROJECT_DIR:-$(pwd)}"
rel="${file#"$project_dir"/}"
base=$(basename "$file")

# 2. Arquivos .env (exceto .env.example)
if [[ "$base" == ".env" || ( "$base" == .env.* && "$base" != ".env.example" ) ]]; then
  echo "Bloqueado: '$rel' guarda credenciais do usuário. Altere apenas o .env.example (sem valores reais)." >&2
  exit 2
fi

# 1. Migration Flyway versionada já commitada
if [[ "$rel" == */db/migration/V*__*.sql ]]; then
  if git -C "$project_dir" ls-files --error-unmatch "$rel" >/dev/null 2>&1; then
    echo "Bloqueado: '$rel' já está commitada e é imutável. Crie uma nova migration (skill gerar-flyway)." >&2
    exit 2
  fi
fi

exit 0
