import java.util.ArrayList;
import java.util.List;

public class FSDirectory extends FSNode {
    private List<FSNode> children;

    public FSDirectory(String name, FSDirectory parent) {
        super(name, parent);
        this.children = new ArrayList<>();
    }

    public void addChild(FSNode node) {
        children.add(node);
    }

    public void removeChild(FSNode node) {
        children.remove(node);
    }

    public List<FSNode> children() {
        return children;
    }

    public FSNode getChildByName(String name) {
        for (FSNode node : children) {
            if (node.name().equals(name)) {
                return node;
            }
        }
        return null;
    }

    @Override
    public String getType() {
        return "DIR";
    }
}
