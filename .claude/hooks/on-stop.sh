#!/usr/bin/env bash
# Stop
# Ao final de uma tarefa, para cada serviço Java ou frontend com arquivos alterados:
#   - aplica a formatação (Spotless) e roda os testes rápidos (./mvnw test);
#   - no frontend, roda lint e testes.
# Se algo falhar, devolve o erro ao Claude (exit 2) para que ele corrija antes de encerrar.
# Os testes de integração (./mvnw verify) ficam a cargo da Definition of Done e do CI.
set -uo pipefail

input=$(cat)

# Evita laço infinito: se já estamos continuando por causa deste hook, não roda de novo.
if [[ "$(jq -r '.stop_hook_active // false' <<<"$input")" == "true" ]]; then
  exit 0
fi

project_dir="${CLAUDE_PROJECT_DIR:-$(pwd)}"
cd "$project_dir" || exit 0
git rev-parse --is-inside-work-tree >/dev/null 2>&1 || exit 0

changed=$(git status --porcelain | awk '{print $NF}')
[[ -z "$changed" ]] && exit 0

failures=""

# Serviços Java
for service_dir in services/*/; do
  service=${service_dir%/}
  grep -q "^$service/" <<<"$changed" || continue
  [[ -x "$service/mvnw" ]] || continue

  if ! (cd "$service" && ./mvnw -q -B spotless:apply >/dev/null 2>&1); then
    failures+="\n- $service: falha ao aplicar Spotless"
  fi
  if ! out=$(cd "$service" && ./mvnw -q -B test 2>&1); then
    failures+="\n- $service: './mvnw test' falhou:\n$(tail -n 40 <<<"$out")"
  fi
done

# Frontend
web="web/aishac-shop-web"
if grep -q "^$web/" <<<"$changed" && [[ -d "$web/node_modules" ]]; then
  if ! out=$(cd "$web" && npm run -s lint 2>&1 && npm run -s test -- --run 2>&1); then
    failures+="\n- $web: lint/testes falharam:\n$(tail -n 40 <<<"$out")"
  fi
fi

if [[ -n "$failures" ]]; then
  printf "Verificação automática falhou. Corrija antes de encerrar:%b\n" "$failures" >&2
  exit 2
fi

exit 0
