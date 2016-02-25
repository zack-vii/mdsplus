// package jTraverser;
import javax.swing.JPanel;

public class NodeEditor extends JPanel{
    private static final long serialVersionUID = -621239038738815183L;
    TreeDialog                frame;
    Node                      node;
    Tree                      tree;

    public void setFrame(final TreeDialog frame) {
        this.frame = frame;
    }

    public void setNode(final Node node) {
        this.node = node;
    }

    public void setTree(final Tree tree) {
        this.tree = tree;
    }
}