param([Parameter(Mandatory=$true)][int]$Task, [Parameter(Mandatory=$true)][string[]]$Programs)
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$taskName = 'Task{0:D2}' -f $Task
$folder = Join-Path $root $taskName
$build = Join-Path $root "build/$taskName"
New-Item -ItemType Directory -Force $build | Out-Null
$sources = @((Join-Path $root 'common/Bench.java')) + @((Get-ChildItem $folder -Filter '*.java').FullName)
& javac --release 17 -Xlint:all -d $build $sources 2>&1 | Set-Content (Join-Path $folder 'compilation.txt')
if ($LASTEXITCODE -ne 0) { throw "Compilation failed for $taskName" }
Push-Location $folder
try {
    $transcript = [System.Collections.Generic.List[string]]::new()
    foreach ($program in $Programs) {
        $transcript.Add("java -Xms256m -Xmx1g -cp build/$taskName $program")
        $output = & java -Xms256m -Xmx1g -cp $build $program 2>&1
        if ($LASTEXITCODE -ne 0) { throw "Execution failed for $program`n$output" }
        foreach ($line in $output) { $transcript.Add([string]$line) }
        $transcript.Add('')
    }
    $transcript | Set-Content 'execution.txt' -Encoding utf8
    $transcript
} finally { Pop-Location }
