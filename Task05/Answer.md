# Q5

The starting principle requires a consistent order. I derive a lexicographic key with three components, descending points, descending goal difference and ascending team name. A later component is used only when all earlier components tie. I choose a separate Comparator because team ranking policy may change and the generic board must also support players, colleges and quiz teams. Comparable is suitable when one permanent natural order belongs to the domain type, but it embeds that policy in the class.

The completed comparison and generic ranking method are as follows.

```java
Comparator<Team> byRank = Comparator
    .comparingInt((Team t) -> t.points).reversed()
    .thenComparing(Comparator
        .comparingInt((Team t) -> t.goalDiff).reversed())
    .thenComparing(t -> t.name);

List<T> ranking() {
    List<T> sorted = new ArrayList<>(items);
    sorted.sort(order);
    return sorted;
}
```

Leaderboard<T> stores List<T> and Comparator<? super T>. No Comparable bound is necessary when the constructor supplies the comparison strategy. The lower-bounded comparator accepts a comparator for T or a supertype of T. A natural-order helper instead needs <T extends Comparable<? super T>> so that sorting is type safe. Both designs were compiled and executed. This distinction avoids unnecessarily restricting the reusable component.

The experiment also executes an addAll helper using a producer wildcard.

```java
static <T> void addAll(Leaderboard<T> board, List<? extends T> input) {
    for (T item : input) board.add(item);
}
```

This accepts List<NaturalTeam> for a Leaderboard<Team>. Values may be read as T, while arbitrary T objects cannot safely be added to the producer list. This helper is outside the supplied class so the original exercise is changed only where its TODOs require working code, with comments removed as requested.

The actual supplied-program output exactly matched the expected output.

```text
[Eagles(9,6), Bisons(9,4), Panthers(9,4), Cobras(6,1)]
```

Bisons and Panthers both have 9 points and goal difference 4. The name tie-breaker puts Bisons first because B precedes P. The larger tie dataset includes a tie at the points level, several goal-difference ties, alphabetically ordered teams, and two separate entries with identical ranking fields. Its verified order was Eagles, Falcons, Bisons, Bisons, Panthers, Tigers, Aardvarks, Cobras. Identical ranking fields compare as zero and both entries remain in the List. Team's supplied Java equals still uses object identity, so comparator equality is ranking equivalence and should not be used as a general team-identity rule in a TreeSet without an explicit identity decision.

The second dataset uses points Integer.MAX_VALUE, 0 and Integer.MIN_VALUE. A faulty comparator using b.points - a.points overflows. In particular, comparing Maximum against Minimum subtracts 2,147,483,647 from -2,147,483,648 and wraps to 1, incorrectly placing Maximum below Minimum. Exhaustive pair and triple checks detected a comparison-law failure. Another faulty comparator omitted the name tie-breaker and compared Panthers and Bisons as zero. Both deliberately faulty comparators were tested. The correct comparator passed all pair/triple checks on both datasets in seven repetitions, and the extreme scores ranked Maximum, Zero, Minimum.

The correctness argument follows from lexicographic ordering. Integer.compare-based ordering and String.compareTo each have consistent signs and transitivity. Reversing a numeric ordering preserves those properties. If A precedes B and B precedes C, consider the earliest component where either adjacent pair differs. All earlier components agree, and that component orders A before C. Later components cannot reverse that decision. If all three components agree, the comparison is zero, and comparisons against any third team have matching signs. No subtraction of arbitrary scores is used, avoiding integer overflow. Reversing each numeric comparator separately preserves alphabetical ascending names. Reversing the entire completed chain would reverse the name order as well.

Comparable can express the same policy with compareTo returning the chain's compare(this, other). The executed NaturalTeam adapter demonstrates this without modifying the supplied Team outside its exercise. The bounded helper copies the list and calls Collections.sort. The comparator-based supplied implementation copies and calls List.sort. Both perform one O(n log n) comparison sort in the general case plus an O(n) copy. Java's object-list sorting can exploit already ordered runs and uses up to O(n) temporary storage. Each returned ranking also needs O(n) references. String comparisons can add a factor depending on team-name length.

The stable-sort alternative sorts a copy by name ascending, then goal difference descending, then points descending. Java's stable object sorting preserves earlier tie order, so the final pass makes points the primary key and retains the other priorities. This performs three O(n log n) sorts, still O(n log n) asymptotically, with a larger constant. Sequential scratch storage is reused, giving O(n) peak auxiliary space rather than O(3n). All three alternatives produced identical results for the tie dataset and the three measured sizes. Clearing a returned list did not change the board, and an empty board returned an empty ranking.

Three warmups and seven measured trials used deterministic data generated with seed 42. Each timed ranking includes its copy and sorting. At 10,000 items, the chain averaged 7.024 ms, Comparable 11.698 ms and three stable sorts 7.105 ms. These noisy local measurements do not establish that Comparable is inherently slower or that three sorts always cost three times one sort. Comparator chains remain easiest to reuse because each new type supplies its own Comparator to the same Leaderboard<T>.

| Items | Comparator mean ± SD ms | Comparable mean ± SD ms | Three stable sorts mean ± SD ms |
| --- | --- | --- | --- |
| 100 | 0.086 ± 0.005 | 0.103 ± 0.017 | 0.278 ± 0.153 |
| 1000 | 0.602 ± 0.068 | 2.883 ± 2.730 | 2.151 ± 2.566 |
| 10000 | 7.024 ± 2.452 | 11.698 ± 7.001 | 7.105 ± 1.994 |
