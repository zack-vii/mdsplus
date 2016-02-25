// package jTraverser;
import java.awt.Component;

public class NodeModifyData extends NodeDataPropertyEditor{
    @Override
    public Component getCustomEditor() {
        return new ModifyData();
    }
}