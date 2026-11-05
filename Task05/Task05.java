import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Random;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

public class Task05 {
    public static void main(String[] args) throws Exception {
        LeaderboardDemo.main(args);
        RankingExperiment.main(args);
    }
}

class Team {
    final String name; final int points; final int goalDiff;
    Team(String name, int points, int goalDiff) {
        this.name = name; this.points = points; this.goalDiff = goalDiff;
    }
    @Override public String toString() {
        return name + "(" + points + "," + goalDiff + ")";
    }
}

class Leaderboard<T> {
    private final List<T> items = new ArrayList<>();
    private final Comparator<? super T> order;

    Leaderboard(Comparator<? super T> order) { this.order = order; }

    void add(T item) { items.add(item); }

    List<T> ranking() {
        List<T> sorted = new ArrayList<>(items);
        sorted.sort(order);
        return sorted;
    }
}

class LeaderboardDemo {
    public static void main(String[] args) {
        Comparator<Team> byRank = Comparator.comparingInt((Team t) -> t.points).reversed()
                .thenComparing(Comparator.comparingInt((Team t) -> t.goalDiff).reversed())
                .thenComparing(t -> t.name);

        Leaderboard<Team> board = new Leaderboard<>(byRank);
        board.add(new Team("Panthers", 9, 4));
        board.add(new Team("Eagles", 9, 6));
        board.add(new Team("Cobras", 6, 1));
        board.add(new Team("Bisons", 9, 4));
        System.out.println(board.ranking());
    }
}

class RankingExperiment {
    static final Comparator<Team> ORDER = Comparator.comparingInt((Team t) -> t.points).reversed()
            .thenComparing(Comparator.comparingInt((Team t) -> t.goalDiff).reversed())
            .thenComparing(t -> t.name);

    static class NaturalTeam extends Team implements Comparable<NaturalTeam> {
        NaturalTeam(String name, int points, int goalDiff) { super(name, points, goalDiff); }
        @Override public int compareTo(NaturalTeam other) { return ORDER.compare(this, other); }
    }

    static <T> void addAll(Leaderboard<T> board, List<? extends T> additions) {
        for (T item : additions) board.add(item);
    }

    static <T extends Comparable<? super T>> List<T> naturalRanking(List<T> input) {
        List<T> result = new ArrayList<>(input);
        Collections.sort(result);
        return result;
    }

    static List<Team> stableRanking(List<? extends Team> input) {
        List<Team> result = new ArrayList<>(input);
        result.sort(Comparator.comparing(t -> t.name));
        result.sort(Comparator.comparingInt((Team t) -> t.goalDiff).reversed());
        result.sort(Comparator.comparingInt((Team t) -> t.points).reversed());
        return result;
    }

    static boolean lawsHold(List<? extends Team> teams, Comparator<Team> comparator) {
        for (Team a : teams) {
            if (comparator.compare(a, a) != 0) return false;
            for (Team b : teams) {
                int ab = Integer.signum(comparator.compare(a, b));
                if (ab != -Integer.signum(comparator.compare(b, a))) return false;
                for (Team c : teams) {
                    int bc = comparator.compare(b, c);
                    int ac = comparator.compare(a, c);
                    if (ab < 0 && bc < 0 && ac >= 0) return false;
                    if (ab == 0 && Integer.signum(ac) != Integer.signum(comparator.compare(b, c))) return false;
                }
            }
        }
        return true;
    }

    static List<NaturalTeam> readTeams(String filename) throws Exception {
        List<NaturalTeam> teams = new ArrayList<>();
        List<String> rows;
        if (filename.equals("ties.csv")) rows = List.of("name,points,goalDiff",
                "Panthers,9,4",
                "Eagles,9,6",
                "Cobras,6,1",
                "Bisons,9,4",
                "Falcons,9,6",
                "Aardvarks,6,1",
                "Tigers,9,4",
                "Bisons,9,4");
        else if (filename.equals("extremes.csv")) rows = List.of("name,points,goalDiff",
                "Maximum,2147483647,0",
                "Zero,0,0",
                "Minimum,-2147483648,0");
        else throw new IllegalArgumentException("Unknown dataset");
        for (String row : rows.subList(1, rows.size())) {
            String[] fields = row.split(",");
            teams.add(new NaturalTeam(fields[0], Integer.parseInt(fields[1]), Integer.parseInt(fields[2])));
        }
        return teams;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Q5 Reusable ranking");
        System.out.println("Prediction: chain, Comparable and three stable sorts agree; subtraction fails on extreme integers.");
        List<NaturalTeam> ties = readTeams("ties.csv");
        List<NaturalTeam> extremes = readTeams("extremes.csv");
        Comparator<Team> subtraction = (a, b) -> b.points - a.points;
        Comparator<Team> missingName = Comparator.comparingInt((Team t) -> t.points).reversed()
                .thenComparing(Comparator.comparingInt((Team t) -> t.goalDiff).reversed());
        String expected = "[Eagles(9,6), Falcons(9,6), Bisons(9,4), Bisons(9,4), Panthers(9,4), Tigers(9,4), Aardvarks(6,1), Cobras(6,1)]";
        try (var checks = Files.newBufferedWriter(Evidence.path("correctness.csv"))) {
            checks.write("trial,chain_laws_hold,subtraction_laws_hold,tie_breaker_complete,copies_independent,alternatives_agree\n");
            for (int trial = 1; trial <= 7; trial++) {
                Leaderboard<Team> board = new Leaderboard<>(ORDER);
                addAll(board, ties);
                List<Team> ranking = board.ranking();
                Bench.require(ranking.toString().equals(expected), "Tie ranking differs");
                Bench.require(ranking.toString().equals(stableRanking(ties).toString())
                        && ranking.toString().equals(naturalRanking(ties).toString()), "Alternative ranking differs");
                ranking.clear();
                Bench.require(board.ranking().toString().equals(expected), "Returned list changed board");
                Bench.require(lawsHold(ties, ORDER) && lawsHold(extremes, ORDER), "Comparator laws failed");
                Bench.require(!lawsHold(extremes, subtraction), "Faulty subtraction escaped detection");
                Bench.require(missingName.compare(ties.get(0), ties.get(3)) == 0, "Missing name comparator should merge tie");
                checks.write(trial + ",true,false,false,true,true\n");
            }
        }
        System.out.println("Ties: " + expected);
        System.out.println("Extremes correct: " + naturalRanking(extremes));
        System.out.println("PASS: 7 checks of comparator laws, tie order, independent copies and both alternatives.");
        System.out.println("Faults detected: overflowing subtraction violates comparison laws; missing name leaves required tie unresolved.");
        try (Bench bench = new Bench()) {
            for (String row : List.of("size",
                "100",
                "1000",
                "10000").subList(1, 4)) {
                int size = Integer.parseInt(row);
                Random random = new Random(42);
                List<NaturalTeam> input = new ArrayList<>();
                for (int index = 0; index < size; index++) {
                    input.add(new NaturalTeam("Team" + index, random.nextInt(20), random.nextInt(10)));
                }
                Leaderboard<Team> board = new Leaderboard<>(ORDER);
                addAll(board, input);
                bench.measure(size, "Comparator",
                "ranking", 1, () -> () -> board.ranking().get(0).points);
                bench.measure(size, "Comparable",
                "ranking", 1, () -> () -> naturalRanking(input).get(0).points);
                bench.measure(size, "Stable3Sorts",
                "ranking", 1, () -> () -> stableRanking(input).get(0).points);
                Bench.require(board.ranking().toString().equals(stableRanking(input).toString())
                        && board.ranking().toString().equals(naturalRanking(input).toString()), "Large rankings differ");
            }
        }
        Leaderboard<Team> empty = new Leaderboard<>(ORDER);
        Bench.require(empty.ranking().isEmpty(), "Empty ranking failed");
        System.out.println("PASS: empty board and all measured rankings correct.");
    }
}

final class Bench implements AutoCloseable {
    public static volatile long sink;
    private final PrintWriter raw;
    private final PrintWriter summary;
    private final int warmups;
    private final int repeats;

    public Bench() throws IOException {
        Locale.setDefault(Locale.US);
        warmups = 3;
        repeats = 7;
        raw = new PrintWriter(Evidence.path("raw.csv").toFile());
        summary = new PrintWriter(Evidence.path("summary.csv").toFile());
        raw.println("size,structure,operation,trial,total_ns,operations,ns_per_op,heap_before_bytes,heap_after_bytes,checksum");
        summary.println("size,structure,operation,operations,mean_ns_per_op,sd_ns_per_op,min_ns_per_op,max_ns_per_op");
        System.out.printf("Java %s | %s | %s%n", System.getProperty("java.version"),
                System.getProperty("os.name"), System.getProperty("os.arch"));
        System.out.printf("Warmups=%d; measured trials=%d; heap is sampled without forced GC.%n", warmups, repeats);
    }

    public void measure(int size, String structure, String operation, long operations,
                        Supplier<LongSupplier> prepare) {
        if (operations <= 0) throw new IllegalArgumentException("Positive operation count required");
        for (int trial = 0; trial < warmups; trial++) sink = prepare.get().getAsLong();
        double[] samples = new double[repeats];
        Runtime runtime = Runtime.getRuntime();
        for (int trial = 0; trial < repeats; trial++) {
            LongSupplier action = prepare.get();
            long before = runtime.totalMemory() - runtime.freeMemory();
            long start = System.nanoTime();
            long checksum = action.getAsLong();
            long elapsed = System.nanoTime() - start;
            sink = checksum;
            long after = runtime.totalMemory() - runtime.freeMemory();
            samples[trial] = (double) elapsed / operations;
            raw.printf("%d,%s,%s,%d,%d,%d,%.6f,%d,%d,%d%n", size, structure, operation,
                    trial + 1, elapsed, operations, samples[trial], before, after, checksum);
        }
        double mean = 0;
        double min = Double.POSITIVE_INFINITY;
        double max = 0;
        for (double sample : samples) {
            mean += sample / repeats;
            min = Math.min(min, sample);
            max = Math.max(max, sample);
        }
        double variance = 0;
        for (double sample : samples) variance += (sample - mean) * (sample - mean);
        double deviation = Math.sqrt(variance / (repeats - 1));
        summary.printf("%d,%s,%s,%d,%.6f,%.6f,%.6f,%.6f%n", size, structure, operation,
                operations, mean, deviation, min, max);
        System.out.printf("%6d %-13s %-16s %10.2f +/- %10.2f ns/op%n",
                size, structure, operation, mean, deviation);
        raw.flush();
        summary.flush();
    }

    public static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    @Override public void close() {
        raw.close();
        summary.close();
    }
}

final class Evidence {
    static Path path(String name) throws java.io.IOException {
        Path directory = Path.of(System.getProperty("assignment.evidence",
                "../docs/evidence/Task05"));
        Files.createDirectories(directory);
        return directory.resolve(name);
    }
}
