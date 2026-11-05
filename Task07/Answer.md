# Q7

A. I selected the Q5 leaderboard. Its ranking() method always creates a new copy and sorts it, even when no team has changed since the last request. Q5 measured a mean of 7.024 ms for a 10,000-team comparator-based ranking. Calling that method repeatedly repeats the same comparison work. The limitation comes from recomputing order from the unsorted items list on every request. It is a repeated-work problem, and simply choosing another List implementation would not remove it. No allocation profiler was used, so the timing evidence does not establish an exact percentage of time spent in sorting versus copying.

B. Before validation, I set two measurable objectives for 30 unchanged requests with at least 1,000 teams. The redesign must reduce full sorts by at least 90% and reduce the measured mean time per request by at least a factor of two. It must preserve the same ranked values, alphabetical tie-breakers and independent mutable result lists. An addition must be visible in the next ranking, and a previously returned result must retain its earlier contents.

C. The redesign retains the original items list and comparator. It adds a private sorted cache and a dirty flag. add marks the cache dirty. The next ranking sorts a fresh copy and stores it as the new cache. Later rankings return defensive ArrayList copies of the cached order until another addition occurs. This exploits the unchanged ordering of immutable team data between updates. It avoids repeated comparisons and sort scratch allocations while retaining the caller's ability to modify its own returned list.

```text
add(team)
    append team to items
    dirty = true

ranking()
    if dirty
        refreshed = copy(items)
        sort refreshed using the same comparator
        sorted = refreshed
        dirty = false
    return copy(sorted)
```

The compared Original class uses the same copy-and-sort algorithm as Q5. The Cached class uses the same comparator and ArrayList storage. Teams are immutable records. All supported changes go through add, ensuring invalidation. If removal or score updates are added later, those operations must invalidate the cache as well. Generic use with externally mutable ranking fields would need explicit invalidation or immutable replacements. The component is single-threaded. Concurrent updates and reads would need a common lock or a separately designed publication mechanism.

The cached design adds O(n) references for the sorted snapshot. It still allocates an O(n) defensive result copy on every query. It also makes an additional cache copy when refreshing after an update. This preserves the original mutable-copy contract. Returning an unmodifiable shared snapshot could avoid that per-query copy but would change the API's behaviour, so that option was not used.

D. Before timing, I predicted that 30 unchanged requests would perform 30 original sorts versus one cached sort, a 96.67% reduction. Updating before requests 0, 10 and 20 should produce three cached sorts, a 90% reduction. Updating before every request should leave both designs with 30 sorts and no comparison-work saving. I predicted at least a twofold lower mean request time for unchanged data at n >= 1,000. With an update on every request, the cache could be slower because it must refresh and defensively copy its result.

E. The same initial teams, updates, request counts and comparator were used for both designs. conditions.csv specifies sizes 100, 1,000 and 10,000, 30 requests, and update intervals 0, 10 and 1. Interval 0 means no updates. Seed 42 generates identical initial fields in both designs. Each measured trial starts with a new board and a cold cache. Three warmup batches precede seven measured trials. Input preparation is outside timing. The timed workload includes additions when requested, ranking and traversal of all returned entries to consume their points. Validation outside the timer compares every returned list with the original, clears returned cached-design lists, and checks that later rankings remain correct.

At 10,000 teams, unchanged queries reduced comparator calls from 3,456,420 to 115,214. Updating every tenth request reduced them from 3,458,020 to 345,802. Updating every request gave 3,459,182 comparisons in both designs. These deterministic counts directly establish when sorting work was avoided. The result lists copied 300,000, 300,060 and 300,465 references respectively in both designs. Those counts exclude cache-refresh and sorting-workspace copies, so they do not claim a reduction in all memory traffic.

| Workload at 10000 initial teams | Original sorts | Cached sorts | Original comparisons | Cached comparisons |
| --- | --- | --- | --- | --- |
| No updates | 30 | 1 | 3456420 | 115214 |
| Update every 10 requests | 30 | 3 | 3458020 | 345802 |
| Update every request | 30 | 30 | 3459182 | 3459182 |

The following table gives mean ± sample standard deviation in milliseconds per request. Each value includes the cold-cache first request within its 30-request batch. The speed ratio is original mean divided by cached mean, so values above one favour the cache in this run.

| Teams | Update interval | Original ms mean ± SD | Cached ms mean ± SD | Ratio |
| --- | --- | --- | --- | --- |
| 100 | 0 | 0.0616 ± 0.0722 | 0.0334 ± 0.0539 | 1.84 |
| 100 | 10 | 0.0227 ± 0.0148 | 0.0037 ± 0.0007 | 6.10 |
| 100 | 1 | 0.0198 ± 0.0020 | 0.0229 ± 0.0041 | 0.86 |
| 1000 | 0 | 0.3965 ± 0.0835 | 0.0648 ± 0.0460 | 6.12 |
| 1000 | 10 | 0.2673 ± 0.0276 | 0.0276 ± 0.0008 | 9.69 |
| 1000 | 1 | 0.2661 ± 0.0331 | 0.2477 ± 0.0203 | 1.07 |
| 10000 | 0 | 15.2683 ± 13.7845 | 0.3549 ± 0.2824 | 43.02 |
| 10000 | 10 | 13.7985 ± 17.6827 | 0.8179 ± 0.2037 | 16.87 |
| 10000 | 1 | 17.6469 ± 17.0686 | 8.2226 ± 3.7474 | 2.15 |

F. The unchanged-query objective was achieved in these measurements. Sorts fell by 96.67%, and mean request time fell by factors of 6.12 at 1,000 teams and 43.02 at 10,000 teams. With one update per ten requests, sorts fell by exactly 90%. Every request produced the same ordered values as the original. The empty-cache test, addition invalidation, independent mutable result and historical-snapshot tests all passed.

Timing variance is large, particularly in the 10,000-team original measurements. The experiment uses a single JVM with fixed method order, so JIT, garbage collection and background scheduling can affect the apparent speed ratio. With an update before every request, the cache was slower at 100 teams but faster at the larger sizes in this run, despite identical comparison counts. Those larger-size timing differences do not establish an algorithmic cache benefit because no sorts were avoided. The deterministic counts provide the clearest validation of the redesign's intended mechanism.

For R requests, U distinct dirty versions and a roughly fixed size n, the original costs O(R n log n). The cached design costs O(U n log n + R n) because each query still returns and consumes an independent list. With no updates, U = 1. With an update before every request, U = R, so the asymptotic cost is unchanged and refresh-copy overhead can make caching worse. Persistent space grows from O(n) to O(n) with an additional O(n)-reference cache, while peak temporary space remains O(n). Historical snapshots held by callers can add further memory to either design.

I would use the cache for read-heavy boards with multiple requests between changes. The original remains preferable for tiny boards, one-off rankings, frequent updates followed by one read, or when minimizing retained reference arrays matters more than repeated sorting. The final choice follows the measured update/read pattern and the verified invalidation rule.
