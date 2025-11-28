import java.util.Scanner;

public class Shell {

    static void clear() {
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

    public static void main(String[] args) throws FSException {
        FileSystemSimulator fs = new FileSystemSimulator();
        Scanner in = new Scanner(System.in);
        boolean running = true;

        clear();

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

            try {
                switch (command) {
                    case "clear":
                        clear();
                        break;
                    case "exit":
                        running = false;
                        fs.shutdown();
                        System.out.println("Shutting down.");
                        break;

                    case "ls":
                        if (arg1 != null && arg2 == null) {
                            System.out.println(fs.listDirectory(arg1));
                        } else if (arg1 == null) {
                            System.out.println(fs.listDirectory());
                        } else {
                            System.out.println("Use: ls [path]");
                        }
                        break;

                    case "mkdir":
                        if (arg1 != null && arg2 == null) {
                            fs.createDirectory(arg1);
                        } else {
                            System.out.println("Use: mkdir <path>");
                        }
                        break;

                    case "touch":
                        if (arg1 != null && arg2 == null) {
                            fs.createFile(arg1);
                        } else {
                            System.out.println("Use: touch <path>");
                        }
                        break;

                    case "cd":
                        if (arg1 != null && arg2 == null) {
                            fs.changeDirectory(arg1);
                        } else {
                            System.out.println("Use: 'cd <path>'");
                        }
                        break;

                    case "rm":
                        if (arg1 != null && arg2 == null) {
                            fs.delete(arg1);
                        } else {
                            System.out.println("Use: 'rm <path>'");
                        }
                        break;

                    case "mv":
                        if (arg1 != null && arg2 != null) {
                            fs.rename(arg1, arg2);
                        } else {
                            System.out.println("Use: mv <oldname> <newname>");
                        }
                        break;

                    case "cp":
                        if (arg1 != null && arg2 != null) {
                            fs.copy(arg1, arg2);
                        } else {
                            System.out.println("Use: cp <src> <dst>");
                        }
                        break;

                    case "help":
                        System.out.println("Commands:");
                        System.out.println("ls <path>      - List directory");
                        System.out.println("mkdir <path>   - Create directory");
                        System.out.println("touch <path>   - Create file");
                        System.out.println("cd <path>      - Change directory");
                        System.out.println("rm <path>      - Remove file or directory");
                        System.out.println("mv <old> <new> - Rename file or directory");
                        System.out.println("cp <src> <dst> - Copy file");
                        System.out.println("clear          - Clear screen");
                        System.out.println("exit           - Exit shell");
                        break;

                    default:
                        System.out.printf("'%s' is not a valid command.", command);
                        System.out.println();
                        break;
                }
            } catch (FSException e) {
                System.out.println(Color.RED + "Error: " + e.getMessage() + Color.RESET);
            }

        }

        in.close();
    }
}
