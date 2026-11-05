# Java Programming Assignment 2

Name: R SARVESH

Register number: 192521143

Java version: Oracle JDK 21.0.12.1. Every source also compiles with --release 17.

GitHub: https://github.com/192521143simats-sketch/CSA09-192521143-Assignment2

## Files

Each Task01 through Task07 folder contains exactly one independent Java source file and one complete output screenshot. Task04 additionally contains input.csv, which its program reads. All other input values and test datasets are embedded in their Java source.

## Run each question

Use JDK 17 or later. From the repository root, enter the task folder and run its source file directly.

| Question | Commands | Result |
| --- | --- | --- |
| Q1 | cd Task01, then java Task01.java | Three ticket-building methods agree. Three workloads and the shared-log check complete successfully. |
| Q2 | cd Task02, then java Task02.java | Playlist operations at three sizes and Up Next checks complete successfully. |
| Q3 | cd Task03, then java Task03.java | Before correction gives 3 students. After correction gives 2. Equality, mutation and alternative-collection checks pass. |
| Q4 | cd Task04, then java Task04.java | Reads input.csv. Both report orders match for all maps at three sizes. |
| Q5 | cd Task05, then java Task05.java | The expected ranking matches. Comparator faults are detected, and ranking alternatives agree. |
| Q6 | cd Task06, then java Task06.java | The expected triage order, FIFO ties, growth, aging and alternative-queue checks pass. |
| Q7 | cd Task07, then java Task07.java | Cached rankings agree with the original, and additions invalidate the cache. |

Run each pair of commands from the repository root. To compile explicitly, enter the task folder and use javac -d ../build/Task01 Task01.java, then java -cp ../build/Task01 Task01. Substitute the corresponding task number for the other questions.

Each source contains its required helper classes, completed supplied-code examples and experiments. No shared source file or third-party Java dependency is required. Q3 includes both the uncorrected and corrected versions in its single source file using distinct internal class names.

## Measurements and screenshots

Output.png in each task folder is one genuine screenshot showing that task's complete executed output. The timing experiments use three warmup batches and seven measured trials. Q3 repeats its correctness observations seven times.

Every Output.png is a lossless 1920 x 1080 PNG captured at the desktop's native resolution. Read two-column outputs down the left column, then down the right column. The captures preserve the original output text without JPEG compression or image enlargement.

Programs save detailed raw trials, summaries and supplementary output locally under docs/evidence/TaskNN, outside the task folders. This directory is ignored by Git. Use -Dassignment.evidence=PATH before the Java source filename to choose a different local evidence directory.

The Word/PDF reports, raw evidence, previous screenshots, report-authoring scripts and compiler logs are kept locally. New timings can differ due to JVM compilation, garbage collection and system activity.
