# Q2

Before timing, I predicted amortized constant-time appends for both lists, constant-time indexed access for ArrayList, and linear traversal for LinkedList.get(k). Front edits should expose array shifting. Middle edits by numerical index require traversal in LinkedList and shifting in ArrayList. Both lists should iterate in O(n).

The experiment uses playlists of 100, 10,000 and 100,000 Song records. For each size it times 500 appends, 500 front insertion/removal pairs, 500 middle insertion/removal pairs, 500 get(k) requests generated with seed 42, and one complete enhanced-for traversal. Three warmup batches precede seven recorded trials. Setup and the full order-validation check are outside the timed edit loops. Each edit pair inserts and then removes the same song at the same position, keeping the baseline size constant. The pair costs two collection operations. Iteration times are divided by the number of visited songs. Up Next performs two additions and two front removals per cycle.

At 100,000 songs, ArrayList get(k) averaged 124.40 ns and LinkedList averaged 42,675.83 ns, a difference of approximately 343 times. ArrayList front-edit pairs grew from 451.71 ns at 100 songs to 17,876.46 ns at 100,000 songs. The corresponding LinkedList front pair at 100,000 songs averaged 35.26 ns. Indexed middle edits favoured ArrayList at the largest size, with 11,024.03 ns against 207,474.60 ns. LinkedList is therefore unsuitable as a general cure for indexed editing.

For 500 front edit pairs, ArrayList must shift 100,000,000 reference slots at n = 100,000. Middle pairs shift approximately 50,000,000 slots. LinkedList needs only local link changes at the front, while two middle index searches require roughly 50,000,000 node steps across the same batch. operation_counts.csv records these algorithmic counts. They explain scaling more reliably than the noisy smallest-size timings.

ArrayList is a reasonable choice for append-heavy playlists with frequent jumps to the k-th song and full traversal. For a concrete boundary, I use a 10 microsecond mean budget per edit pair for the collection work. The largest tested ArrayList front and middle edits exceeded this budget. I would stop recommending it for frequent front edits at that measured scale, while still retaining it for indexed reads. A different device, operation mix or latency budget can move this boundary. These trials do not measure user-interface, network or music playback latency.

For Up Next, ArrayDeque gives amortized O(1) additions at either end and O(1) removal from the front, with compact array storage and no allocation of a linked node per addition. The measured ArrayDeque cycle means were 238.29, 388.74 and 851.91 ns. LinkedList means were 1,504.26, 1,040.14 and 152.80 ns. ArrayDeque won at the two smaller sizes, while LinkedList won at the largest. The deque constructed from a full collection grows during its first cycle, and that O(n) capacity expansion is included in each measured batch. Reserving spare capacity before steady-state operation would change this comparison. I recommend ArrayDeque for this endpoint-only queue, with capacity chosen for expected occupancy and real latency measurements before deployment. It has no indexed get method.

| Operation | ArrayList | LinkedList | ArrayDeque |
| --- | --- | --- | --- |
| Add at end | O(1) amortized, O(n) resize | O(1) | O(1) amortized, O(n) resize |
| Insert or remove at front | O(n) | O(1) | O(1) amortized insert, O(1) removal |
| Insert in middle by index | O(n) | O(n) search, O(1) link update | No indexed API, O(n) manual traversal/rearrangement |
| Get k-th | O(1) | O(min(k, n-k)) | No indexed API, O(n) iterator traversal |
| Iterate all | O(n) | O(n) | O(n) |

All three use O(n) collection storage. ArrayList and ArrayDeque hold reference slots with spare capacity. A typical 64-bit HotSpot JVM with compressed references uses about 4 bytes per slot. A LinkedList node commonly occupies about 24 bytes including its header, element reference and two link references, plus list bookkeeping. These are illustrative JVM-dependent layout estimates, not measured object sizes. Song objects themselves are additional and common to every design. Iterator-based insertion at an already positioned LinkedList cursor is O(1), which differs from the indexed operations tested here.

A manual array solution keeps Song[] data and an integer size. Append stores data[size++] and doubles capacity with Arrays.copyOf when full. Insertion at position k uses System.arraycopy(data, k, data, k + 1, size - k), then stores the song. Removal shifts the suffix left and clears the unused last slot. Get returns data[k] after a bounds check. This has the same asymptotic complexities as ArrayList, including O(n) shifts, amortized O(1) append and O(n) capacity space. A fixed-capacity array must reject appends when full. ArrayList supplies these operations and bounds checks without requiring custom shifting code.

All order checks, traversal checksums and queue-size checks passed. An empty playlist rejected get(0) with IndexOutOfBoundsException. The following tables report mean ± sample standard deviation across seven trials in nanoseconds per operation, with edit pairs and Up Next cycles counted as described above.

| Size | Structure | Operation | Mean ns | SD ns |
| --- | --- | --- | --- | --- |
| 100 | ArrayList | append | 90.29 | 54.74 |
| 100 | ArrayList | front-edit-pair | 451.71 | 622.80 |
| 100 | ArrayList | middle-edit-pair | 398.31 | 341.65 |
| 100 | ArrayList | get-k | 67.29 | 3.85 |
| 100 | ArrayList | iterate | 98.71 | 10.44 |
| 100 | LinkedList | append | 104.46 | 100.00 |
| 100 | LinkedList | front-edit-pair | 272.37 | 152.22 |
| 100 | LinkedList | middle-edit-pair | 449.97 | 227.63 |
| 100 | LinkedList | get-k | 210.23 | 149.68 |
| 100 | LinkedList | iterate | 126.86 | 56.68 |
| 100 | ArrayDeque | up-next-cycle | 238.29 | 42.09 |
| 100 | LinkedList | up-next-cycle | 1504.26 | 2810.42 |
| 10000 | ArrayList | append | 76.54 | 20.20 |
| 10000 | ArrayList | front-edit-pair | 1159.34 | 449.71 |
| 10000 | ArrayList | middle-edit-pair | 649.89 | 273.62 |
| 10000 | ArrayList | get-k | 78.86 | 31.65 |
| 10000 | ArrayList | iterate | 48.37 | 32.74 |
| 10000 | LinkedList | append | 44.91 | 3.11 |
| 10000 | LinkedList | front-edit-pair | 97.97 | 5.50 |
| 10000 | LinkedList | middle-edit-pair | 17450.94 | 2855.21 |
| 10000 | LinkedList | get-k | 3736.77 | 421.95 |
| 10000 | LinkedList | iterate | 20.88 | 0.64 |
| 10000 | ArrayDeque | up-next-cycle | 388.74 | 71.24 |
| 10000 | LinkedList | up-next-cycle | 1040.14 | 1900.60 |
| 100000 | ArrayList | append | 908.09 | 210.37 |
| 100000 | ArrayList | front-edit-pair | 17876.46 | 2571.31 |
| 100000 | ArrayList | middle-edit-pair | 11024.03 | 3396.98 |
| 100000 | ArrayList | get-k | 124.40 | 43.97 |
| 100000 | ArrayList | iterate | 33.62 | 33.28 |
| 100000 | LinkedList | append | 81.77 | 59.67 |
| 100000 | LinkedList | front-edit-pair | 35.26 | 6.44 |
| 100000 | LinkedList | middle-edit-pair | 207474.60 | 87160.25 |
| 100000 | LinkedList | get-k | 42675.83 | 6742.64 |
| 100000 | LinkedList | iterate | 15.06 | 4.38 |
| 100000 | ArrayDeque | up-next-cycle | 851.91 | 535.09 |
| 100000 | LinkedList | up-next-cycle | 152.80 | 5.19 |
