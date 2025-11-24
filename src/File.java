public class File extends FSNode {
    private String content;

    public File(String name, Directory parent) {
        super(name, parent);
        this.content = "";
    }

    public String content() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public String getType() {
        return "FILE";
    }
}
