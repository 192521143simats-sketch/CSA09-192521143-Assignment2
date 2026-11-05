$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
New-Item -ItemType Directory -Force "$root/build/Task03-before" | Out-Null
& javac --release 17 -Xlint:all -d "$root/build/Task03-before" "$root/Task03/before/Attendance.java" 2>&1 |
    Set-Content "$root/Task03/before/compilation.txt"
if ($LASTEXITCODE -ne 0) { throw 'Before-correction compilation failed' }
$before = & java -cp "$root/build/Task03-before" Attendance
if ($LASTEXITCODE -ne 0) { throw 'Before-correction execution failed' }
$before | Set-Content "$root/Task03/before/execution.txt"
& "$PSScriptRoot/Run-Task.ps1" -Task 1 -Programs TicketExperiment
& "$PSScriptRoot/Run-Task.ps1" -Task 2 -Programs PlaylistExperiment
& "$PSScriptRoot/Run-Task.ps1" -Task 3 -Programs Attendance,AttendanceExperiment
& "$PSScriptRoot/Run-Task.ps1" -Task 4 -Programs InventoryExperiment
& "$PSScriptRoot/Run-Task.ps1" -Task 5 -Programs LeaderboardDemo,RankingExperiment
& "$PSScriptRoot/Run-Task.ps1" -Task 6 -Programs Triage,TriageExperiment
& "$PSScriptRoot/Run-Task.ps1" -Task 7 -Programs RankingRedesign
