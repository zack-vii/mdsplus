// package jTraverser;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import javax.swing.JPanel;

public class ProcedureEditor extends JPanel implements Editor{
    private static final long serialVersionUID = 7305707909433418426L;
    ArgEditor                 arg_edit;
    ProcedureData             procedure;
    LabeledExprEditor         procedure_edit, language_edit, timeout_edit;

    public ProcedureEditor(){
        this(null);
    }

    public ProcedureEditor(final ProcedureData procedure){
        this.procedure = procedure;
        if(this.procedure == null){
            this.procedure = new ProcedureData(null, null, null, new Data[0]);
        }
        this.setLayout(new BorderLayout());
        final JPanel jp = new JPanel();
        final GridLayout gl = new GridLayout(2, 1);
        gl.setVgap(0);
        jp.setLayout(gl);
        this.procedure_edit = new LabeledExprEditor("Procedure", new ExprEditor(this.procedure.getProcedure(), true));
        this.language_edit = new LabeledExprEditor("Language", new ExprEditor(this.procedure.getLanguage(), true));
        jp.add(this.procedure_edit);
        jp.add(this.language_edit);
        this.add(jp, "North");
        this.arg_edit = new ArgEditor(this.procedure.getArguments());
        this.add(this.arg_edit, "Center");
        this.timeout_edit = new LabeledExprEditor("Timeout", new ExprEditor(this.procedure.getTimeout(), false));
        this.add(this.timeout_edit, "South");
    }

    @Override
    public Data getData() {
        return new ProcedureData(this.timeout_edit.getData(), this.language_edit.getData(), this.procedure_edit.getData(), this.arg_edit.getData());
    }

    @Override
    public void reset() {
        this.procedure_edit.reset();
        this.language_edit.reset();
        this.arg_edit.reset();
        this.timeout_edit.reset();
    }

    public void setData(final Data data) {
        this.procedure = (ProcedureData)data;
        if(this.procedure == null){
            this.procedure = new ProcedureData(null, null, null, new Data[0]);
        }
        this.reset();
    }

    @Override
    public void setEditable(final boolean editable) {
        if(this.procedure_edit != null) this.procedure_edit.setEditable(editable);
        if(this.language_edit != null) this.language_edit.setEditable(editable);
        if(this.timeout_edit != null) this.timeout_edit.setEditable(editable);
        if(this.arg_edit != null) this.arg_edit.setEditable(editable);
    }
}