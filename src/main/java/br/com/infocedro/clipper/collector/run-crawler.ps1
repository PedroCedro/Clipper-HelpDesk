param(
    [string] $Modulo,
    [switch] $All,
    [int] $MaxSections = 0,
    [int] $MaxArticles = 0
)

$ErrorActionPreference = "Stop"

if (($All -and -not [string]::IsNullOrWhiteSpace($Modulo)) -or
    (-not $All -and [string]::IsNullOrWhiteSpace($Modulo))) {
    throw "Informe exatamente uma opção: -Modulo <chave> ou -All."
}

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..\..\..\..\..\..\..")
$catalogFile = Join-Path $repoRoot "src\main\resources\collector\totvs-winthor-modules.yaml"

function Get-ModuleKeys {
    # O script conhece apenas as chaves públicas. IDs e nomes continuam no
    # catálogo do adapter, evitando duas listas de configuração concorrentes.
    Get-Content -LiteralPath $catalogFile -Encoding UTF8 |
        Where-Object { $_ -match '^\s*-\s+key:\s*(.+?)\s*$' } |
        ForEach-Object { $Matches[1].Trim('"', "'") }
}

function Invoke-Collector([string] $ModuleKey) {
    $arguments = @(
        "--spring.main.web-application-type=none",
        "--spring.main.log-startup-info=false",
        "--spring.devtools.restart.enabled=false",
        "--spring.devtools.livereload.enabled=false",
        "--spring.devtools.add-properties=false",
        "--spring.main.banner-mode=off",
        "--debug=false",
        "--logging.level.root=ERROR",
        "--clipper.collector.totvs-winthor.enabled=true",
        "--clipper.collector.totvs-winthor.module=$ModuleKey"
    )

    if ($MaxSections -gt 0) {
        $arguments += "--clipper.collector.totvs-winthor.max-sections=$MaxSections"
    }
    if ($MaxArticles -gt 0) {
        $arguments += "--clipper.collector.totvs-winthor.max-articles=$MaxArticles"
    }

    Write-Host "Coletando módulo $ModuleKey..."
    & .\mvnw.cmd -q spring-boot:run "-Dspring-boot.run.arguments=$($arguments -join ' ')"
    if ($LASTEXITCODE -ne 0) {
        throw "A coleta do módulo $ModuleKey falhou com código $LASTEXITCODE."
    }
}

$modules = if ($All) { @(Get-ModuleKeys) } else { @($Modulo) }
if ($modules.Count -eq 0) {
    throw "Nenhum módulo foi encontrado em $catalogFile."
}

Push-Location $repoRoot
try {
    foreach ($moduleKey in $modules) {
        Invoke-Collector $moduleKey
    }
}
finally {
    Pop-Location
}
