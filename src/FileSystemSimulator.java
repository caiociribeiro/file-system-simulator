import java.io.*;
import java.util.List;

public class FileSystemSimulator {
    private FSDirectory root;
    private FSDirectory currentDirectory;
    private Journal journal;
    private final String DISK_IMAGE = "filesystem.dat";

    public FileSystemSimulator() {
        this.journal = new Journal();
        loadFileSystem();

        if (root == null) {
            journal.log("BOOT: No file system found. Formatting disk.");
            root = new FSDirectory("/", null);
            currentDirectory = root;
            saveFileSystem();
        } else {
            journal.log("BOOT: File System loaded successfully.");
        }
    }

    public void createDirectory(String path) throws FSException {
        String[] names = path.split("/");

        FSDirectory node = currentDirectory;

        for (int i = 0; i < names.length; i++) {
            String name = names[i];

            if (!isValidName(name))
                throw new FSException(String.format("'%s' is not a valid name.", name));

            FSNode child = node.getChildByName(name);

            if (child != null) {
                if (i == names.length - 1) {
                    throw new FSException(String.format("File or directory '%s' already exists.", child.name()));
                }

                if (child.getType() == "FILE") {
                    throw new FSException(String.format("'%s' is not a directory.", child.name()));
                }

                node = (FSDirectory) child;
                continue;
            }

            node = createDirectory(name, node);
        }
    }

    private FSDirectory createDirectory(String name, FSDirectory currentDir) {
        journal.log("START: mkdir " + name);
        FSDirectory newDir = new FSDirectory(name, currentDir);
        currentDir.addChild(newDir);
        saveFileSystem();
        journal.log("COMMIT: mkdir " + name);

        return newDir;
    }

    public void createFile(String path) throws FSException {
        String[] names = path.split("/");

        FSDirectory current = currentDirectory;

        for (int i = 0; i < names.length; i++) {
            String name = names[i];

            if (!isValidName(name))
                throw new FSException(String.format("'%s' is not a valid name.", name));

            FSNode child = current.getChildByName(name);

            if (child != null) {
                if (i == names.length - 1)
                    throw new FSException(String.format("File or directory '%s' already exists.", child.name()));

                if (child.getType() == "FILE") {
                    throw new FSException(String.format("'%s' is not a directory.", child.name()));
                }

                current = (FSDirectory) child;
                continue;
            }

            if (i < names.length - 1) {
                current = createDirectory(name, current);
                continue;
            }

            createFile(name, current);
        }
    }

    private void createFile(String name, FSDirectory currentDir) {
        journal.log("START: touch " + name);
        String extension = getFileExtension(name);
        FSFile newFile = new FSFile(name, extension, currentDir);
        currentDir.addChild(newFile);
        saveFileSystem();
        journal.log("COMMIT: touch " + name);
    }

    public String listDirectory() {
        return listDirectory(currentDirectory);
    }

    public String listDirectory(String path) {
        // TODO
        return null;
    }

    private String listDirectory(FSDirectory dir) {
        List<FSNode> content = dir.children();

        if (content.isEmpty())
            return "";

        StringBuilder s = new StringBuilder();
        s.append("\nDirectory: ").append(getPath(dir)).append("\n\n");
        s.append(Color.GREEN).append("Type   LastWriteTime         Name\n").append(Color.RESET);
        s.append(Color.GREEN).append("----   -------------------   ----\n").append(Color.RESET);

        for (FSNode node : content) {
            String format = "%-4s   %-19s   %s\n";
            s.append(String.format(format, node.getType(), node.updatedAt(), node.name()));
        }

        return s.toString();
    }

    public void changeDirectory(String name) throws FSException {
        if (name.equals("..")) {
            if (currentDirectory.parent() == null)
                return;
            currentDirectory = currentDirectory.parent();
            return;
        }

        FSNode node = currentDirectory.getChildByName(name);
        if (node == null)
            throw new FSException(String.format("File or directory '%s' not found.", name));
        if (node instanceof FSFile)
            throw new FSException(String.format("'%s' is not a directory.", name));

        currentDirectory = (FSDirectory) node;
    }

    public void rename(String oldName, String newName) throws FSException {
        FSNode node = currentDirectory.getChildByName(oldName);

        if (node == null)
            throw new FSException(String.format("File or directory '%s' not found.", oldName));

        if (currentDirectory.getChildByName(newName) != null)
            throw new FSException(String.format("File or directory '%s' already exists.", newName));

        journal.log("START: mv " + oldName + " " + newName);
        node.setName(newName);
        saveFileSystem();
        journal.log("COMMIT: mv " + oldName + " " + newName);
    }

    public void delete(String path) throws FSException {
        String[] names = path.split("/");

        FSDirectory parent = currentDirectory;
        FSNode child = null;

        for (int i = 0; i < names.length; i++) {
            child = parent.getChildByName(names[i]);

            if (child == null)
                throw new FSException(String.format("Couldn't delete in '%s'. Wrong path.", path));

            if (i < names.length - 1) {
                if (child.getType() == "FILE") {
                    throw new FSException(String.format("'%s' is not a directory.", child.name()));
                } else {
                    parent = (FSDirectory) child;
                }
            }
        }

        if (child == null)
            throw new FSException(String.format("Couldn't delete in '%s'. Wrong path.", path));

        delete(parent, child, path);

    }

    private void delete(FSDirectory parent, FSNode child, String path) {
        journal.log("START: rm " + path);
        parent.removeChild(child);
        saveFileSystem();
        journal.log("COMMIT: rm " + path);
    }

    public String currentPath() {
        return getPath(currentDirectory);
    }

    private String getPath(FSNode node) {
        if (node == root)
            return "/";

        StringBuilder path = new StringBuilder();
        FSNode current = node;
        while (current != null && current.parent() != null) {
            path.insert(0, "/" + current.name());
            current = current.parent();
        }
        return path.toString();
    }

    private void saveFileSystem() {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(DISK_IMAGE))) {
            out.writeObject(root);
        } catch (IOException e) {
            System.err.println("Error while saving in virtual disk." + e.getMessage());
            journal.log("CRITICAL_ERROR: Failed to save in disk.");
        }
    }

    private void loadFileSystem() {
        File disk = new File(DISK_IMAGE);
        if (disk.exists()) {
            try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(DISK_IMAGE))) {
                root = (FSDirectory) in.readObject();
                currentDirectory = root;
            } catch (Exception e) {
                System.err.println("Error while reading virtual disk." + e.getMessage());
            }
        }
    }

    private boolean isValidName(String name) {
        if (name == null || name.isEmpty() || name.equals(".") || name.equals("..")) {
            return false;
        }
        String prohibitedChars = "\\/:*?\"<>|&";
        for (char c : prohibitedChars.toCharArray()) {
            if (name.indexOf(c) != -1) {
                return false;
            }
        }
        return true;
    }

    private String getFileExtension(String name) {
        int lastDotIndex = name.lastIndexOf(".");
        if (lastDotIndex > 0 && lastDotIndex < name.length() - 1) {
            return name.substring(lastDotIndex + 1);
        }
        return null;
    }

    public void shutdown() {
        journal.close();
    }
}