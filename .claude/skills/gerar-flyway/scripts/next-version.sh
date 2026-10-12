#!/usr/bin/env bash
# Imprime o próximo número de versão Flyway de um serviço (portável: Linux, macOS, WSL).
# Uso: next-version.sh services/<servico>
set -euo pipefail

service_dir="${1:?Informe o diretório do serviço, ex.: services/sales-service}"
migration_dir="$service_dir/src/main/resources/db/migration"

last=0
if [[ -d "$migration_dir" ]]; then
  for file in "$migration_dir"/V*__*.sql; do
    [[ -e "$file" ]] || continue
    name=$(basename "$file")
    version=${name#V}
    version=${version%%__*}
    if [[ "$version" =~ ^[0-9]+$ ]] && (( 10#$version > last )); then
      last=$((10#$version))
    fi
  done
fi

echo $(( last + 1 ))
