import java.util.Scanner;

public class Shell {

    static void clearConsole() {
        try {
            System.out.print(new String(new char[50]).replace("\0", "\n")); // Fallback
            if (System.getProperty("os.name").contains("Windows")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                System.out.print("\033[H\033[2J");
                System.out.flush();
            }
        } catch (final Exception e) {
            for (int i = 0; i < 50; ++i)
                System.out.println();
        }
    }

    public static void main(String[] args) {
        FileSystemSimulator fs = new FileSystemSimulator();
        Scanner in = new Scanner(System.in);
        boolean running = true;

        clearConsole();

        System.out.println("System started. Type 'help' to see commands.");

        while (running) {
            System.out.print(Color.CYAN + "root@FileSystemSimulator: " + Color.RESET);
            System.out.println(Color.YELLOW + fs.currentPath() + Color.RESET);
            System.out.print("$ ");

            String input = in.nextLine().trim();
            if (input.isEmpty())
                continue;

            String[] parts = input.split("\\s+");
            String command = parts[0];
            String arg1 = parts.length > 1 ? parts[1] : null;
            String arg2 = parts.length > 2 ? parts[2] : null;

            switch (command) {
                case "clear":
                    clearConsole();
                    break;
                case "exit":
                    running = false;
                    fs.shutdown();
                    System.out.println("Shutting down.");
                    break;

                case "ls":
                    System.out.println(fs.listDirectory());
                    break;

                case "mkdir":
                    if (arg1 != null) {
                        String res = fs.createDirectory(arg1);
                        System.out.println(res == null ? "" : res);
                    } else {
                        System.out.println("Use: mkdir <name>");
                    }
                    break;

                case "touch":
                    if (arg1 != null) {
                        String res = fs.createFile(arg1);
                        System.out.println(res == null ? "" : res);
                    } else {
                        System.out.println("Use: mkdir <name>");
                    }
                    break;

                case "cd":
                    if (arg1 != null) {
                        String res = fs.changeDirectory(arg1);
                        System.out.println(res == null ? "" : res);
                    } else {
                        System.out.println("Use: 'cd <name>' or 'cd ..'");
                    }
                    break;

                case "rm":
                    // TODO
                    break;

                case "mv":
                    // TODO
                    break;

                default:
                    System.out.printf("'%s' is not a valid command.", command);
                    System.out.println();
                    break;
            }
        }

        in.close();
    }
}
