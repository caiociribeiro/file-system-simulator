public class FSFile extends FSNode {
    private String content;
    private String extension;

    public FSFile(String name, String extension, FSDirectory parent) {
        super(name, parent);
        this.extension = extension;
        this.content = "";
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    @Override
    public String getType() {
        return "FILE";
    }
}
