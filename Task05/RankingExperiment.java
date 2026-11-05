import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class RankingExperiment {
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
        List<String> rows = Files.readAllLines(Path.of(filename));
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
        try (var checks = Files.newBufferedWriter(Path.of("correctness.csv"))) {
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
            for (String row : Files.readAllLines(Path.of("sizes.csv")).subList(1, 4)) {
                int size = Integer.parseInt(row);
                Random random = new Random(42);
                List<NaturalTeam> input = new ArrayList<>();
                for (int index = 0; index < size; index++) {
                    input.add(new NaturalTeam("Team" + index, random.nextInt(20), random.nextInt(10)));
                }
                Leaderboard<Team> board = new Leaderboard<>(ORDER);
                addAll(board, input);
                bench.measure(size, "Comparator", "ranking", 1, () -> () -> board.ranking().get(0).points);
                bench.measure(size, "Comparable", "ranking", 1, () -> () -> naturalRanking(input).get(0).points);
                bench.measure(size, "Stable3Sorts", "ranking", 1, () -> () -> stableRanking(input).get(0).points);
                Bench.require(board.ranking().toString().equals(stableRanking(input).toString())
                        && board.ranking().toString().equals(naturalRanking(input).toString()), "Large rankings differ");
            }
        }
        Leaderboard<Team> empty = new Leaderboard<>(ORDER);
        Bench.require(empty.ranking().isEmpty(), "Empty ranking failed");
        System.out.println("PASS: empty board and all measured rankings correct.");
    }
}
