# Q4

Before implementation, I predicted average O(1) put and get for HashMap and LinkedHashMap, and O(log n) for TreeMap. TreeMap naturally supplies code order. Insertion-ordered LinkedHashMap naturally supplies addition order. HashMap supplies neither order. To recover an order that a map does not retain, the design stores an immutable addition sequence in each Medicine record and sorts a copy by code or sequence as required. Updating stock must preserve the existing sequence. The benchmark measures the initial distinct insertions.

| Map | Put | Get | Iterate | Native order | Extra work for the other report |
| --- | --- | --- | --- | --- | --- |
| HashMap | O(1) average | O(1) average | O(n + capacity) | Unspecified | Sort O(n log n) for either order |
| TreeMap | O(log n) | O(log n) | O(n) | Code order | Sort by sequence O(n log n) |
| LinkedHashMap | O(1) average | O(1) average | O(n) | Insertion order | Sort by code O(n log n) |

Hash-map operations have an O(n) worst-case contractual bound. Tree comparisons also depend on key length. The fixed-width medicine codes keep that cost bounded in this experiment. All structures use O(n) storage. LinkedHashMap adds two order links per entry. TreeMap adds parent, child and balancing information. A hash table also reserves empty buckets. A report copy uses O(n) references, and its rendered text needs O(C) space for C output characters.

medicines.csv contains 100,000 distinct shuffled codes with stock values. The experiments use prefixes of 100, 10,000 and 100,000 records. Each map is built outside the lookup and report timers. Exactly 100,000 successful lookups use equal but separately allocated query strings selected with seed 42. Three warmups precede seven measured trials. Warmups can cache String hashes. This is an in-memory hot-data test. Each report timer includes ordering where needed and formatting the complete CSV text, while file and printer I/O are excluded. Put has its own separately timed measurement.

Before timing, I defined an acceptable mean collection lookup as less than 10 microseconds. At 100,000 medicines, HashMap averaged 98.09 ns, LinkedHashMap 174.09 ns and TreeMap 1,125.61 ns per lookup. All met the criterion. Their respective 100,000-lookup batch times were 9.81, 17.41 and 112.56 ms. A batch mean cannot establish a worst-case or 99th-percentile latency guarantee.

I would deploy an insertion-ordered LinkedHashMap for this single-threaded inventory component. It meets the lookup criterion, retains addition order directly, and sorts only for the evening code report. At the largest size, its code report averaged 44.90 ms and its insertion report 10.50 ms. The insertion-report standard deviation is large, so these values should be treated as local observations. TreeMap's code report averaged 20.12 ms, but its insertion report needed 39.75 ms plus sequence metadata. Most remaining cost comes from sorting, traversing the records, formatting strings, memory allocation and GC. Real deployment also adds synchronization or snapshots, persistence, request processing and I/O. Those costs were not measured here.

Every rendered report was compared with independently ordered expected rows before timing. All nine map/size combinations passed both order checks in order_checks.csv. Missing keys returned null. Complete 100-record reports are saved as code_order_report.csv and insertion_order_report.csv. Using a default LinkedHashMap is important because access-order mode changes iteration order on get and would violate the addition-order requirement. Removing and adding an entry again also defines a new insertion event.

Hashtable is a legacy synchronized map. Its per-method locking can add contention and does not make a multi-step inventory update atomic. A modern shared inventory can use ConcurrentHashMap for suitable independent operations, or explicit synchronization and consistent snapshots when reports must agree with concurrent changes. That concurrency design needs separate testing.

A sorted-array alternative was executed using Arrays.binarySearch(sorted, query, Comparator.comparing(Medicine::code)). All existing-code positions were checked, and the measured lookup means were 78.21, 176.70 and 457.16 ns at the three sizes. For a new medicine, binarySearch returns a negative insertion result, so position = -result - 1. Allocate or grow the array if necessary, shift its suffix right with System.arraycopy, then insert the medicine. An existing code updates its record while retaining the original sequence. A code-order report can iterate the array directly.

| Sorted array operation | Complexity and storage | Comparison |
| --- | --- | --- |
| Lookup | O(log n), O(1) search space | Same asymptotic lookup as TreeMap, slower growth than average hash lookup |
| Insert | O(log n) search plus O(n) shift | More expensive than average O(1) HashMap or O(log n) TreeMap insertion |
| Build from unsorted data | O(n log n) sorting | HashMap builds in O(n) average, TreeMap in O(n log n) |
| Code report | O(n + C) time for n rows and C characters | Like TreeMap, avoids report-time sorting |
| Addition-order report | O(n log n + C) unless a second order index is maintained | LinkedHashMap provides this order directly |
| Collection storage | O(n) reference slots plus medicines | Less node overhead, with capacity and shifting trade-offs |

The following table reports mean ± sample standard deviation in milliseconds. The lookup column is the total for 100,000 lookups, and each report column is one complete report. Raw per-trial nanosecond evidence is retained in raw.csv.

| Size | Structure | 100000 lookups ms | Code report ms | Addition report ms |
| --- | --- | --- | --- | --- |
| 100 | HashMap | 1.688 ± 0.600 | 0.224 ± 0.050 | 0.200 ± 0.190 |
| 100 | TreeMap | 8.990 ± 3.154 | 0.046 ± 0.011 | 0.108 ± 0.013 |
| 100 | LinkedHashMap | 2.463 ± 2.062 | 0.131 ± 0.044 | 0.048 ± 0.005 |
| 10000 | HashMap | 5.269 ± 3.158 | 15.298 ± 9.335 | 3.934 ± 1.557 |
| 10000 | TreeMap | 23.858 ± 2.154 | 0.642 ± 0.300 | 2.935 ± 0.665 |
| 10000 | LinkedHashMap | 1.990 ± 0.589 | 3.143 ± 0.450 | 0.612 ± 0.407 |
| 100000 | HashMap | 9.809 ± 2.154 | 81.712 ± 43.582 | 39.224 ± 4.247 |
| 100000 | TreeMap | 112.561 ± 53.780 | 20.121 ± 3.848 | 39.751 ± 4.842 |
| 100000 | LinkedHashMap | 17.409 ± 15.341 | 44.897 ± 5.147 | 10.505 ± 15.055 |
