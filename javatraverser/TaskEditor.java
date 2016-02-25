// package jTraverser;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JComboBox;
import javax.swing.JPanel;

public class TaskEditor extends JPanel implements ActionListener, Editor{
    private static final long serialVersionUID = -5397713086983571561L;
    JComboBox                 combo;
    Data                      data;
    TreeDialog                dialog;
    int                       dtype_idx, curr_dtype_idx;
    boolean                   editable         = true;
    LabeledExprEditor         expr_edit;
    MethodEditor              method_edit;
    ProcedureEditor           procedure_edit;
    ProgramEditor             program_edit;
    RoutineEditor             routine_edit;

    @SuppressWarnings("unchecked")
    public TaskEditor(final Data data, final TreeDialog dialog){
        this.dialog = dialog;
        this.data = data;
        if(data == null) this.dtype_idx = 0;
        else{
            switch(data.dtype){
                case Data.DTYPE_METHOD:
                    this.dtype_idx = 1;
                    break;
                case Data.DTYPE_ROUTINE:
                    this.dtype_idx = 2;
                    break;
                case Data.DTYPE_PROCEDURE:
                    this.dtype_idx = 3;
                    break;
                case Data.DTYPE_PROGRAM:
                    this.dtype_idx = 4;
                    break;
                default:
                    this.dtype_idx = 5;
            }
        }
        this.curr_dtype_idx = this.dtype_idx;
        final String names[] = {"Undefined", "Method", "Routine", "Procedure", "Program", "Expression"};
        this.combo = new JComboBox(names);
        this.combo.setEditable(false);
        this.combo.setSelectedIndex(this.dtype_idx);
        this.combo.addActionListener(this);
        this.setLayout(new BorderLayout());
        final JPanel jp = new JPanel();
        jp.add(this.combo);
        this.add(jp, BorderLayout.PAGE_START);
        this.addEditor();
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        if(!this.editable){
            this.combo.setSelectedIndex(this.curr_dtype_idx);
            return;
        }
        final int idx = this.combo.getSelectedIndex();
        if(idx == this.curr_dtype_idx) return;
        switch(this.curr_dtype_idx){
            case 1:
                this.remove(this.method_edit);
                break;
            case 2:
                this.remove(this.routine_edit);
                break;
            case 3:
                this.remove(this.procedure_edit);
                break;
            case 4:
                this.remove(this.program_edit);
                break;
            case 5:
                this.remove(this.expr_edit);
                break;
        }
        this.curr_dtype_idx = idx;
        this.addEditor();
        this.validate();
        this.dialog.repack();
        this.repaint();
    }

    private void addEditor() {
        switch(this.curr_dtype_idx){
            case 0:
                return;
            case 1:
                if(this.dtype_idx == this.curr_dtype_idx) this.method_edit = new MethodEditor((MethodData)this.data);
                else this.method_edit = new MethodEditor(null);
                this.add(this.method_edit);
                break;
            case 2:
                if(this.dtype_idx == this.curr_dtype_idx) this.routine_edit = new RoutineEditor((RoutineData)this.data);
                else this.routine_edit = new RoutineEditor(null);
                this.add(this.routine_edit);
                break;
            case 3:
                if(this.dtype_idx == this.curr_dtype_idx) this.procedure_edit = new ProcedureEditor((ProcedureData)this.data);
                else this.procedure_edit = new ProcedureEditor(null);
                this.add(this.procedure_edit);
                break;
            case 4:
                if(this.dtype_idx == this.curr_dtype_idx) this.program_edit = new ProgramEditor((ProgramData)this.data);
                else this.program_edit = new ProgramEditor(null);
                this.add(this.program_edit);
                break;
            case 5:
                this.expr_edit = new LabeledExprEditor(this.data);
                this.add(this.expr_edit);
                break;
        }
    }

    @Override
    public Data getData() {
        switch(this.curr_dtype_idx){
            case 0:
                return null;
            case 1:
                return this.method_edit.getData();
            case 2:
                return this.routine_edit.getData();
            case 3:
                return this.procedure_edit.getData();
            case 4:
                return this.program_edit.getData();
            case 5:
                return this.expr_edit.getData();
        }
        return null;
    }

    @Override
    public void reset() {
        switch(this.curr_dtype_idx){
            case 1:
                this.remove(this.method_edit);
                break;
            case 2:
                this.remove(this.routine_edit);
                break;
            case 3:
                this.remove(this.procedure_edit);
                break;
            case 4:
                this.remove(this.program_edit);
                break;
            case 5:
                this.remove(this.expr_edit);
                break;
        }
        this.curr_dtype_idx = this.dtype_idx;
        this.addEditor();
        this.validate();
        this.repaint();
    }

    public void setData(final Data data) {
        this.data = data;
        if(data == null) this.dtype_idx = 0;
        else{
            switch(data.dtype){
                case Data.DTYPE_METHOD:
                    this.dtype_idx = 1;
                    break;
                case Data.DTYPE_ROUTINE:
                    this.dtype_idx = 2;
                    break;
                case Data.DTYPE_PROCEDURE:
                    this.dtype_idx = 3;
                    break;
                case Data.DTYPE_PROGRAM:
                    this.dtype_idx = 4;
                    break;
                default:
                    this.dtype_idx = 5;
            }
        }
        this.reset();
    }

    @Override
    public void setEditable(final boolean editable) {
        this.editable = editable;
        if(this.expr_edit != null) this.expr_edit.setEditable(editable);
        if(this.method_edit != null) this.method_edit.setEditable(editable);
        if(this.routine_edit != null) this.routine_edit.setEditable(editable);
        if(this.program_edit != null) this.program_edit.setEditable(editable);
        if(this.procedure_edit != null) this.procedure_edit.setEditable(editable);
    }
}