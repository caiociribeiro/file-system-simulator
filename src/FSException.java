public class FSException {

    public static String alreadyExists(String name) {
        return Color.RED + String.format("Error: %s already exists.", name) + Color.RESET;
    }

    public static String notFound(String name) {
        return Color.RED + String.format("Error: %s not found.", name) + Color.RESET;
    }

    public static String notADirectory(String name) {
        return Color.RED + String.format("Error: %s is not a directory.", name) + Color.RESET;
    }

    public static String invalidName(String name) {
        return Color.RED + String.format("Error: '%s' is not a valid name for a file or directory.", name)
                + Color.RESET;
    }
}
