import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.StringJoiner;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

public class Task01 {
    public static void main(String[] args) throws Exception {
        TicketExperiment.main(args);
    }
}

class TicketExperiment {
    static String build(List<String> fields, String method) {
        if (method.equals("String")) {
            String text = "";
            for (String field : fields) text = text + field + "\n";
            return text;
        }
        if (method.equals("StringBuffer")) {
            StringBuffer text = new StringBuffer();
            for (String field : fields) text.append(field).append('\n');
            return text.toString();
        }
        StringBuilder text = new StringBuilder();
        for (String field : fields) text.append(field).append('\n');
        return text.toString();
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Q1 Ticket construction");
        System.out.println("Prediction before timing: repeated + copies growing prefixes; builders grow amortized linearly.");
        List<String> allFields = new ArrayList<>();
        for (int index = 1; index <= 200; index++) {
            allFields.add(String.format(Locale.ROOT, "Field%03d=Value%03d", index, index));
        }
        List<String> workloads = List.of("tickets,fields,batches",
                "1,8,2000",
                "10000,50,1",
                "100,200,1");
        try (Bench bench = new Bench(); var counts = Files.newBufferedWriter(Evidence.path("operation_counts.csv"))) {
            counts.write("tickets,fields,batches,characters_per_ticket,plus_string_results_per_ticket,plus_characters_copied,builder_appended_characters\n");
            for (String row : workloads.subList(1, workloads.size())) {
                String[] values = row.split(",");
                int tickets = Integer.parseInt(values[0]);
                int count = Integer.parseInt(values[1]);
                int batches = Integer.parseInt(values[2]);
                List<String> fields = new ArrayList<>(allFields.subList(0, count));
                String expected = String.join("\n", fields) + "\n";
                StringJoiner joiner = new StringJoiner("\n",
                "",
                "\n");
                fields.forEach(joiner::add);
                Bench.require(expected.equals(joiner.toString()), "StringJoiner differs");
                long prefix = 0;
                long copied = 0;
                for (String field : fields) {
                    prefix += field.length() + 1;
                    copied += prefix;
                }
                counts.write(tickets + "," + count + "," + batches + "," + prefix + "," + count + "," + copied + "," + prefix + "\n");
                System.out.printf("Workload %d tickets x %d fields; %d batching repetitions; %d chars/ticket%n", tickets, count, batches, prefix);
                for (String method : List.of("String",
                "StringBuilder",
                "StringBuffer")) {
                    Bench.require(build(fields, method).equals(expected), "Ticket content differs");
                    bench.measure(count, method, tickets + "tickets", (long) tickets * batches, () -> () -> {
                        long checksum = 0;
                        for (int batch = 0; batch < batches; batch++) {
                            for (int ticket = 0; ticket < tickets; ticket++) checksum += build(fields, method).length();
                        }
                        return checksum;
                    });
                }
            }
        }
        StringBuffer sharedLog = new StringBuffer();
        List<Thread> writers = new ArrayList<>();
        for (int index = 0; index < 4; index++) {
            Thread writer = new Thread(() -> {
                for (int line = 0; line < 1000; line++) sharedLog.append("BOOKING\n");
            });
            writers.add(writer);
            writer.start();
        }
        for (Thread writer : writers) writer.join();
        Bench.require(sharedLog.toString().lines().count() == 4000, "Lost log records");
        System.out.println("PASS: all ticket texts equal; String.join and StringJoiner equal; shared log has 4000 intact records.");
        System.out.println("Use thread-confined StringBuilder per ticket; StringBuffer.append(completeRecord) for a shared log.");
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
                "../docs/evidence/Task01"));
        Files.createDirectories(directory);
        return directory.resolve(name);
    }
}
