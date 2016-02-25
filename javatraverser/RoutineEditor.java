// package jTraverser;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import javax.swing.JPanel;

public class RoutineEditor extends JPanel implements Editor{
    private static final long serialVersionUID = -7442386781619109098L;
    ArgEditor                 arg_edit;
    LabeledExprEditor         image_edit, routine_edit, timeout_edit;
    RoutineData               routine;

    public RoutineEditor(){
        this(null);
    }

    public RoutineEditor(final RoutineData routine){
        this.routine = routine;
        if(this.routine == null){
            this.routine = new RoutineData(null, null, null, new Data[0]);
        }
        this.setLayout(new BorderLayout());
        final JPanel jp = new JPanel();
        final GridLayout gl = new GridLayout(2, 1);
        gl.setVgap(0);
        jp.setLayout(gl);
        this.image_edit = new LabeledExprEditor("Image", new ExprEditor(this.routine.getImage(), true));
        this.routine_edit = new LabeledExprEditor("Routine", new ExprEditor(this.routine.getRoutine(), true));
        jp.add(this.image_edit);
        jp.add(this.routine_edit);
        this.add(jp, "North");
        this.arg_edit = new ArgEditor(this.routine.getArguments());
        this.add(this.arg_edit, "Center");
        this.timeout_edit = new LabeledExprEditor("Timeout", new ExprEditor(this.routine.getTimeout(), false));
        this.add(this.timeout_edit, "South");
    }

    @Override
    public Data getData() {
        return new RoutineData(this.timeout_edit.getData(), this.image_edit.getData(), this.routine_edit.getData(), this.arg_edit.getData());
    }

    @Override
    public void reset() {
        this.image_edit.reset();
        this.routine_edit.reset();
        this.arg_edit.reset();
        this.timeout_edit.reset();
    }

    public void setData(final Data data) {
        this.routine = (RoutineData)data;
        if(this.routine == null){
            this.routine = new RoutineData(null, null, null, new Data[0]);
        }
        this.reset();
    }

    @Override
    public void setEditable(final boolean editable) {
        if(this.image_edit != null) this.image_edit.setEditable(editable);
        if(this.routine_edit != null) this.routine_edit.setEditable(editable);
        if(this.timeout_edit != null) this.timeout_edit.setEditable(editable);
        if(this.arg_edit != null) this.arg_edit.setEditable(editable);
    }
}