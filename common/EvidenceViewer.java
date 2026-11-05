import java.awt.BorderLayout;
import java.awt.Font;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

public class EvidenceViewer {
    public static void main(String[] args) throws Exception {
        Path transcript = Path.of(args[0]).toAbsolutePath();
        var lines = Files.readAllLines(transcript);
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Assignment 2 - " + transcript.getParent().getFileName() + " - Executed output");
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            JLabel heading = new JLabel("Actual saved stdout | " + transcript.getParent().getFileName() + " | execution.txt");
            heading.setFont(new Font("SansSerif", Font.BOLD, 17));
            heading.setBorder(BorderFactory.createEmptyBorder(12, 15, 12, 15));
            frame.add(heading, BorderLayout.NORTH);
            JTabbedPane pages = new JTabbedPane();
            for (int start = 0; start < lines.size(); start += 29) {
                JTextArea area = new JTextArea(String.join("\n", lines.subList(start, Math.min(start + 29, lines.size()))));
                area.setEditable(false);
                area.setFont(new Font("Monospaced", Font.PLAIN, 16));
                area.setBorder(BorderFactory.createEmptyBorder(12, 15, 12, 15));
                pages.addTab("Page " + (start / 29 + 1), new JScrollPane(area));
            }
            frame.add(pages, BorderLayout.CENTER);
            frame.setSize(1180, 760);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
