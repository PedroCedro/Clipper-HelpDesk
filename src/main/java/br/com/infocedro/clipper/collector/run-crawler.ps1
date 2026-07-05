param(
    [int] $MaxSections = 0,
    [int] $MaxArticles = 0
)

$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..\..\..\..\..\..\..")
$arguments = @(
    "--spring.main.web-application-type=none",
    "--spring.main.log-startup-info=false",
    "--spring.devtools.restart.enabled=false",
    "--spring.devtools.livereload.enabled=false",
    "--spring.devtools.add-properties=false",
    "--spring.main.banner-mode=off",
    "--debug=false",
    "--logging.level.root=ERROR",
    "--clipper.collector.totvs-winthor.enabled=true"
)

if ($MaxSections -gt 0) {
    $arguments += "--clipper.collector.totvs-winthor.max-sections=$MaxSections"
}

if ($MaxArticles -gt 0) {
    $arguments += "--clipper.collector.totvs-winthor.max-articles=$MaxArticles"
}

Push-Location $repoRoot
try {
    & .\mvnw.cmd -q spring-boot:run "-Dspring-boot.run.arguments=$($arguments -join ' ')"
}
finally {
    Pop-Location
}
