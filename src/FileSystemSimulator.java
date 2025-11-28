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
        ParentResolve pr = resolveParent(path, true);
        FSDirectory parent = pr.parent;
        String name = pr.name;

        if (parent.getChildByName(name) != null)
            throw new FSException(String.format("File or directory '%s' already exists.", name));

        createDirectory(name, parent);
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
        ParentResolve pr = resolveParent(path, true);
        FSDirectory parent = pr.parent;
        String name = pr.name;

        if (parent.getChildByName(name) != null)
            throw new FSException(String.format("File or directory '%s' already exists.", name));

        createFile(name, parent);
    }

    private void createFile(String name, FSDirectory currentDir) {
        journal.log("START: touch " + name);
        String extension = getFileExtension(name);
        FSFile newFile = new FSFile(name, extension, currentDir);
        currentDir.addChild(newFile);
        saveFileSystem();
        journal.log("COMMIT: touch " + name);
    }

    private static class ParentResolve {
        FSDirectory parent;
        String name;

        ParentResolve(FSDirectory parent, String name) {
            this.parent = parent;
            this.name = name;
        }
    }

    private ParentResolve resolveParent(String path, boolean createIfMissing) throws FSException {
        if (path == null || path.isEmpty())
            throw new FSException("Invalid path.");

        int lastSlash = path.lastIndexOf('/');
        String parentPath = null;
        String name = path;

        if (lastSlash != -1) {
            parentPath = path.substring(0, lastSlash);
            name = path.substring(lastSlash + 1);
        }

        if (!isValidName(name))
            throw new FSException(String.format("'%s' is not a valid name.", name));

        FSDirectory parent;

        if (parentPath == null || parentPath.isEmpty()) {
            parent = path.startsWith("/") ? root : currentDirectory;
        } else {
            if (createIfMissing) {
                String[] parts = parentPath.split("/");
                FSDirectory node = parentPath.startsWith("/") ? root : currentDirectory;
                for (String part : parts) {
                    if (part == null || part.isEmpty() || part.equals("."))
                        continue;
                    if (part.equals("..")) {
                        if (node.parent() != null)
                            node = node.parent();
                        continue;
                    }

                    FSNode child = node.getChildByName(part);
                    if (child == null) {
                        node = createDirectory(part, node);
                        continue;
                    }

                    if (child.getType().equals("FILE"))
                        throw new FSException(String.format("'%s' is not a directory.", child.name()));

                    node = (FSDirectory) child;
                }
                parent = node;
            } else {
                FSNode p = resolvePathToNode(parentPath);
                if (p == null || p.getType().equals("FILE"))
                    throw new FSException(String.format("Destination path '%s' not found.", parentPath));
                parent = (FSDirectory) p;
            }
        }

        return new ParentResolve(parent, name);
    }

    public String listDirectory() {
        return listDirectory(currentDirectory);
    }

    public String listDirectory(String path) {
        if (path == null || path.isEmpty()) {
            return listDirectory();
        }

        try {
            FSNode node = resolvePathToNode(path);

            if (node.getType().equals("FILE")) {
                StringBuilder s = new StringBuilder();
                s.append("\nFile: ").append(getPath(node)).append("\n\n");
                s.append(Color.GREEN).append("Type   LastWriteTime         Name\n").append(Color.RESET);
                s.append(Color.GREEN).append("----   -------------------   ----\n").append(Color.RESET);
                String format = "%-4s   %-19s   %s\n";
                s.append(String.format(format, node.getType(), node.updatedAt(), node.name()));
                return s.toString();
            }

            return listDirectory((FSDirectory) node);
        } catch (FSException e) {
            return String.format("Path '%s' not found.", path);
        }
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

    public void changeDirectory(String path) throws FSException {
        FSNode node = resolvePathToNode(path);

        if (node.getType().equals("FILE"))
            throw new FSException(String.format("'%s' is not a directory.", node.name()));

        changeDirectory((FSDirectory) node);
    }

    private void changeDirectory(FSDirectory dir) {
        currentDirectory = dir;
    }

    public void rename(String oldName, String newName) throws FSException {
        FSNode src = resolvePathToNode(oldName);

        if (src == null)
            throw new FSException(String.format("File or directory '%s' not found.", oldName));

        try {
            FSNode destNode = resolvePathToNode(newName);
            if (destNode.getType().equals("DIR")) {
                FSDirectory targetDir = (FSDirectory) destNode;
                if (targetDir.getChildByName(src.name()) != null)
                    throw new FSException(
                            String.format("File or directory '%s' already exists in destination.", src.name()));

                journal.log("START: mv " + oldName + " " + newName);
                src.parent().removeChild(src);
                src.setParent(targetDir);
                targetDir.addChild(src);
                saveFileSystem();
                journal.log("COMMIT: mv " + oldName + " " + newName);
                return;
            }
        } catch (FSException e) {
        }

        int lastSlash = newName.lastIndexOf('/');
        String parentPath = null;
        String destName = newName;
        FSDirectory parentDir = currentDirectory;

        if (lastSlash != -1) {
            parentPath = newName.substring(0, lastSlash);
            destName = newName.substring(lastSlash + 1);
        }

        if (!isValidName(destName))
            throw new FSException(String.format("'%s' is not a valid name.", destName));

        if (parentPath != null && !parentPath.isEmpty()) {
            FSNode p = resolvePathToNode(parentPath);
            if (p == null || p.getType().equals("FILE"))
                throw new FSException(String.format("Destination path '%s' not found.", parentPath));
            parentDir = (FSDirectory) p;
        } else if (newName.startsWith("/")) {
            parentDir = root;
        }

        if (parentDir.getChildByName(destName) != null)
            throw new FSException(String.format("File or directory '%s' already exists.", destName));

        journal.log("START: mv " + oldName + " " + newName);

        FSDirectory oldParent = src.parent();
        oldParent.removeChild(src);
        src.setParent(parentDir);
        src.setName(destName);
        parentDir.addChild(src);
        saveFileSystem();
        journal.log("COMMIT: mv " + oldName + " " + newName);
    }

    public void copy(String srcPath, String dstPath) throws FSException {
        FSNode src = resolvePathToNode(srcPath);
        if (src == null) {
            throw new FSException(String.format("Source '%s' not found.", srcPath));
        }

        try {
            FSNode destNode = resolvePathToNode(dstPath);
            if (destNode.getType().equals("DIR")) {
                FSDirectory targetDir = (FSDirectory) destNode;
                if (src.getType().equals("FILE")) {
                    if (targetDir.getChildByName(src.name()) != null)
                        throw new FSException(String.format("File '%s' already exists in destination.", src.name()));

                    journal.log("START: cp " + srcPath + " " + dstPath);
                    FSFile srcFile = (FSFile) src;
                    FSFile copy = new FSFile(srcFile.name(), srcFile.getExtension(), targetDir);
                    copy.setContent(srcFile.getContent());
                    targetDir.addChild(copy);
                    saveFileSystem();
                    journal.log("COMMIT: cp " + srcPath + " " + dstPath);
                    return;
                } else {
                    throw new FSException("Copying directories is not supported.");
                }
            }
        } catch (FSException e) {
        }

        int lastSlash = dstPath.lastIndexOf('/');
        String parentPath = null;
        String destName = dstPath;
        FSDirectory parentDir = currentDirectory;

        if (lastSlash != -1) {
            parentPath = dstPath.substring(0, lastSlash);
            destName = dstPath.substring(lastSlash + 1);
        }

        if (!isValidName(destName))
            throw new FSException(String.format("'%s' is not a valid name.", destName));

        if (parentPath != null && !parentPath.isEmpty()) {
            FSNode p = resolvePathToNode(parentPath);
            if (p == null || p.getType().equals("FILE"))
                throw new FSException(String.format("Destination path '%s' not found.", parentPath));
            parentDir = (FSDirectory) p;
        } else if (dstPath.startsWith("/")) {
            parentDir = root;
        }

        if (parentDir.getChildByName(destName) != null)
            throw new FSException(String.format("File or directory '%s' already exists.", destName));

        if (src.getType().equals("FILE")) {
            journal.log("START: cp " + srcPath + " " + dstPath);
            FSFile srcFile = (FSFile) src;
            FSFile copy = new FSFile(destName, srcFile.getExtension(), parentDir);
            copy.setContent(srcFile.getContent());
            parentDir.addChild(copy);
            saveFileSystem();
            journal.log("COMMIT: cp " + srcPath + " " + dstPath);
            return;
        }

        throw new FSException("Copying directories is not supported.");
    }

    private FSNode resolvePathToNode(String path) throws FSException {
        if (path == null || path.isEmpty())
            return null;

        if (path.equals("/"))
            return root;

        FSNode node = path.startsWith("/") ? root : currentDirectory;
        String[] parts = path.split("/");

        for (String part : parts) {
            if (part == null || part.isEmpty())
                continue;

            if (part.equals("."))
                continue;

            if (part.equals("..")) {
                if (node.parent() != null)
                    node = node.parent();
                continue;
            }

            if (!(node instanceof FSDirectory))
                throw new FSException(String.format("'%s' is not a directory.", node.name()));

            FSDirectory dir = (FSDirectory) node;
            FSNode child = dir.getChildByName(part);
            if (child == null)
                throw new FSException(String.format("Path segment '%s' not found.", part));
            node = child;
        }

        return node;
    }

    public void delete(String path) throws FSException {
        FSNode node = resolvePathToNode(path);
        if (node == null)
            throw new FSException(String.format("Couldn't delete '%s'. Wrong path.", path));

        FSDirectory parent = node.parent();
        if (parent == null)
            throw new FSException(String.format("Couldn't delete '%s'. No parent directory.", path));

        delete(parent, node, path);

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