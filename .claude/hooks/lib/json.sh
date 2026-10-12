#!/usr/bin/env bash
# Leitura do JSON de entrada dos hooks sem depender só do jq (ausente no Git Bash do Windows).
#
# json_get "<json>" campo.caminho [outro.caminho ...]
#   Imprime o primeiro campo não vazio (booleanos como true/false). Usa jq; na falta dele, Python.
#   Sem nenhum dos dois, retorna 3: quem chama decide se bloqueia (guardas) ou segue (on-stop).

_json_python() {
  local candidate
  for candidate in python3 python py; do
    # Descarta o atalho da Microsoft Store, que existe no PATH mas não executa nada.
    if command -v "$candidate" >/dev/null 2>&1 && "$candidate" -c "" >/dev/null 2>&1; then
      echo "$candidate"
      return 0
    fi
  done
  return 1
}

json_get() {
  local input="$1"
  shift
  if command -v jq >/dev/null 2>&1; then
    local filter=""
    local path
    for path in "$@"; do filter+=".${path} // "; done
    jq -r "${filter}empty" <<<"$input"
    return 0
  fi
  local python
  python=$(_json_python) || return 3
  "$python" -c '
import json, sys
data = json.loads(sys.stdin.read() or "{}")
for path in sys.argv[1:]:
    value = data
    for key in path.split("."):
        value = value.get(key) if isinstance(value, dict) else None
    if value not in (None, ""):
        print(str(value).lower() if isinstance(value, bool) else value)
        break
' "$@" <<<"$input"
}
