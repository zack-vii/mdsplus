// package jTraverser;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.beans.PropertyChangeListener;
import java.beans.PropertyEditor;

public abstract class NodeDataPropertyEditor implements PropertyEditor{
    protected Data data;

    // event notification not used here
    @Override
    public void addPropertyChangeListener(final PropertyChangeListener l) {}

    @Override
    public String getAsText() {
        try{
            return Tree.dataToString(this.data);
        }catch(final Exception e){
            return null;
        }
    }

    @Override
    public abstract Component getCustomEditor(); // to be subclassed

    @Override
    public String getJavaInitializationString() {
        return null;
    }

    @Override
    public String[] getTags() {
        return null;
    }

    @Override
    public Object getValue() {
        return this.data;
    }

    @Override
    public boolean isPaintable() {
        return false;
    }

    @Override
    public void paintValue(final Graphics g, final Rectangle r) {}

    @Override
    public void removePropertyChangeListener(final PropertyChangeListener l) {}

    @Override
    public void setAsText(final String s) {
        try{
            this.data = Tree.dataFromExpr(s);
        }catch(final Exception e){
            this.data = null;
        }
    }

    @Override
    public void setValue(final Object o) {
        this.data = (Data)o;
    }

    @Override
    public boolean supportsCustomEditor() {
        return true;
    }
}