param(
    [string]$DatabasePath = "src/main/java/br/com/infocedro/clipper/collector/totvs-winthor"
)

$projectRoot = Resolve-Path (Join-Path $PSScriptRoot "../../../../../../../..")
Push-Location $projectRoot
try {
    & .\mvnw.cmd spring-boot:run `
        "-Dspring-boot.run.arguments=--spring.main.web-application-type=none --clipper.catalog.legacy-migration.enabled=true --clipper.catalog.legacy-migration.database-path=$DatabasePath --logging.level.root=ERROR --spring.jpa.show-sql=false"
    if ($LASTEXITCODE -ne 0) {
        throw "Migração legada falhou com código $LASTEXITCODE."
    }
} finally {
    Pop-Location
}
