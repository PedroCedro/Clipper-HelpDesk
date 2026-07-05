param(
    [Parameter(Mandatory = $true)]
    [string] $Query,

    [int] $Limit = 10
)

$ErrorActionPreference = "Stop"

$articlesFile = Join-Path $PSScriptRoot "artigos.jsonl"

if (-not (Test-Path -LiteralPath $articlesFile)) {
    throw "Arquivo nao encontrado: $articlesFile. Rode run-crawler.ps1 primeiro."
}

$terms = $Query.ToLowerInvariant().Split(" ", [System.StringSplitOptions]::RemoveEmptyEntries)

Get-Content -LiteralPath $articlesFile -Encoding UTF8 |
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
