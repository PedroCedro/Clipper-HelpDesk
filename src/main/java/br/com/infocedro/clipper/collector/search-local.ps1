param(
    [Parameter(Mandatory = $true)]
    [string] $Modulo,

    [Parameter(Mandatory = $true)]
    [string] $Query,

    [int] $Limit = 10
)

$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..\..\..\..\..\..\..")
$moduleRoot = Join-Path $repoRoot "var\knowledge\raw\totvs-winthor\$Modulo"

function Test-CompleteCollection([System.IO.DirectoryInfo] $Directory) {
    $manifestFile = Join-Path $Directory.FullName "manifest.json"
    if (-not (Test-Path -LiteralPath $manifestFile -PathType Leaf)) {
        return $false
    }

    try {
        $manifest = Get-Content -LiteralPath $manifestFile -Raw -Encoding UTF8 | ConvertFrom-Json
        if ($manifest.sourceType -ne "totvs-winthor" -or $manifest.scope -ne $Modulo) {
            return $false
        }

        $hasDocuments = $false
        foreach ($artifact in @($manifest.artifacts)) {
            # Manifestos válidos usam nomes relativos simples. A busca nunca
            # segue caminhos externos ao diretório da execução.
            if ([string]::IsNullOrWhiteSpace($artifact.path) -or
                [System.IO.Path]::IsPathRooted($artifact.path) -or
                $artifact.path.Contains("..")) {
                return $false
            }
            $artifactFile = Join-Path $Directory.FullName $artifact.path
            if (-not (Test-Path -LiteralPath $artifactFile -PathType Leaf)) {
                return $false
            }
            $actualHash = (Get-FileHash -LiteralPath $artifactFile -Algorithm SHA256).Hash.ToLowerInvariant()
            if ($actualHash -ne ([string] $artifact.sha256).ToLowerInvariant()) {
                return $false
            }
            if ($artifact.path -eq "documents.jsonl") {
                $hasDocuments = $true
            }
        }
        return $hasDocuments
    }
    catch {
        return $false
    }
}

$latestExecution = Get-ChildItem -LiteralPath $moduleRoot -Directory -ErrorAction Stop |
    Where-Object { Test-CompleteCollection $_ } |
    Sort-Object Name -Descending |
    Select-Object -First 1

if ($null -eq $latestExecution) {
    throw "Nenhuma coleta encontrada para o módulo $Modulo."
}

$documentsFile = Join-Path $latestExecution.FullName "documents.jsonl"
$terms = $Query.ToLowerInvariant().Split(" ", [System.StringSplitOptions]::RemoveEmptyEntries)

# Busca exploratória do bruto. O ranking definitivo e paginado pertence ao C1;
# este script existe para inspeção local enquanto o catálogo não entra no banco.
Get-Content -LiteralPath $documentsFile -Encoding UTF8 |
    ForEach-Object {
        if ([string]::IsNullOrWhiteSpace($_)) {
            return
        }

        $article = $_ | ConvertFrom-Json
        $title = [string] $article.titulo
        $content = [string] $article.conteudo_texto
        $haystack = ($title + "`n" + $content).ToLowerInvariant()

        $score = 0
        foreach ($term in $terms) {
            if ($title.ToLowerInvariant().Contains($term)) {
                $score += 5
            }
            if ($haystack.Contains($term)) {
                $score += 1
            }
        }

        if ($score -gt 0) {
            [PSCustomObject]@{
                Score = $score
                Id = $article.id
                Titulo = $title
                Secao = $article.secao_caminho
                Url = $article.url
            }
        }
    } |
    Sort-Object Score -Descending |
    Select-Object -First $Limit |
    Format-List
