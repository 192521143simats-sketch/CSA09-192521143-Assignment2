import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Random;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

public class Task02 {
    public static void main(String[] args) throws Exception {
        PlaylistExperiment.main(args);
    }
}

class PlaylistExperiment {
    record Song(int id, String title) { }

    static List<Song> playlist(String type, List<Song> songs) {
        return type.equals("ArrayList") ? new ArrayList<>(songs) : new LinkedList<>(songs);
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Q2 Playlist operations");
        System.out.println("Prediction: indexed access favours ArrayList; front edits favour LinkedList; iteration is linear for both.");
        List<String> rows = List.of("size,operations",
                "100,500",
                "10000,500",
                "100000,500");
        Song inserted = new Song(-1, "New song");
        try (Bench bench = new Bench(); var counts = Files.newBufferedWriter(Evidence.path("operation_counts.csv"))) {
            counts.write("size,pairs,array_front_slots_shifted,array_middle_slots_shifted,linked_middle_approx_node_steps\n");
            for (String row : rows.subList(1, rows.size())) {
                String[] values = row.split(",");
                int size = Integer.parseInt(values[0]);
                int operations = Integer.parseInt(values[1]);
                List<Song> songs = new ArrayList<>();
                for (int index = 0; index < size; index++) songs.add(new Song(index, "Song " + index));
                int[] positions = new Random(42).ints(operations, 0, size).toArray();
                counts.write(size + "," + operations + "," + (2L * size * operations) + ","
                        + (2L * (size - size / 2) * operations) + "," + ((long) size * operations) + "\n");
                for (String type : List.of("ArrayList",
                "LinkedList")) {
                    bench.measure(size, type, "append", operations, () -> {
                        List<Song> list = playlist(type, songs);
                        return () -> {
                            for (int step = 0; step < operations; step++) list.add(inserted);
                            return list.size();
                        };
                    });
                    for (String location : List.of("front",
                "middle")) {
                        List<Song> checked = playlist(type, songs);
                        int checkedPosition = location.equals("front") ? 0 : size / 2;
                        checked.add(checkedPosition, inserted);
                        Bench.require(checked.remove(checkedPosition) == inserted && checked.equals(songs),
                                "Edit pair changed playlist");
                        bench.measure(size, type, location + "-edit-pair", operations, () -> {
                            List<Song> list = playlist(type, songs);
                            int position = location.equals("front") ? 0 : size / 2;
                            return () -> {
                                long checksum = 0;
                                for (int step = 0; step < operations; step++) {
                                    list.add(position, inserted);
                                    checksum += list.remove(position).id();
                                }
                                return checksum;
                            };
                        });
                    }
                    bench.measure(size, type, "get-k", operations, () -> {
                        List<Song> list = playlist(type, songs);
                        return () -> {
                            long checksum = 0;
                            for (int position : positions) checksum += list.get(position).id();
                            return checksum;
                        };
                    });
                    bench.measure(size, type, "iterate", size, () -> {
                        List<Song> list = playlist(type, songs);
                        return () -> {
                            long checksum = 0;
                            for (Song song : list) checksum += song.id();
                            Bench.require(checksum == (long) size * (size - 1) / 2, "Iteration lost songs");
                            return checksum;
                        };
                    });
                }
                for (String type : List.of("ArrayDeque",
                "LinkedList")) {
                    bench.measure(size, type, "up-next-cycle", operations, () -> {
                        java.util.Deque<Song> deque = type.equals("ArrayDeque")
                                ? new ArrayDeque<>(songs) : new LinkedList<>(songs);
                        return () -> {
                            long checksum = 0;
                            for (int step = 0; step < operations; step++) {
                                deque.addFirst(inserted);
                                deque.addLast(inserted);
                                checksum += deque.removeFirst().id();
                                checksum += deque.removeFirst().id();
                            }
                            Bench.require(deque.size() == size, "Up Next size changed");
                            return checksum;
                        };
                    });
                }
            }
        }
        List<Song> empty = new ArrayList<>();
        try {
            empty.get(0);
            throw new AssertionError("Invalid index accepted");
        } catch (IndexOutOfBoundsException expected) {
            System.out.println("PASS: empty-playlist invalid index rejected.");
        }
        System.out.println("PASS: edit pairs restore order; iteration checksums match; Up Next size preserved.");
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
                "../docs/evidence/Task02"));
        Files.createDirectories(directory);
        return directory.resolve(name);
    }
}
