<#
.SYNOPSIS
  Prepara o ambiente local do Aishac Shop sem expor senhas a ninguem (nem ao Claude Code).

.DESCRIPTION
  1. Cria infra/docker/.env a partir do .env.example, com senhas aleatorias para o superusuario,
     customer_app, sales_app e claude_mcp (so se o .env ainda nao existir).
  2. Define, no seu usuario do Windows, CLAUDE_PG_CUSTOMERS_URL e CLAUDE_PG_SALES_URL para o MCP,
     usando a senha do claude_mcp e a porta do .env.
  Nenhuma senha e exibida. O LOCALSTACK_AUTH_TOKEN continua para voce preencher (F01-018).

.PARAMETER PostgresPort
  Porta do host para o PostgreSQL do projeto. Use 5433 se a 5432 ja estiver ocupada por outro container.

.EXAMPLE
  powershell -ExecutionPolicy Bypass -File .\setup-local-env.ps1 -PostgresPort 5433
#>
param([int]$PostgresPort = 5432)

$ErrorActionPreference = 'Stop'
$envFile = Join-Path $PSScriptRoot '.env'
$exampleFile = Join-Path $PSScriptRoot '.env.example'

function New-Secret([int]$Length = 32) {
    # So letras e digitos: dispensa codificacao na URL do MCP e no .env.
    $chars = [char[]]'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789'
    $bytes = New-Object byte[] $Length
    [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
    -join ($bytes | ForEach-Object { $chars[$_ % $chars.Length] })
}

function Read-EnvFile([string]$Path) {
    $values = @{}
    foreach ($line in Get-Content $Path) {
        if ($line -match '^\s*([A-Z0-9_]+)=(.*)$') { $values[$Matches[1]] = $Matches[2] }
    }
    $values
}

if (Test-Path $envFile) {
    Write-Host '.env ja existe: mantido sem alteracoes (senhas so valem na criacao do volume).'
} else {
    $generated = @{
        POSTGRES_SUPERUSER_PASSWORD = New-Secret
        CUSTOMER_DB_PASSWORD        = New-Secret
        SALES_DB_PASSWORD           = New-Secret
        CLAUDE_MCP_DB_PASSWORD      = New-Secret
        POSTGRES_PORT               = "$PostgresPort"
    }
    $lines = foreach ($line in Get-Content $exampleFile) {
        if ($line -match '^([A-Z0-9_]+)=' -and $generated.ContainsKey($Matches[1])) {
            "$($Matches[1])=$($generated[$Matches[1]])"
        } else {
            $line
        }
    }
    # UTF-8 sem BOM e LF: formato que o Docker Compose le sem surpresas.
    [IO.File]::WriteAllText($envFile, (($lines -join "`n") + "`n"), (New-Object Text.UTF8Encoding $false))
    Write-Host ".env criado com senhas aleatorias (porta do PostgreSQL: $PostgresPort)."
}

$values = Read-EnvFile $envFile
$port = if ($values['POSTGRES_PORT']) { $values['POSTGRES_PORT'] } else { '5432' }
$mcpPassword = $values['CLAUDE_MCP_DB_PASSWORD']
if (-not $mcpPassword) { throw 'CLAUDE_MCP_DB_PASSWORD vazio no .env.' }

foreach ($db in @{ CLAUDE_PG_CUSTOMERS_URL = 'aishac_customer'; CLAUDE_PG_SALES_URL = 'aishac_sales' }.GetEnumerator()) {
    $url = "postgresql://claude_mcp:$mcpPassword@localhost:$port/$($db.Value)"
    [Environment]::SetEnvironmentVariable($db.Key, $url, 'User')
    Write-Host "$($db.Key) definida no seu usuario (banco $($db.Value), porta $port)."
}

if (-not $values['LOCALSTACK_AUTH_TOKEN']) {
    Write-Host 'Pendente: LOCALSTACK_AUTH_TOKEN no .env (F01-018). Sem ele, so o postgres sobe.'
}
Write-Host 'Pronto. Feche e reabra o VS Code / Claude Code para as variaveis valerem.'
