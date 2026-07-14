param(
    [string] $Source = "totvs-winthor",
    [string] $Modulo,
    [switch] $All
)

$ErrorActionPreference = "Stop"

if (($All -and -not [string]::IsNullOrWhiteSpace($Modulo)) -or
    (-not $All -and [string]::IsNullOrWhiteSpace($Modulo))) {
    throw "Informe exatamente uma opção: -Modulo <chave> ou -All."
}

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..\..\..\..\..\..\..")
$catalogFile = Join-Path $repoRoot "src\main\resources\collector\totvs-winthor-modules.yaml"

function Get-ModuleKeys {
    Get-Content -LiteralPath $catalogFile -Encoding UTF8 |
        Where-Object { $_ -match '^\s*-\s+key:\s*(.+?)\s*$' } |
        ForEach-Object { $Matches[1].Trim('"', "'") }
}

function Invoke-Importer([string] $Scope) {
    $arguments = @(
        "--spring.main.web-application-type=none",
        "--spring.main.banner-mode=off",
        "--spring.devtools.restart.enabled=false",
        "--spring.devtools.livereload.enabled=false",
        "--spring.devtools.add-properties=false",
        "--debug=false",
        "--logging.level.root=ERROR",
        "--logging.level.org.hibernate.SQL=OFF",
        "--spring.jpa.show-sql=false",
        "--clipper.catalog.import.enabled=true",
        "--clipper.catalog.import.source-type=$Source",
        "--clipper.catalog.import.scope=$Scope"
    )
    & .\mvnw.cmd -q spring-boot:run "-Dspring-boot.run.arguments=$($arguments -join ' ')"
    if ($LASTEXITCODE -ne 0) {
        throw "A importação de $Source/$Scope falhou com código $LASTEXITCODE."
    }
}

$scopes = if ($All) { @(Get-ModuleKeys) } else { @($Modulo) }
Push-Location $repoRoot
try {
    foreach ($scope in $scopes) {
        Invoke-Importer $scope
    }
}
finally {
    Pop-Location
}
