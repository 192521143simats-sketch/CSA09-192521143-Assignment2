import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;

public class TriageExperiment {
    record Entry(String name, int severity, long arrival, long arrivalMinute) {
        Entry {
            if (severity < 1 || severity > 5) throw new IllegalArgumentException("Severity must be 1..5");
        }
        @Override public String toString() { return name + "(S" + severity + ")"; }
    }
    record Aged(Entry patient, int effectiveSeverity) { }
    static final Comparator<Entry> ORDER = Comparator.comparingInt(Entry::severity).thenComparingLong(Entry::arrival);

    static List<Entry> drain(PriorityQueue<Entry> queue) {
        List<Entry> result = new ArrayList<>();
        while (!queue.isEmpty()) result.add(queue.poll());
        return result;
    }

    static List<Entry> sortedLinkedList(List<Entry> input) {
        LinkedList<Entry> queue = new LinkedList<>();
        for (Entry patient : input) {
            var cursor = queue.listIterator();
            while (cursor.hasNext()) {
                if (ORDER.compare(patient, cursor.next()) < 0) {
                    cursor.previous();
                    break;
                }
            }
            cursor.add(patient);
        }
        List<Entry> result = new ArrayList<>();
        while (!queue.isEmpty()) result.add(queue.removeFirst());
        return result;
    }

    static List<Entry> fiveQueues(List<Entry> input) {
        List<ArrayDeque<Entry>> queues = new ArrayList<>();
        for (int severity = 1; severity <= 5; severity++) queues.add(new ArrayDeque<>());
        for (Entry patient : input) queues.get(patient.severity() - 1).addLast(patient);
        List<Entry> result = new ArrayList<>();
        while (result.size() < input.size()) {
            for (ArrayDeque<Entry> queue : queues) {
                if (!queue.isEmpty()) {
                    result.add(queue.removeFirst());
                    break;
                }
            }
        }
        return result;
    }

    static PriorityQueue<Aged> agedSnapshot(List<Entry> waiting, long nowMinute) {
        Comparator<Aged> order = Comparator.comparingInt(Aged::effectiveSeverity)
                .thenComparingLong(item -> item.patient().arrival());
        PriorityQueue<Aged> rebuilt = new PriorityQueue<>(order);
        for (Entry patient : waiting) {
            long waited = Math.max(0, nowMinute - patient.arrivalMinute());
            int effective = (int) Math.max(1L, patient.severity() - waited / 30);
            rebuilt.add(new Aged(patient, effective));
        }
        return rebuilt;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Q6 Triage queue");
        System.out.println("Prediction before timing: heap admit and next are O(log n); a full drain is O(n log n).");
        System.out.println("Hand order: Meena Kumar Lata Omar Ravi John Priya Asha");
        List<Entry> patients = new ArrayList<>();
        List<String> rows = Files.readAllLines(Path.of("patients.csv"));
        for (String row : rows.subList(1, rows.size())) {
            String[] fields = row.split(",");
            patients.add(new Entry(fields[0], Integer.parseInt(fields[1]), patients.size(), 0));
        }
        PriorityQueue<Entry> queue = new PriorityQueue<>(ORDER);
        queue.addAll(patients);
        System.out.println("Heap storage iteration (not treatment order): " + queue);
        PriorityQueue<Entry> snapshot = new PriorityQueue<>(queue);
        List<Entry> actual = drain(snapshot);
        String names = String.join(" ", actual.stream().map(Entry::name).toList());
        Bench.require(names.equals("Meena Kumar Lata Omar Ravi John Priya Asha"), "Hand order mismatch");
        Bench.require(queue.size() == 8, "Displaying snapshot consumed live queue");
        System.out.println("Observed treatment order: " + names);
        PriorityQueue<Entry> witness = new PriorityQueue<>(ORDER);
        int[] severities = {1, 4, 2, 5, 4, 3, 2, 5};
        for (int index = 0; index < severities.length; index++) {
            witness.add(new Entry("W" + index, severities[index], index, 0));
        }
        List<Entry> heapLayout = new ArrayList<>(witness);
        Bench.require(!heapLayout.equals(drain(new PriorityQueue<>(witness))), "Non-order witness failed");
        System.out.println("Separate heap iteration witness: " + witness);
        Bench.require(sortedLinkedList(patients).equals(actual) && fiveQueues(patients).equals(actual), "Alternative queue differs");
        Entry oldMinor = new Entry("OldMinor", 5, 0, 0);
        List<Entry> agingInput = List.of(oldMinor, new Entry("NewCritical", 1, 1, 120));
        var aged = agedSnapshot(agingInput, 120);
        Bench.require(aged.poll().patient() == oldMinor, "Aging failed to promote old patient");
        System.out.println("Aging simulation at minute 120: OldMinor reaches effective severity 1 and precedes NewCritical by arrival.");
        try (Bench bench = new Bench(); var counts = Files.newBufferedWriter(Path.of("operation_counts.csv"))) {
            counts.write("size,admit_comparisons,serve_comparisons,single_critical_admit_comparisons\n");
            for (String row : Files.readAllLines(Path.of("sizes.csv")).subList(1, 4)) {
                int size = Integer.parseInt(row);
                Random random = new Random(42);
                List<Entry> input = new ArrayList<>();
                for (int index = 0; index < size; index++) input.add(new Entry("P" + index, random.nextInt(5) + 1, index, index));
                long[] comparisons = {0};
                Comparator<Entry> counted = (a, b) -> { comparisons[0]++; return ORDER.compare(a, b); };
                PriorityQueue<Entry> checked = new PriorityQueue<>(size, counted);
                checked.addAll(input);
                long addedComparisons = comparisons[0];
                comparisons[0] = 0;
                List<Entry> ordered = drain(checked);
                for (int index = 1; index < ordered.size(); index++) {
                    Bench.require(ORDER.compare(ordered.get(index - 1), ordered.get(index)) < 0, "Heap order or FIFO failed");
                }
                long servedComparisons = comparisons[0];
                PriorityQueue<Entry> minorQueue = new PriorityQueue<>(size + 1, counted);
                for (int index = 0; index < size; index++) minorQueue.add(new Entry("Minor" + index, 5, index, index));
                comparisons[0] = 0;
                minorQueue.add(new Entry("Critical", 1, size, size));
                counts.write(size + "," + addedComparisons + "," + servedComparisons + "," + comparisons[0] + "\n");
                bench.measure(size, "PriorityQueue", "admit", size, () -> {
                    PriorityQueue<Entry> target = new PriorityQueue<>(size, ORDER);
                    return () -> { for (Entry patient : input) target.add(patient); return target.size(); };
                });
                bench.measure(size, "PriorityQueue", "serve", size, () -> {
                    PriorityQueue<Entry> target = new PriorityQueue<>(size, ORDER);
                    target.addAll(input);
                    return () -> {
                        long checksum = 0;
                        while (!target.isEmpty()) checksum += target.poll().arrival();
                        return checksum;
                    };
                });
            }
        }
        Bench.require(new PriorityQueue<>(ORDER).poll() == null, "Empty queue failed");
        try {
            new Entry("Invalid", 0, 0, 0);
            throw new AssertionError("Invalid severity accepted");
        } catch (IllegalArgumentException expected) {
            System.out.println("PASS: invalid severity rejected; empty poll returns null.");
        }
        System.out.println("PASS: hand order, FIFO ties, display snapshot, both alternative queues and aging rule.");
    }
}
