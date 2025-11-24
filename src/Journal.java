import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Journal {
    private static final String LOG_FILE = "journal.log";
    private PrintWriter writer;

    public Journal() {
        try {
            FileWriter fw = new FileWriter(LOG_FILE, true);
            writer = new PrintWriter(fw, true);

        } catch (IOException e) {
            System.err.println("Error: Couldn't initiate journaling.");
            e.printStackTrace();
        }
    }

    public void log(String operation) {
        if (writer != null) {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            writer.println(String.format("[%s] %s", timestamp, operation));
        }
    }

    public void close() {
        if (writer != null) {
            writer.close();
        }
    }
}
