$ErrorActionPreference = "Stop"

function Wait-ForHealthyService {
    param([string]$Name, [string]$Url)

    for ($attempt = 1; $attempt -le 30; $attempt++) {
        try {
            $response = Invoke-RestMethod -Uri $Url -TimeoutSec 3
            if ($response.status -eq "UP") {
                Write-Host "$Name is healthy"
                return
            }
        } catch {
            Start-Sleep -Seconds 2
        }
    }
    throw "$Name did not become healthy"
}

Wait-ForHealthyService "Search service" "http://localhost:8080/actuator/health"
Wait-ForHealthyService "Indexer service" "http://localhost:8081/actuator/health"

$run = Invoke-RestMethod -Method Post -Uri "http://localhost:8081/api/index/sync?maxSets=1"
Write-Host "Started indexing run $($run.runId)"

for ($attempt = 1; $attempt -le 90; $attempt++) {
    $status = Invoke-RestMethod -Uri "http://localhost:8081/api/index/runs/$($run.runId)"
    if ($status.status -eq "COMPLETED") {
        Write-Host "Indexed $($status.documentsIndexed) documents from $($status.packetsProcessed) packets"
        break
    }
    if ($status.status -eq "FAILED") {
        throw "Indexing failed: $($status.errorMessage)"
    }
    Start-Sleep -Seconds 2
}

$result = Invoke-RestMethod -Uri "http://localhost:8080/api/search?q=mitochondria&size=3"
if ($result.total -lt 1) {
    throw "Search returned no results"
}

Write-Host "Search returned $($result.total) matches in $($result.tookMs) ms"
$result.results | Select-Object -First 3 id, answerText, setName, score | Format-Table
