import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.PriorityQueue;
import java.util.Properties;
import java.util.Random;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

public class Task06 {
    public static void main(String[] args) throws Exception {
        Triage.main(args);
        TriageExperiment.main(args);
    }
}

class Patient {
    final String name; final int severity; final long arrival;
    Patient(String name, int severity, long arrival) {
        this.name = name; this.severity = severity; this.arrival = arrival;
    }
    @Override public String toString() { return name + "(S" + severity + ")"; }
}

class Triage {
    private long counter = 0;

    private final PriorityQueue<Patient> queue = new PriorityQueue<>(
            Comparator.comparingInt((Patient p) -> p.severity)
                    .thenComparingLong(p -> p.arrival));

    void admit(String name, int severity) {
        queue.add(new Patient(name, severity, counter++));
    }

    Patient next() { return queue.poll(); }

    public static void main(String[] args) {
        Triage er = new Triage();
        er.admit("Ravi", 3); er.admit("Meena", 1); er.admit("John", 3);
        er.admit("Asha", 5); er.admit("Kumar", 1); er.admit("Lata", 2);
        StringBuilder order = new StringBuilder();
        Patient p;
        while ((p = er.next()) != null) order.append(p).append(' ');
        System.out.println(order.toString().trim());
    }
}

class TriageExperiment {
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
        List<String> rows = List.of("name,severity",
                "Ravi,3",
                "Meena,1",
                "John,3",
                "Asha,5",
                "Kumar,1",
                "Lata,2",
                "Priya,4",
                "Omar,2");
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
        try (Bench bench = new Bench(); var counts = Files.newBufferedWriter(Evidence.path("operation_counts.csv"))) {
            counts.write("size,admit_comparisons,serve_comparisons,single_critical_admit_comparisons\n");
            for (String row : List.of("size",
                "100",
                "10000",
                "50000").subList(1, 4)) {
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
                bench.measure(size, "PriorityQueue",
                "admit", size, () -> {
                    PriorityQueue<Entry> target = new PriorityQueue<>(size, ORDER);
                    return () -> { for (Entry patient : input) target.add(patient); return target.size(); };
                });
                bench.measure(size, "PriorityQueue",
                "serve", size, () -> {
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
                "../docs/evidence/Task06"));
        Files.createDirectories(directory);
        return directory.resolve(name);
    }
}
