import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

public abstract class FSNode implements Serializable {
    protected String name;
    protected FSDirectory parent;
    protected Date createdAt;
    protected Date updatedAt;

    private static final String DATE_PATTERN = "dd-MM-yyyy hh:mm a";

    public FSNode(String name, FSDirectory parent) {
        this.name = name;
        this.parent = parent;
        this.createdAt = new Date();
        this.updatedAt = this.createdAt;
    }

    public String name() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public FSDirectory parent() {
        return this.parent;
    }

    public void setParent(FSDirectory parent) {
        this.parent = parent;
    }

    public void updated() {
        updatedAt = new Date();
    }

    public String createdAt() {
        SimpleDateFormat formatter = new SimpleDateFormat(DATE_PATTERN);
        return formatter.format(this.createdAt);
    }

    public String updatedAt() {
        SimpleDateFormat formatter = new SimpleDateFormat(DATE_PATTERN);
        return formatter.format(this.updatedAt);
    }

    public abstract String getType();
}
