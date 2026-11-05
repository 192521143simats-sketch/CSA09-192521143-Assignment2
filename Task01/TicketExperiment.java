import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class TicketExperiment {
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
        List<String> allFields = Files.readAllLines(Path.of("fields.txt"));
        List<String> workloads = Files.readAllLines(Path.of("workloads.csv"));
        try (Bench bench = new Bench(); var counts = Files.newBufferedWriter(Path.of("operation_counts.csv"))) {
            counts.write("tickets,fields,batches,characters_per_ticket,plus_string_results_per_ticket,plus_characters_copied,builder_appended_characters\n");
            for (String row : workloads.subList(1, workloads.size())) {
                String[] values = row.split(",");
                int tickets = Integer.parseInt(values[0]);
                int count = Integer.parseInt(values[1]);
                int batches = Integer.parseInt(values[2]);
                List<String> fields = new ArrayList<>(allFields.subList(0, count));
                String expected = String.join("\n", fields) + "\n";
                StringJoiner joiner = new StringJoiner("\n", "", "\n");
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
                for (String method : List.of("String", "StringBuilder", "StringBuffer")) {
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
