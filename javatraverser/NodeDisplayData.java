// package jTraverser;
import java.awt.Component;

public class NodeDisplayData extends NodeDataPropertyEditor{
    @Override
    public Component getCustomEditor() {
        final NodeEditor ne = new DisplayData();
        return ne;
    }
}