import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class RankingRedesign {
    record Team(String name, int points, int goalDiff) { }
    static final Comparator<Team> ORDER = Comparator.comparingInt(Team::points).reversed()
            .thenComparing(Comparator.comparingInt(Team::goalDiff).reversed())
            .thenComparing(Team::name);

    interface Ranking<T> {
        void add(T item);
        List<T> ranking();
        long sortCount();
    }

    static class Original<T> implements Ranking<T> {
        final List<T> items = new ArrayList<>();
        final Comparator<? super T> order;
        long sorts;
        Original(Comparator<? super T> order) { this.order = order; }
        @Override public void add(T item) { items.add(item); }
        @Override public List<T> ranking() {
            List<T> result = new ArrayList<>(items);
            result.sort(order);
            sorts++;
            return result;
        }
        @Override public long sortCount() { return sorts; }
    }

    static class Cached<T> implements Ranking<T> {
        final List<T> items = new ArrayList<>();
        final Comparator<? super T> order;
        List<T> sorted = List.of();
        boolean dirty;
        long sorts;
        Cached(Comparator<? super T> order) { this.order = order; }
        @Override public void add(T item) {
            items.add(item);
            dirty = true;
        }
        @Override public List<T> ranking() {
            if (dirty) {
                List<T> refreshed = new ArrayList<>(items);
                refreshed.sort(order);
                sorted = refreshed;
                dirty = false;
                sorts++;
            }
            return new ArrayList<>(sorted);
        }
        @Override public long sortCount() { return sorts; }
    }

    static Ranking<Team> create(String type, Comparator<Team> order, List<Team> initial) {
        Ranking<Team> board = type.equals("Original") ? new Original<>(order) : new Cached<>(order);
        for (Team team : initial) board.add(team);
        return board;
    }

    static long request(Ranking<Team> board, int requests, int updateEvery, Team[] updates) {
        long checksum = 0;
        for (int request = 0; request < requests; request++) {
            if (updateEvery > 0 && request % updateEvery == 0) board.add(updates[request]);
            List<Team> result = board.ranking();
            for (Team team : result) checksum += team.points();
        }
        return checksum;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Q7 Redesign repeated leaderboard queries");
        System.out.println("Diagnosis from Q5: every ranking copies and sorts the same unchanged data again.");
        System.out.println("Prediction before validation: 30 read-only requests reduce sorts 30->1; update-every-10 reduces 30->3.");
        System.out.println("Objective: at least 90% fewer sorts and 2x lower mean ns/request for read-only n>=1000.");
        System.out.println("Counterprediction: update-before-every-request gets no sort saving and may be slower.");
        try (Bench bench = new Bench(); var counts = Files.newBufferedWriter(Path.of("results", "operation_counts.csv"))) {
            counts.write("size,requests,update_every,structure,sorts,comparator_calls,result_references_copied\n");
            List<String> conditions = Files.readAllLines(Path.of("inputs", "conditions.csv"));
            for (String row : conditions.subList(1, conditions.size())) {
                String[] values = row.split(",");
                int size = Integer.parseInt(values[0]);
                int requests = Integer.parseInt(values[1]);
                int updateEvery = Integer.parseInt(values[2]);
                Random random = new Random(42);
                List<Team> initial = new ArrayList<>();
                for (int index = 0; index < size; index++) initial.add(new Team("Team" + index, random.nextInt(20), random.nextInt(10)));
                Team[] updates = new Team[requests];
                for (int index = 0; index < requests; index++) updates[index] = new Team("Added" + index, 30 + index, index);
                Ranking<Team> original = create("Original", ORDER, initial);
                Ranking<Team> revised = create("Cached", ORDER, initial);
                long referencesCopied = 0;
                for (int request = 0; request < requests; request++) {
                    if (updateEvery > 0 && request % updateEvery == 0) {
                        original.add(updates[request]);
                        revised.add(updates[request]);
                    }
                    List<Team> expected = original.ranking();
                    List<Team> observed = revised.ranking();
                    Bench.require(expected.equals(observed), "Cached ranking stale or misordered");
                    referencesCopied += expected.size();
                    observed.clear();
                }
                for (String type : List.of("Original", "Cached")) {
                    long[] comparisons = {0};
                    Comparator<Team> counted = (a, b) -> { comparisons[0]++; return ORDER.compare(a, b); };
                    Ranking<Team> countedBoard = create(type, counted, initial);
                    request(countedBoard, requests, updateEvery, updates);
                    counts.write(size + "," + requests + "," + updateEvery + "," + type + ","
                            + countedBoard.sortCount() + "," + comparisons[0] + "," + referencesCopied + "\n");
                    bench.measure(size, type, "updateEvery" + updateEvery, requests, () -> {
                        Ranking<Team> board = create(type, ORDER, initial);
                        return () -> request(board, requests, updateEvery, updates);
                    });
                }
                long expectedSorts = updateEvery == 0 ? 1 : (requests + updateEvery - 1) / updateEvery;
                Bench.require(revised.sortCount() == expectedSorts, "Unexpected cache invalidation count");
            }
        }
        Ranking<Team> board = new Cached<>(ORDER);
        Bench.require(board.ranking().isEmpty(), "Empty cache incorrect");
        board.add(new Team("Old", 1, 0));
        List<Team> oldSnapshot = board.ranking();
        board.add(new Team("New", 9, 0));
        List<Team> newSnapshot = board.ranking();
        Bench.require(oldSnapshot.size() == 1 && newSnapshot.size() == 2 && newSnapshot.get(0).name().equals("New"),
                "Snapshot isolation failed");
        newSnapshot.clear();
        Bench.require(board.ranking().size() == 2, "Client mutation changed cache");
        System.out.println("PASS: all requests equal original; invalidation counts match; empty and isolated mutable snapshots verified.");
    }
}
