// package jTraverser;
import java.awt.Component;

public class NodeDisplayTags extends NodeDataPropertyEditor{
    @Override
    public Component getCustomEditor() {
        final NodeEditor ne = new DisplayTags();
        return ne;
    }
}
