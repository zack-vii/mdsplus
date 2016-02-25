// package jTraverser;
import java.awt.Component;

public class NodeDisplayNci extends NodeDataPropertyEditor{
    @Override
    public Component getCustomEditor() {
        final NodeEditor ne = new DisplayNci();
        return ne;
    }
}