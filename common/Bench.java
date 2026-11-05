import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

public final class Bench implements AutoCloseable {
    public static volatile long sink;
    private final PrintWriter raw;
    private final PrintWriter summary;
    private final int warmups;
    private final int repeats;

    public Bench() throws IOException {
        Locale.setDefault(Locale.US);
        Properties settings = new Properties();
        try (var input = Files.newInputStream(Path.of("input.properties"))) {
            settings.load(input);
        }
        warmups = Integer.parseInt(settings.getProperty("warmups", "3"));
        repeats = Integer.parseInt(settings.getProperty("repeats", "7"));
        raw = new PrintWriter("raw.csv");
        summary = new PrintWriter("summary.csv");
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
        System.out.printf("n=%-6d %-15s %-16s mean=%11.2f ns/op sd=%10.2f%n",
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
