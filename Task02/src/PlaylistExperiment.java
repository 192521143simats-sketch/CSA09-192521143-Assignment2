import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class PlaylistExperiment {
    record Song(int id, String title) { }

    static List<Song> playlist(String type, List<Song> songs) {
        return type.equals("ArrayList") ? new ArrayList<>(songs) : new LinkedList<>(songs);
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Q2 Playlist operations");
        System.out.println("Prediction: indexed access favours ArrayList; front edits favour LinkedList; iteration is linear for both.");
        List<String> rows = Files.readAllLines(Path.of("inputs", "sizes.csv"));
        Song inserted = new Song(-1, "New song");
        try (Bench bench = new Bench(); var counts = Files.newBufferedWriter(Path.of("results", "operation_counts.csv"))) {
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
                for (String type : List.of("ArrayList", "LinkedList")) {
                    bench.measure(size, type, "append", operations, () -> {
                        List<Song> list = playlist(type, songs);
                        return () -> {
                            for (int step = 0; step < operations; step++) list.add(inserted);
                            return list.size();
                        };
                    });
                    for (String location : List.of("front", "middle")) {
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
                for (String type : List.of("ArrayDeque", "LinkedList")) {
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
