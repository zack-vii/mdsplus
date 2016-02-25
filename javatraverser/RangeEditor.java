// package jTraverser;
import java.awt.GridLayout;
import javax.swing.JPanel;

public class RangeEditor extends JPanel implements Editor{
    private static final long serialVersionUID = 8930270158951086998L;
    LabeledExprEditor         begin_edit, end_edit, delta_edit;
    RangeData                 range;

    public RangeEditor(){
        this(null);
    }

    public RangeEditor(final RangeData range){
        this.range = range;
        if(this.range == null){
            this.range = new RangeData(null, null, null);
        }
        final GridLayout gl = new GridLayout(3, 1);
        gl.setVgap(0);
        this.setLayout(gl);
        this.begin_edit = new LabeledExprEditor("Start", new ExprEditor(this.range.getBegin(), false));
        this.add(this.begin_edit);
        this.end_edit = new LabeledExprEditor("End", new ExprEditor(this.range.getEnd(), false));
        this.add(this.end_edit);
        this.delta_edit = new LabeledExprEditor("Increment", new ExprEditor(this.range.getDelta(), false));
        this.add(this.delta_edit);
    }

    @Override
    public Data getData() {
        return new RangeData(this.begin_edit.getData(), this.end_edit.getData(), this.delta_edit.getData());
    }

    @Override
    public void reset() {
        this.begin_edit.reset();
        this.end_edit.reset();
        this.delta_edit.reset();
    }

    public void setData(final Data data) {
        this.range = (RangeData)data;
        if(this.range == null){
            this.range = new RangeData(null, null, null);
        }
        this.begin_edit.setData(this.range.getBegin());
        this.end_edit.setData(this.range.getEnd());
        this.delta_edit.setData(this.range.getDelta());
        this.reset();
    }

    @Override
    public void setEditable(final boolean editable) {
        if(this.begin_edit != null) this.begin_edit.setEditable(editable);
        if(this.end_edit != null) this.end_edit.setEditable(editable);
        if(this.delta_edit != null) this.delta_edit.setEditable(editable);
    }
}