package Chess;

import java.util.LinkedList;

public class Tree<T> {
    private Node root;
    private LinkedList<Node> children;

    public Node getRoot() {
        return root;
    }
    public void setRoot(final Node node) {
        root = node;
    }
    public int size() {
        return children.size();
    }
    public boolean add(final Node node) {
        return children.add(node);
    }
    public Node get(final int index) throws IndexOutOfBoundsException {
        return children.get(index);
    }
    public Node remove(final int index) throws IndexOutOfBoundsException {
        return children.remove(index);
    }
    public void clear() {
        children.clear();
    }
}
