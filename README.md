# Java Programming Assignment 2

Name: R SARVESH

Register number: 192521143

Java version used: Oracle JDK 21.0.12.1 on Windows 11, compiled for Java 17.

Repository: https://github.com/192521143simats-sketch/CSA09-192521143-Assignment2

## Submission structure

Every task uses the same structure.

```text
Task01/ ... Task07/
    src/            Java source code
    inputs/         Input data and experiment settings
    results/        Actual stdout, raw trials, summaries and operation counts
    screenshots/    Genuine output screenshots

common/Bench.java   Shared measurement code
tools/              Compile-and-run scripts
```

Task03 additionally keeps the uncorrected supplied program in `src/before/` and its actual output in `results/before_execution.txt`.

## Compile and run

Install a JDK version 17 or later. No Java libraries or build-tool installation are required. Open PowerShell in the repository root.

Run all tasks, including every supplied Complete the Code program and the uncorrected Q3 program.

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\Run-All.ps1
```

Run an individual task with the corresponding command.

| Task | Compile and run command | Result of recorded execution |
| --- | --- | --- |
| Q1 | `.\tools\Run-Task.ps1 -Task 1 -Programs TicketExperiment` | All three methods produced equal tickets across three workloads. The four-thread log retained all 4,000 records. |
| Q2 | `.\tools\Run-Task.ps1 -Task 2 -Programs PlaylistExperiment` | Three playlist sizes tested append, front/middle edits, indexed access, iteration and Up Next. Order and checksum checks passed. |
| Q3 | `.\tools\Run-Task.ps1 -Task 3 -Programs Attendance,AttendanceExperiment` | The supplied code produced 3 students before correction and 2 afterward. All seven repetitions of the five designed observations matched their predictions. |
| Q4 | `.\tools\Run-Task.ps1 -Task 4 -Programs InventoryExperiment` | All nine map/size combinations produced correct code-order and addition-order reports. Each map met the declared mean lookup target. |
| Q5 | `.\tools\Run-Task.ps1 -Task 5 -Programs LeaderboardDemo,RankingExperiment` | The supplied expected ranking matched exactly. Overflow and omitted tie-breaker faults were detected. All three ranking alternatives agreed. |
| Q6 | `.\tools\Run-Task.ps1 -Task 6 -Programs Triage,TriageExperiment` | The supplied treatment order and eight-patient hand order matched. FIFO ties, growth, aging and both alternative queues passed their checks. |
| Q7 | `.\tools\Run-Task.ps1 -Task 7 -Programs RankingRedesign` | Cached rankings matched the original for every request. Unchanged data reduced 30 sorts to 1, and additions invalidated the cache correctly. |

For a manual compile-and-run example, run Q5 as follows.

```powershell
New-Item -ItemType Directory -Force build\Task05 | Out-Null
javac --release 17 -d build\Task05 common\Bench.java Task05\src\*.java
Push-Location Task05
java -Xms256m -Xmx1g -cp ..\build\Task05 LeaderboardDemo
java -Xms256m -Xmx1g -cp ..\build\Task05 RankingExperiment
Pop-Location
```

All other tasks use the same pattern with their task number and entry points from the table. Run from the task folder so that `inputs/` and `results/` resolve correctly. Q3's before-correction example has a separate compilation directory because it intentionally uses the same class names as the corrected program. Run-All.ps1 handles that separation.

## Experimental evidence

The timing experiments use three warmup batches followed by seven measured trials. `results/raw.csv` retains each trial, elapsed nanoseconds, operation count, checksum and heap readings. `results/summary.csv` records mean, sample standard deviation, minimum and maximum. Q3 instead records the predicted and observed outcomes of its correctness experiments.

Operation counts and additional order/correctness checks are retained where required. The submission contains 819 measured timing trials and 10 output screenshots across Q1-Q7. Screenshots show actual saved Java output displayed in a labelled output viewer. Raw data and text output provide the complete execution evidence.

Rerunning the experiments regenerates result files. Timing depends on the JVM, garbage collection and system activity, so a new run can produce different values. Heap differences do not measure total allocated bytes. Use the repeated trials and operation counts when interpreting results.

The Git history retains a separate completion commit for each task. The DOCX/PDF reports, report-authoring tools, compiler logs and verification helpers are kept locally and excluded from this submission repository. Compiled classes and temporary build files are also ignored.
