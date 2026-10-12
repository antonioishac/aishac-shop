#!/usr/bin/env bash
# PreToolUse (Bash)
# Complementa as regras "deny" do settings.json, cobrindo comandos compostos
# (ex.: "cd infra && docker compose up").
# Bloqueia:
#   1. docker / docker compose / podman (o usuário controla os containers);
#   2. terraform apply / destroy (infra paga é decisão do usuário);
#   3. git push --force;
#   4. leitura de arquivos .env por comando;
#   5. git commit com possíveis segredos no conteúdo staged.
set -uo pipefail

input=$(cat)
cmd=$(jq -r '.tool_input.command // empty' <<<"$input")
[[ -z "$cmd" ]] && exit 0

block() { echo "Bloqueado: $1" >&2; exit 2; }

# Separa o comando em segmentos (&&, ||, ;, |, quebras de linha) para checar cada um.
segments=$(printf '%s\n' "$cmd" | sed -E 's/(&&|\|\||;|\|)/\n/g')

while IFS= read -r seg; do
  seg="${seg#"${seg%%[![:space:]]*}"}"   # trim à esquerda
  seg="${seg#sudo }"
  first=$(awk '{print $1}' <<<"$seg")
  case "$first" in
    docker|docker-compose|podman|podman-compose|nerdctl)
      block "o Claude não executa '$first'. Informe o comando ao usuário para que ele execute (premissa P1)." ;;
    terraform|tofu)
      if grep -Eq '(^|[[:space:]])(apply|destroy)([[:space:]]|$)' <<<"$seg"; then
        block "'terraform apply/destroy' é executado apenas pelo usuário (premissa P3). Use 'terraform plan'."
      fi ;;
    git)
      if grep -Eq 'push' <<<"$seg" && grep -Eq '(--force|--force-with-lease|[[:space:]]-f([[:space:]]|$))' <<<"$seg"; then
        block "git push forçado é proibido."
      fi ;;
  esac
done <<<"$segments"

# 4. Leitura de .env por comandos (cat, less, grep, source...), exceto .env.example
if grep -Eq '(^|[[:space:]/"'"'"'])\.env([[:space:]"'"'"']|$|\.[a-zA-Z]+)' <<<"$cmd" \
   && ! grep -Eq '\.env\.example' <<<"$cmd"; then
  block "arquivos .env contêm credenciais do usuário e não podem ser lidos ou carregados (premissa P2)."
fi

# 5. Varredura simples de segredos antes de commit
if grep -Eq '(^|[[:space:];&|])git[[:space:]]+commit' <<<"$cmd"; then
  project_dir="${CLAUDE_PROJECT_DIR:-$(pwd)}"
  staged=$(git -C "$project_dir" diff --cached -U0 2>/dev/null | grep -E '^\+' | grep -vE '^\+\+\+' || true)
  if grep -Eq 'AKIA[0-9A-Z]{16}|ASIA[0-9A-Z]{16}|-----BEGIN [A-Z ]*PRIVATE KEY-----|aws_secret_access_key|LOCALSTACK_AUTH_TOKEN=[^$[:space:]]+' <<<"$staged"; then
    block "possível segredo no conteúdo staged (chave AWS, chave privada ou token). Remova antes de commitar."
  fi
  if grep -Eiq '(password|passwd|secret|token)[[:space:]]*[=:][[:space:]]*[^$\{[:space:]][^[:space:]]{5,}' <<<"$staged"; then
    block "possível senha/token literal no conteúdo staged. Use \${VARIAVEL} em vez de valores reais."
  fi
fi

exit 0
