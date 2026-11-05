import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

public class Task03 {
    public static void main(String[] args) throws Exception {
        ByteArrayOutputStream beforeBytes = new ByteArrayOutputStream();
        PrintStream console = System.out;
        try (PrintStream captured = new PrintStream(beforeBytes, true, StandardCharsets.UTF_8)) {
            System.setOut(captured);
            BeforeAttendance.main(args);
        } finally { System.setOut(console); }
        String before = beforeBytes.toString(StandardCharsets.UTF_8);
        Files.writeString(Evidence.path("before_execution.txt"), before);
        System.out.println("Before correction: " + before.trim());
        System.out.println("After correction:");
        Attendance.main(args);
        AttendanceExperiment.main(args);
    }
}

class BeforeStudent {
    private final String regNo;
    private final String name;

    BeforeStudent(String regNo, String name) { this.regNo = regNo; this.name = name; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BeforeStudent)) return false;
        return regNo.equals(((BeforeStudent) o).regNo);
    }

    @Override public String toString() { return regNo + " " + name; }
}

class BeforeAttendance {
    public static void main(String[] args) {
        Set<BeforeStudent> present = new HashSet<>();
        present.add(new BeforeStudent("192311001",
                "Arun"));
        present.add(new BeforeStudent("192311002",
                "Divya"));
        present.add(new BeforeStudent("192311001",
                "Arun"));
        System.out.println("Unique students: " + present.size());
    }
}

class Student {
    private final String regNo;
    private final String name;

    Student(String regNo, String name) { this.regNo = regNo; this.name = name; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Student)) return false;
        return regNo.equals(((Student) o).regNo);
    }

    @Override public int hashCode() { return regNo.hashCode(); }

    @Override public String toString() { return regNo + " " + name; }
}

class Attendance {
    public static void main(String[] args) {
        Set<Student> present = new HashSet<>();
        present.add(new Student("192311001",
                "Arun"));
        present.add(new Student("192311002",
                "Divya"));
        present.add(new Student("192311001",
                "Arun"));
        System.out.println("Unique students: " + present.size());
    }
}

class AttendanceExperiment {
    static class BrokenStudent {
        final String regNo;
        final String name;
        BrokenStudent(String regNo, String name) { this.regNo = regNo; this.name = name; }
        @Override public boolean equals(Object other) {
            return other instanceof BrokenStudent && regNo.equals(((BrokenStudent) other).regNo);
        }
    }

    static class MutableStudent {
        String regNo;
        MutableStudent(String regNo) { this.regNo = regNo; }
        @Override public boolean equals(Object other) {
            return other instanceof MutableStudent && regNo.equals(((MutableStudent) other).regNo);
        }
        @Override public int hashCode() { return regNo.hashCode(); }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Q3 Equality and hashing");
        System.out.println("Supplied code before TODO execution: " + Files.readString(Evidence.path("before_execution.txt")).trim());
        System.out.println("Predictions before tests: same-reference scans size 2; distinct-identity duplicate scans size 3.");
        System.out.println("Mutation A->B: size remains 1 but contains and remove fail; reinsertion can make size 2.");
        var scans = List.of("regNo,name",
                "192311001,Arun",
                "192311002,Divya",
                "192311001,Arun");
        try (var raw = Files.newBufferedWriter(Evidence.path("raw.csv"))) {
            raw.write("trial,test,predicted_size,actual_size,contains_after_mutation,remove_after_mutation\n");
            for (int trial = 1; trial <= 7; trial++) {
                BrokenStudent arun = new BrokenStudent("192311001",
                "Arun");
                Set<BrokenStudent> reused = new HashSet<>();
                reused.add(arun);
                reused.add(new BrokenStudent("192311002",
                "Divya"));
                reused.add(arun);
                Bench.require(reused.size() == 2, "Same-reference scan failed");
                raw.write(trial + ",same-reference,2," + reused.size() + ",NA,NA\n");
                Set<BrokenStudent> broken = new HashSet<>();
                BrokenStudent duplicate;
                do {
                    duplicate = new BrokenStudent(arun.regNo, arun.name);
                } while (duplicate.hashCode() == arun.hashCode());
                broken.add(arun);
                broken.add(new BrokenStudent("192311002",
                "Divya"));
                broken.add(duplicate);
                Bench.require(broken.size() == 3, "Distinct-hash duplicate not exposed");
                raw.write(trial + ",distinct-instances,3," + broken.size() + ",NA,NA\n");
                MutableStudent mutable = new MutableStudent("A");
                Set<MutableStudent> changing = new HashSet<>();
                changing.add(mutable);
                mutable.regNo = "B";
                boolean contains = changing.contains(mutable);
                boolean removed = changing.remove(mutable);
                Bench.require(changing.size() == 1 && !contains && !removed, "Mutation outcome differs");
                raw.write(trial + ",mutated-key,1," + changing.size() + "," + contains + "," + removed + "\n");
                changing.add(mutable);
                Bench.require(changing.size() == 2, "Mutated-key reinsertion not exposed");
                raw.write(trial + ",mutated-reinserted,2," + changing.size() + ",true,NA\n");
                Set<Student> corrected = new HashSet<>();
                Set<BrokenStudent> ordered = new TreeSet<>(Comparator.comparing(student -> student.regNo));
                Map<String, BrokenStudent> keyed = new HashMap<>();
                for (String row : scans.subList(1, scans.size())) {
                    String[] value = row.split(",");
                    corrected.add(new Student(value[0], value[1]));
                    BrokenStudent scan = new BrokenStudent(value[0], value[1]);
                    ordered.add(scan);
                    keyed.putIfAbsent(value[0], scan);
                }
                Bench.require(corrected.size() == 2 && ordered.size() == 2 && keyed.size() == 2,
                        "Corrected deduplication failed");
                Student first = new Student("192311001",
                "Arun");
                Student same = new Student("192311001",
                "Changed name");
                Student third = new Student("192311001",
                "Another name");
                Bench.require(first.equals(first) && first.equals(same) && same.equals(first)
                        && same.equals(third) && first.equals(third) && !first.equals(null)
                        && !first.equals("192311001") && first.hashCode() == same.hashCode()
                        && corrected.contains(same), "Equality contract failed");
                raw.write(trial + ",corrected,2," + corrected.size() + ",NA,NA\n");
            }
        }
        System.out.println("Observed in all 7 trials: reused-reference=2; distinct-instances=3; corrected=2.");
        System.out.println("Observed mutation: size=1, contains=false, remove=false; reinsertion size=2.");
        System.out.println("PASS: reflexivity, symmetry, transitivity, null/type rejection and equal-hash checks.");
        System.out.println("PASS: TreeSet by regNo and HashMap keyed by regNo both retain 2 students.");
    }
}

final class Bench {
    static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}

final class Evidence {
    static Path path(String name) throws java.io.IOException {
        Path directory = Path.of(System.getProperty("assignment.evidence",
                "../docs/evidence/Task03"));
        Files.createDirectories(directory);
        return directory.resolve(name);
    }
}
