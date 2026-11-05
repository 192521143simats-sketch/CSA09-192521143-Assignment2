# Q1

Prediction before implementation. String is immutable. Each loop concatenation must produce the new accumulated value, so earlier characters are repeatedly copied. StringBuilder and StringBuffer append to a growable buffer. Their growth should be approximately linear in total output length. StringBuffer synchronizes its methods and may cost more, although the JVM can remove locks on a thread-confined object. Small tickets may show negligible differences or a different ranking because timing overhead and compilation dominate.

The three workloads use identical 18-character fields including the newline. The first is one short ticket with 8 fields, batched 2,000 times to improve timing resolution. The second is 10,000 tickets with 50 fields each. The additional workload is 100 long tickets with 200 fields each. fields.txt contains the actual fields, and workloads.csv records ticket counts, field counts and batches. Setup occurs before timing. Each method has three warmup batches and seven recorded trials. A volatile checksum consumes the generated lengths. These are exploratory measurements in one JVM, with fixed method order, rather than a statistically controlled production benchmark.

| Tickets per workload | Fields | String ns/ticket mean ± SD | StringBuilder ns/ticket mean ± SD | StringBuffer ns/ticket mean ± SD |
| --- | --- | --- | --- | --- |
| 1 | 8 | 552.04 ± 489.43 | 864.53 ± 343.00 | 844.03 ± 314.32 |
| 10000 | 50 | 4790.45 ± 589.39 | 622.41 ± 40.73 | 2114.14 ± 55.80 |
| 100 | 200 | 67299.71 ± 7211.55 | 2401.86 ± 76.06 | 9550.57 ± 904.98 |

The large-ticket prediction is supported. StringBuilder is approximately 7.70 times faster than repeated + at 50 fields and 28.02 times faster at 200 fields. The small workload does not support a universal speed ordering. Its standard deviation is large, so its mean alone cannot establish a reliable advantage. The experiment checks that all methods produce exactly the same ticket.

| Fields n | Final characters | Logical concatenated String results per ticket | Intermediate results excluding final | Characters copied by repeated + | Characters appended by a builder |
| --- | --- | --- | --- | --- | --- |
| 8 | 144 | 8 | 7 | 648 | 144 |
| 50 | 900 | 50 | 49 | 22950 | 900 |
| 200 | 3600 | 200 | 199 | 361800 | 3600 |

These are algorithmic estimates. The expression text + field + newline is one concatenation expression in Java 17-compatible bytecode, and JVM optimization can change actual allocations. Each builder uses one builder object, growing backing arrays, and one final String. The experiment does not claim that these estimates count physical heap objects. Buffer growth and the final toString copy add linear work beyond the appended-character count.

raw.csv records used heap before and after every trial. At 200 fields, the observed heap change for String ranged from -160,945,360 to 37,224,448 bytes per 100 tickets. The builder and buffer changes each ranged from 1,048,576 to 1,572,864 bytes. Negative values indicate garbage collection between samples. Used-heap differences include GC and allocator behaviour and cannot measure total allocated bytes or retained memory reliably. The deterministic copy counts are the stronger allocation-pressure evidence.

For fixed field length L, repeated + copies L(1 + 2 + ... + n) = Ln(n + 1)/2 characters, giving O(n²) time. Its cumulative intermediate character storage allocated is O(n²), while the live accumulated text and new result at a step require O(n) space if obsolete strings can be reclaimed. A builder appends Ln characters and occasionally copies a geometrically expanded backing array, giving amortized O(n) time and O(n) extra space including its final String. With varying field lengths, use total text length C. Builder time is O(C), while repeated + costs the sum of the prefix lengths, at most O(nC).

The alternatives can preserve the exact trailing newline as follows.

```java
String ticket = String.join("\n", fields) + "\n";
StringJoiner joiner = new StringJoiner("\n", "", "\n");
fields.forEach(joiner::add);
String ticket2 = joiner.toString();
String ticket3 = String.format("%s%n", String.join("\n", fields));
```

String.join and StringJoiner require O(C + n) time and O(C + n) storage for this input. String.format adds parsing and formatting overhead and remains linear in the produced text for this fixed format. Platform %n may differ from a literal newline. The first two alternatives were executed and checked against the builder output. Format is most useful when field formatting is needed.

Use a thread-confined StringBuilder for each ticket. For the shared in-memory log, StringBuffer is suitable when each complete record is appended with one append call. Multiple append calls making one record require an external synchronized block to prevent interleaving. Four threads each appended 1,000 complete records, and the resulting 4,000 lines passed the integrity check. This demonstrates synchronization correctness for that test, while persistent storage, I/O and server contention require separate deployment measurement.
