// package jTraverser;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JComboBox;
import javax.swing.JPanel;

public class DispatchEditor extends JPanel implements ActionListener, Editor{
    class DispatchEdt extends JPanel{
        /**
         *
         */
        private static final long serialVersionUID = -6076590353205086521L;
        DispatchData              data;
        LabeledExprEditor         ident_edit, phase_edit, sequence_edit, completion_edit;
        boolean                   is_sequential    = false;

        public DispatchEdt(final DispatchData data, final boolean is_conditional){
            this.data = data;
            if(this.data == null){
                if(is_conditional) this.data = new DispatchData(DispatchData.SCHED_COND, null, null, null, null);
                else this.data = new DispatchData(DispatchData.SCHED_SEQ, null, null, null, null);
            }
            if(this.data.getType() == DispatchData.SCHED_SEQ) this.is_sequential = true;
            this.ident_edit = new LabeledExprEditor("Ident", new ExprEditor(this.data.getIdent(), true));
            this.phase_edit = new LabeledExprEditor("Phase", new ExprEditor(this.data.getPhase(), true));
            this.ident_edit = new LabeledExprEditor("Ident", new ExprEditor(this.data.getIdent(), true));
            if(this.is_sequential) this.sequence_edit = new LabeledExprEditor("Sequence", new ExprEditor(this.data.getWhen(), false));
            else this.sequence_edit = new LabeledExprEditor("After", new ExprEditor(this.data.getWhen(), false));
            this.completion_edit = new LabeledExprEditor("Completion", new ExprEditor(this.data.getCompletion(), true));
            final JPanel jp = new JPanel();
            jp.setLayout(new GridLayout(4, 1));
            jp.add(this.ident_edit);
            jp.add(this.phase_edit);
            jp.add(this.sequence_edit);
            jp.add(this.completion_edit);
            this.setLayout(new BorderLayout());
            this.add(jp, BorderLayout.NORTH);
        }

        public Data getData() {
            return new DispatchData(this.is_sequential ? DispatchData.SCHED_SEQ : DispatchData.SCHED_COND, this.ident_edit.getData(), this.phase_edit.getData(), this.sequence_edit.getData(), this.completion_edit.getData());
        }

        public void reset() {
            DispatchEditor.this.combo.setSelectedIndex(DispatchEditor.this.dtype_idx);
            this.ident_edit.reset();
            this.phase_edit.reset();
            this.sequence_edit.reset();
            this.completion_edit.reset();
        }

        public void setEditable(final boolean editable) {
            if(this.ident_edit != null) this.ident_edit.setEditable(editable);
            if(this.phase_edit != null) this.phase_edit.setEditable(editable);
            if(this.sequence_edit != null) this.sequence_edit.setEditable(editable);
            if(this.completion_edit != null) this.completion_edit.setEditable(editable);
        }
    }
    private static final long serialVersionUID = -9187606764049896331L;
    JComboBox                 combo;
    Data                      data;
    TreeDialog                dialog;
    DispatchEdt               dispatch_edit;
    int                       dtype_idx, curr_dtype_idx;
    boolean                   editable         = true;
    LabeledExprEditor         expr_edit;

    @SuppressWarnings("unchecked")
    public DispatchEditor(final Data data, final TreeDialog dialog){
        this.dialog = dialog;
        this.data = data;
        if(data == null) this.dtype_idx = 0;
        else if(data.dtype == Data.DTYPE_DISPATCH){
            final DispatchData ddata = (DispatchData)data;
            if(ddata.getType() == DispatchData.SCHED_SEQ) this.dtype_idx = 1;
            else this.dtype_idx = 2;
        }else this.dtype_idx = 3;
        this.curr_dtype_idx = this.dtype_idx;
        final String[] names = {"Undefined", "Sequential", "Conditional", "Expression"};
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
            case 2:
                this.remove(this.dispatch_edit);
                break;
            case 3:
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
                if(this.dtype_idx == this.curr_dtype_idx) this.dispatch_edit = new DispatchEdt((DispatchData)this.data, false);
                else this.dispatch_edit = new DispatchEdt(null, false);
                this.add(this.dispatch_edit);
                break;
            case 2:
                if(this.dtype_idx == this.curr_dtype_idx) this.dispatch_edit = new DispatchEdt((DispatchData)this.data, true);
                else this.dispatch_edit = new DispatchEdt(null, true);
                this.add(this.dispatch_edit);
                break;
            case 3:
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
            case 2:
                return this.dispatch_edit.getData();
            case 3:
                return this.expr_edit.getData();
        }
        return null;
    }

    @Override
    public void reset() {
        switch(this.curr_dtype_idx){
            case 1:
            case 2:
                this.remove(this.dispatch_edit);
                break;
            case 3:
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
        else if(data.dtype == Data.DTYPE_DISPATCH){
            final DispatchData ddata = (DispatchData)data;
            if(ddata.getType() == DispatchData.SCHED_SEQ) this.dtype_idx = 1;
            else this.dtype_idx = 2;
        }else this.dtype_idx = 3;
        this.reset();
    }

    @Override
    public void setEditable(final boolean editable) {
        this.editable = editable;
        if(this.dispatch_edit != null) this.dispatch_edit.setEditable(editable);
        if(this.expr_edit != null) this.expr_edit.setEditable(editable);
    }
}