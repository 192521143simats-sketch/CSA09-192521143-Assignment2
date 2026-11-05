# Java Programming Assignment 2

Name: R SARVESH

Register number: 192521143

Repository: https://github.com/192521143simats-sketch/CSA09-192521143-Assignment2

Java used: Oracle JDK 21.0.12.1 on Windows 11 amd64, compiled with `--release 17`. A JDK 17 or later is required. Java programs have no external dependencies.

## Tasks and recorded results

| Task | Entry points | Result |
| --- | --- | --- |
| Q1 | TicketExperiment | Three text-building methods produced equal tickets. At 50 fields, StringBuilder averaged 622.41 ns/ticket and repeated + 4790.45 ns/ticket. The shared log retained 4000 complete records. |
| Q2 | PlaylistExperiment | Three playlist sizes exposed the cost of array shifting and linked-list indexed traversal. All edit-order, iteration and Up Next checks passed. |
| Q3 | Attendance, AttendanceExperiment | The supplied program reported 3 students before the correction and 2 afterward. Reused references, deliberately distinct hashes, mutation and alternative structures were tested. |
| Q4 | InventoryExperiment | All map reports matched both required orders at three sizes. All tested maps met the predeclared mean lookup criterion. |
| Q5 | LeaderboardDemo, RankingExperiment | The expected ranking matched exactly. Overflow and incomplete tie-breakers were detected. Comparator, Comparable and stable-sort alternatives agreed. |
| Q6 | Triage, TriageExperiment | The supplied treatment order matched exactly. Eight-patient hand ordering, FIFO ties, growth counts, alternative queues and aging checks passed. |
| Q7 | RankingRedesign | All cached results matched the original. Unchanged data reduced 30 sorts to 1, while updates invalidated the cache correctly. |

## Structure

Each `Task01` through `Task07` contains its Java sources, deterministic input files, `Answer.md`, `execution.txt`, `raw.csv`, and genuine output screenshots named `Output_PageNN.png`. Timing tasks also contain `summary.csv` with mean, sample standard deviation, minimum and maximum. Additional operation counts and correctness records are stored beside the sources. Task03 includes the original uncorrected program in `before/`.

`common/Bench.java` provides the same measurement method for all timing tasks. It performs three warmup batches followed by seven recorded trials. Preparation happens outside the timer. A volatile checksum consumes timed results. Heap readings are sampled without forcing garbage collection. They are used-heap observations and do not count total allocations. Each task's answer states its operation unit and limitations.

`common/EvidenceViewer.java` displays the actual saved program stdout in numbered pages. The PNGs are native Windows screenshots of that viewer. It labels the source as saved stdout. The text comes from actual Java executions recorded in `execution.txt`, and the raw trials remain available for independent checking. Screenshots are supporting evidence alongside repeated measurements and operation counts.

The local `docs/` folder holds the DOCX, PDF and their rendering tools. That folder and all DOCX/PDF files are ignored by Git, as requested.

## Compile and execute every task

From the repository root in PowerShell, run

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\Run-All.ps1
```

This compiles every task independently and runs all 10 main entry points, plus the before-correction Attendance program. It recreates raw data and execution transcripts. New measurements will differ from this recorded run. Refresh the answer tables, reports and screenshots when adopting a new set of results.

To compile and run an individual task, use its entry points from the task table.

```powershell
.\tools\Run-Task.ps1 -Task 1 -Programs TicketExperiment
.\tools\Run-Task.ps1 -Task 2 -Programs PlaylistExperiment
.\tools\Run-Task.ps1 -Task 3 -Programs Attendance,AttendanceExperiment
.\tools\Run-Task.ps1 -Task 4 -Programs InventoryExperiment
.\tools\Run-Task.ps1 -Task 5 -Programs LeaderboardDemo,RankingExperiment
.\tools\Run-Task.ps1 -Task 6 -Programs Triage,TriageExperiment
.\tools\Run-Task.ps1 -Task 7 -Programs RankingRedesign
```

For a manual example, compile Q5 from the repository root and run from its task folder so its input paths resolve.

```powershell
New-Item -ItemType Directory -Force build\Task05 | Out-Null
javac --release 17 -d build\Task05 common\Bench.java Task05\*.java
Push-Location Task05
java -Xms256m -Xmx1g -cp ..\build\Task05 LeaderboardDemo
java -Xms256m -Xmx1g -cp ..\build\Task05 RankingExperiment
Pop-Location
```

Task03's intentional broken equals/hashCode classes produce expected compiler warnings. Q5's supplied package-private classes produce auxiliary-class warnings when reused by its experiment. Successful compilation does not imply an absence of these deliberately retained warnings.

## View an output transcript

```powershell
New-Item -ItemType Directory -Force build\viewer | Out-Null
javac --release 17 -d build\viewer common\EvidenceViewer.java
java -cp build\viewer EvidenceViewer Task02\execution.txt 1
```

The last argument chooses the initially selected page. Every screenshot page is saved in the corresponding task folder.

## Verify the submission

Python 3 with Pillow is required only for the evidence-verification helper. Run

```powershell
python tools\Verify-Submission.py
```

This recompiles every source, checks the supplied program outputs, validates seven-trial statistics and cross-implementation checksums, confirms task evidence and screenshot coverage, and checks Git tracking for excluded documents. It does not overwrite the saved benchmark measurements. The recorded result is in `verification.txt`.

## History and measurement scope

The history contains seven actual task-completion commits followed by verification and reproducibility changes. Commit timestamps represent the real work performed. No historical student activity is reconstructed.

Measurements describe this machine and these generated inputs. Fixed method order, JIT compilation, garbage collection and other system activity can influence timing. Mean and sample deviation are reported together, with deterministic operation counts used where available. Detailed comparisons, Big-O analysis, predictions and engineering decisions are in each task's `Answer.md`. The programs and evidence should be reviewed and understood before an assessment discussion.
