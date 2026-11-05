import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

public class InventoryExperiment {
    record Medicine(String code, int stock, int sequence) { }
    static final Comparator<Medicine> CODE = Comparator.comparing(Medicine::code);

    static Map<String, Medicine> emptyMap(String type) {
        return switch (type) {
            case "TreeMap" -> new TreeMap<>();
            case "LinkedHashMap" -> new LinkedHashMap<>();
            default -> new HashMap<>();
        };
    }

    static Map<String, Medicine> inventory(String type, List<Medicine> medicines) {
        Map<String, Medicine> map = emptyMap(type);
        for (Medicine medicine : medicines) map.put(medicine.code(), medicine);
        return map;
    }

    static Iterable<Medicine> reportRows(Map<String, Medicine> map, boolean codeOrder) {
        if (codeOrder && map instanceof TreeMap) return map.values();
        if (!codeOrder && map instanceof LinkedHashMap) return map.values();
        List<Medicine> rows = new ArrayList<>(map.values());
        rows.sort(codeOrder ? CODE : Comparator.comparingInt(Medicine::sequence));
        return rows;
    }

    static String render(Iterable<Medicine> rows) {
        StringBuilder report = new StringBuilder();
        for (Medicine medicine : rows) report.append(medicine.code()).append(',').append(medicine.stock()).append('\n');
        return report.toString();
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Q4 Pharmacy maps");
        System.out.println("Prediction: hash lookups average O(1); tree lookups O(log n); wrong-order reports require O(n log n) sorting.");
        System.out.println("Acceptance criterion declared before timing: mean collection lookup below 10000 ns (10 microseconds).");
        List<Medicine> all = new ArrayList<>();
        for (String row : Files.readAllLines(Path.of("medicines.csv")).subList(1, 100001)) {
            String[] values = row.split(",");
            all.add(new Medicine(values[0], Integer.parseInt(values[1]), all.size()));
        }
        try (Bench bench = new Bench(); var ordering = Files.newBufferedWriter(Path.of("order_checks.csv"))) {
            ordering.write("size,structure,code_order_ok,insertion_order_ok\n");
            for (String sizeRow : Files.readAllLines(Path.of("sizes.csv")).subList(1, 4)) {
                int size = Integer.parseInt(sizeRow);
                List<Medicine> medicines = new ArrayList<>(all.subList(0, size));
                List<Medicine> codeRows = new ArrayList<>(medicines);
                codeRows.sort(CODE);
                String expectedCode = render(codeRows);
                String expectedInsertion = render(medicines);
                Random random = new Random(42);
                String[] queries = new String[100000];
                for (int index = 0; index < queries.length; index++) {
                    queries[index] = new String(medicines.get(random.nextInt(size)).code());
                }
                for (String type : List.of("HashMap", "TreeMap", "LinkedHashMap")) {
                    Map<String, Medicine> check = inventory(type, medicines);
                    boolean codeOK = render(reportRows(check, true)).equals(expectedCode);
                    boolean insertionOK = render(reportRows(check, false)).equals(expectedInsertion);
                    Bench.require(codeOK && insertionOK && check.get("MISSING") == null, "Map report or missing-key check failed");
                    ordering.write(size + "," + type + "," + codeOK + "," + insertionOK + "\n");
                    bench.measure(size, type, "put", size, () -> {
                        Map<String, Medicine> map = emptyMap(type);
                        return () -> {
                            for (Medicine medicine : medicines) map.put(medicine.code(), medicine);
                            return map.size();
                        };
                    });
                    bench.measure(size, type, "100000-lookups", queries.length, () -> {
                        Map<String, Medicine> map = inventory(type, medicines);
                        return () -> {
                            long stock = 0;
                            for (String code : queries) stock += map.get(code).stock();
                            return stock;
                        };
                    });
                    for (boolean codeOrder : List.of(true, false)) {
                        bench.measure(size, type, codeOrder ? "code-report" : "insertion-report", 1, () -> {
                            Map<String, Medicine> map = inventory(type, medicines);
                            return () -> render(reportRows(map, codeOrder)).length();
                        });
                    }
                }
                Medicine[] sorted = codeRows.toArray(new Medicine[0]);
                Medicine[] queryObjects = new Medicine[queries.length];
                for (int index = 0; index < queries.length; index++) queryObjects[index] = new Medicine(queries[index], 0, 0);
                for (int index = 0; index < size; index++) {
                    Bench.require(Arrays.binarySearch(sorted, sorted[index], CODE) == index, "Array search failed");
                }
                bench.measure(size, "SortedArray", "100000-lookups", queries.length, () -> () -> {
                    long stock = 0;
                    for (Medicine query : queryObjects) stock += sorted[Arrays.binarySearch(sorted, query, CODE)].stock();
                    return stock;
                });
                if (size == 100) {
                    Files.writeString(Path.of("code_order_report.csv"), expectedCode);
                    Files.writeString(Path.of("insertion_order_report.csv"), expectedInsertion);
                }
                System.out.println("PASS n=" + size + ": all report orders, missing-key check and binary-search positions correct.");
            }
        }
    }
}
