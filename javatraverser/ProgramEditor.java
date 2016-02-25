// package jTraverser;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import javax.swing.JPanel;

public class ProgramEditor extends JPanel implements Editor{
    private static final long serialVersionUID = -7840137374103863094L;
    ProgramData               program;
    LabeledExprEditor         program_edit, timeout_edit;

    public ProgramEditor(){
        this(null);
    }

    public ProgramEditor(final ProgramData program){
        this.program = program;
        if(this.program == null){
            this.program = new ProgramData(null, null);
        }
        this.program_edit = new LabeledExprEditor("Program", new ExprEditor(this.program.getProgram(), true));
        this.timeout_edit = new LabeledExprEditor("Timeout", new ExprEditor(this.program.getTimeout(), false));
        final JPanel jp = new JPanel();
        jp.setLayout(new GridLayout(2, 1));
        jp.add(this.program_edit);
        jp.add(this.timeout_edit);
        this.setLayout(new BorderLayout());
        this.add(jp, BorderLayout.NORTH);
    }

    @Override
    public Data getData() {
        return new ProgramData(this.timeout_edit.getData(), this.program_edit.getData());
    }

    @Override
    public void reset() {
        this.program_edit.reset();
        this.timeout_edit.reset();
    }

    public void setData(final Data data) {
        this.program = (ProgramData)data;
        if(this.program == null){
            this.program = new ProgramData(null, null);
        }
        this.reset();
    }

    @Override
    public void setEditable(final boolean editable) {
        if(this.program_edit != null) this.program_edit.setEditable(editable);
        if(this.timeout_edit != null) this.timeout_edit.setEditable(editable);
    }
}