import java.io.Serializable;
import java.util.Date;

public abstract class FSNode implements Serializable {
    protected String name;
    protected Directory parent;
    protected Date creationDate;

    public FSNode(String name, Directory parent) {
        this.name = name;
        this.parent = parent;
        this.creationDate = new Date();
    }

    public String name() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Directory parent() {
        return this.parent;
    }

    public void setParent(Directory parent) {
        this.parent = parent;
    }

    public Date creationDate() {
        return creationDate;
    }

    public abstract String getType();

}
