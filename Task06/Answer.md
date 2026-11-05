# Q6

I derived the queue policy before using the supplied code. The ordering key is severity ascending, followed by an increasing arrival counter. ArrayDeque and LinkedList used as ordinary FIFO queues preserve arrival order but cannot bring a newly arriving critical patient ahead of an earlier minor patient. A PriorityQueue selects the smallest ordering key at its head. Each accepted arrival receives a unique counter value, so patients with the same severity are served in arrival order.

The completed declaration in the supplied Triage program is as follows.

```java
private final PriorityQueue<Patient> queue = new PriorityQueue<>(
    Comparator.comparingInt((Patient p) -> p.severity)
        .thenComparingLong(p -> p.arrival));
```

admit adds a new Patient with counter++, and next returns queue.poll(). poll removes and returns the head or returns null for an empty queue. The actual supplied-program output exactly matched the expected output.

```text
Meena(S1) Kumar(S1) Lata(S2) Ravi(S3) John(S3) Asha(S5)
```

The eight-patient design test admits Ravi S3, Meena S1, John S3, Asha S5, Kumar S1, Lata S2, Priya S4 and Omar S2 in that order. Before execution, I worked out Meena, Kumar, Lata, Omar, Ravi, John, Priya, Asha. The program produced exactly that order. Within severity 1, Meena precedes Kumar, within severity 2, Lata precedes Omar, and within severity 3, Ravi precedes John. The counter preserves these ties even when a heap rearranges its internal array.

To display a waiting list, copy the queue with new PriorityQueue<>(queue), repeatedly poll the copy and print the results. This leaves the original queue intact, which was checked by confirming its size stayed eight. Printing PriorityQueue directly traverses heap storage, which guarantees only that each parent precedes its children. It does not guarantee global sorted iteration. The main eight-patient layout happened to be sorted, so a separate witness was deliberately tested. Its iteration severities were 1, 4, 2, 5, 4, 3, 2, 5. Seeing severity 4 before severity 2 demonstrates the difference. Polling a copy produced the correct order.

Before timing, I predicted O(log n) worst-case heap work for admit and next, with occasional O(n) backing-array growth on admission. Capacity was reserved in the timed admission test to isolate heap maintenance. The growth experiment used 100, 10,000 and 50,000 pre-created patients with seed 42, three warmups and seven measured trials. Admission times are total batch time divided by n. Serving times divide a complete drain by n, so they average over decreasing heap sizes. Patient construction and initial queue preparation are outside these timers.

| Initial patients | Admit mean ± SD ns/patient | Serve mean ± SD ns/patient | Admission comparator calls | Drain comparator calls | One new critical patient comparator calls |
| --- | --- | --- | --- | --- | --- |
| 100 | 806.57 ± 1023.29 | 1541.86 ± 2887.42 | 164 | 837 | 6 |
| 10000 | 203.50 ± 300.99 | 245.74 ± 37.65 | 18153 | 216496 | 13 |
| 50000 | 74.29 ± 31.50 | 1120.32 ± 636.51 | 90746 | 1314833 | 15 |

Random admission required about 1.6 to 1.8 comparisons per patient and did not exhibit the worst-case logarithmic growth. A deliberately adverse single admission placed one critical patient into a heap containing only minor patients. It required 6, 13 and 15 comparisons, matching the height growth floor(log2(n + 1)). Drain comparisons per patient grew from 8.37 to 21.65 to 26.30. Operation counts support the predicted logarithmic removal and worst-case insertion bounds more clearly than timings, which include JIT compilation, GC and scheduling noise. Faster admission at a larger size does not prove constant worst-case cost.

A severity-5 patient can starve if a higher-severity patient is always available when the doctor requests next. A simulated fair rule promotes a patient's effective severity by one for each completed 30 minutes of waiting, with a minimum effective severity of 1. Ties use the original arrival counter. At a fixed current time of 120 minutes, the tested old severity-5 patient reached effective severity 1 and preceded a newly arrived severity-1 patient. Under a finite existing backlog and continuing service, later arrivals cannot repeatedly jump ahead of that patient after promotion.

Time-dependent priorities must not be changed silently inside a heap comparator because the heap will not reorder itself as the clock advances. The implementation computes immutable effective priorities at a snapshot time and rebuilds the queue when refreshing priorities. Its straightforward repeated-add rebuild costs O(n log n) and O(n) temporary space. Refreshing before every service preserves the stated rule exactly but adds that cost. Refreshing periodically reduces overhead and introduces up to one refresh interval of promotion delay. The aging rule changes strict original-severity priority, so its threshold and policy require an explicit operational decision. It is tested here as an assignment simulation.

The largest test admitted and served a much larger simultaneous backlog than 5,000. Using its mean admission plus removal cost, 5,000 admit/serve pairs would account for approximately 5.97 ms of collection work, excluding record allocation and every hospital-system dependency. This supports the queue algorithm's capacity for the stated daily count. It does not establish end-to-end response latency or the cost of frequent aging rebuilds. PriorityQueue is not thread safe, so a shared deployed queue needs serialized access or synchronization around the queue and counter. The measured comparator and FIFO logic remain applicable.

| Design | Admit | Serve next | Space | Trade-off |
| --- | --- | --- | --- | --- |
| PriorityQueue with severity and arrival | O(log n) heap work, occasional O(n) growth | O(log n) | O(n) array references and patients | General priorities, sorted display requires copy and polling |
| LinkedList kept sorted | O(n) search and O(1) insertion at a positioned iterator | O(1) removeFirst | O(n) linked nodes and patients | Expensive admissions, directly ordered iteration |
| Five FIFO queues | O(1) amortized addLast | At most five checks, O(1) | O(n + 5) | Fixed five levels, promotions require moving entries or rebuilding |

The sorted-list alternative advances a ListIterator until it reaches the first greater ordering key and inserts at that cursor. Removing the first node serves next. The five-queue alternative appends to queues[severity - 1] and scans severity 1 through 5 to remove from the first non-empty queue. Its fixed five checks are constant time. Both alternatives were executed against the same eight-patient dataset and exactly matched the PriorityQueue treatment order. Invalid severity was rejected by the designed Entry type, and an empty queue returned null. The supplied exercise was completed without altering its remaining functional code.
