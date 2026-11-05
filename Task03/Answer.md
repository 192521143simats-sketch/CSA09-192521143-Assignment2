# Q3

Before executing the supplied program without hashCode, I expected three entries because separately allocated Student objects generally receive different identity hashes. The actual output in before/execution.txt was Unique students: 3. After completing hashCode with return regNo.hashCode(), the actual output was Unique students: 2, exactly matching the expected output.

| Test designed before execution | Prediction and reason | Observed result in all 7 trials |
| --- | --- | --- |
| Scan the same Arun object twice and a separate Divya object once | Size 2, because the same reference has the same identity hash and is already present | Size 2 |
| Scan two separately allocated Arun objects with the same registration and distinct identity hashes, plus Divya | Size 3, because equal registrations have inconsistent hashes | Size 3 |
| Insert a correctly hashed mutable registration A, change it to B, then test membership and removal | Size stays 1, but contains and remove fail because the new hash selects a different location | Size 1, contains false, remove false |
| Reinsert the mutated object | Size 2, because its old entry is stranded under its previous stored hash | Size 2 |
| Use immutable registration and a matching hashCode | Size 2 for the three supplied scans | Size 2 |

The second test deliberately chooses a duplicate whose identity hash differs from the original. Identity hashes are not guaranteed unique. Without that control, a rare accidental equal hash could conceal the defect. The first test shows why a broken implementation can appear correct when the same objects are reused.

HashSet stores keys through a HashMap. It spreads hashCode into a hash and uses it to choose a table bucket. Within that bucket, matching stored hashes are candidates for reference identity or equals checks. Two equal objects with different hashes may be stored separately because the search does not compare every object in the entire set. The contract requires equal objects to have equal hashes. Unequal objects are allowed to have equal hashes, and equals then distinguishes collisions.

A key's stored entry does not move when the key changes. In this test A and B produce different spread hashes and buckets in the initial table. A query after mutation computes B's new hash, while the original entry still carries A's insertion hash. Consequently size still counts the old entry, membership and removal fail, and reinsertion adds a second entry. This is a designed outcome for these keys, while arbitrary mutations can produce other symptoms if hashes collide. Remove a key before changing its equality fields, then reinsert it, or use an immutable key.

The corrected supplied class keeps regNo final, compares regNo in equals, and returns regNo.hashCode() in hashCode. The name is deliberately excluded from both because registration determines identity. The supplied class assumes a non-null registration. Production input validation should reject null or blank registrations before construction. The experiments verified reflexivity, symmetry, transitivity, rejection of null and another type, equal hashes for equal registrations, and successful lookup using a new equal object. A different name with the same registration remained equal. Seven repetitions reproduced all predicted sizes in raw.csv.

The alternative solutions were also executed.

```java
Set<Student> unique = new TreeSet<>(Comparator.comparing(s -> s.regNo));
for (Student scan : scans) unique.add(scan);
Map<String, Student> byRegistration = new HashMap<>();
for (Student scan : scans) byRegistration.putIfAbsent(scan.regNo, scan);
```

This pseudocode uses registration access appropriate to its Student type. AttendanceExperiment uses an accessible experimental registration field, while the supplied corrected Student keeps its fields private. A getter would allow this exact pattern with that class. Both alternatives produced two unique registrations. TreeSet uses compare(a,b) == 0 to define duplicate keys, so a comparator on regNo agrees with the intended identity. A comparator using only name would incorrectly merge different students who share a name. HashMap keyed by String avoids dependence on Student.hashCode, and putIfAbsent retains the first scan instead of replacing its value.

| Structure for n unique students | Time per scan | Space | Relevant trade-off |
| --- | --- | --- | --- |
| HashSet with corrected Student | O(1) average, O(n) worst-case contractual bound | O(n) table and entries | Requires correct immutable equality fields |
| TreeSet ordered by regNo | O(log n) comparisons | O(n) balanced-tree nodes | Naturally keeps registrations ordered |
| HashMap keyed by regNo | O(1) average, O(n) worst-case contractual bound | O(n) table, entries and key references | Direct retrieval of the student by registration |

String hashing and comparison additionally depend on registration length. The assignment's short, fixed-length registration numbers make that a constant factor. Some Java HashMap collision bins can become trees, but that implementation detail does not justify treating every hash-table operation as guaranteed constant time. The correction is established by set sizes and contract checks, so timing measurements are unnecessary for this correctness investigation.
