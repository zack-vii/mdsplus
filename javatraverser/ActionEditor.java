// package jTraverser;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JComboBox;
import javax.swing.JPanel;

public class ActionEditor extends JPanel implements ActionListener, Editor{
    private static final long serialVersionUID = 5505321760234697543L;
    JPanel                    action_panel;
    JComboBox                 combo;
    Data                      data;
    TreeDialog                dialog;
    DispatchEditor            dispatch_edit;
    boolean                   editable;
    int                       mode_idx, curr_mode_idx;
    LabeledExprEditor         notify_edit, expr_edit;
    TaskEditor                task_edit;

    @SuppressWarnings("unchecked")
    public ActionEditor(final Data data, final TreeDialog dialog){
        this.dialog = dialog;
        this.data = data;
        if(data == null) this.mode_idx = 0;
        else{
            if(data.dtype == Data.DTYPE_ACTION) this.mode_idx = 1;
            else this.mode_idx = 2;
        }
        if(data == null) this.data = new ActionData(null, null, null, null, null);
        this.curr_mode_idx = this.mode_idx;
        final String names[] = {"Undefined", "Action", "Expression"};
        this.combo = new JComboBox(names);
        this.combo.setEditable(false);
        this.combo.setSelectedIndex(this.mode_idx);
        this.combo.addActionListener(this);
        this.setLayout(new BorderLayout());
        final JPanel jp = new JPanel();
        jp.add(this.combo);
        this.add(jp, BorderLayout.NORTH);
        this.addEditor();
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        if(!this.editable){
            this.combo.setSelectedIndex(this.curr_mode_idx);
            return;
        }
        final int idx = this.combo.getSelectedIndex();
        if(idx == this.curr_mode_idx) return;
        switch(this.curr_mode_idx){
            case 1:
                this.remove(this.action_panel);
                this.action_panel = null;
                this.task_edit = null;
                this.dispatch_edit = null;
                this.remove(this.notify_edit);
                this.notify_edit = null;
                break;
            case 2:
                this.remove(this.expr_edit);
                this.expr_edit = null;
                break;
        }
        this.curr_mode_idx = idx;
        this.addEditor();
        this.validate();
        this.dialog.repack();
    }

    private void addEditor() {
        switch(this.curr_mode_idx){
            case 0:
                return;
            case 1:
                if(this.curr_mode_idx == this.mode_idx){
                    this.dispatch_edit = new DispatchEditor(((ActionData)this.data).getDispatch(), this.dialog);
                    this.task_edit = new TaskEditor(((ActionData)this.data).getTask(), this.dialog);
                    this.notify_edit = new LabeledExprEditor("Notify", new ExprEditor(((ActionData)this.data).getErrorlogs(), true));
                }else{
                    this.dispatch_edit = new DispatchEditor(null, this.dialog);
                    this.task_edit = new TaskEditor(null, this.dialog);
                    this.notify_edit = new LabeledExprEditor("Notify", new ExprEditor(null, true));
                }
                this.action_panel = new JPanel();
                this.action_panel.setLayout(new GridLayout(1, 2));
                this.action_panel.add(this.dispatch_edit);
                this.action_panel.add(this.task_edit);
                this.add(this.action_panel, BorderLayout.CENTER);
                this.add(this.notify_edit, BorderLayout.SOUTH);
                break;
            case 2:
                this.expr_edit = new LabeledExprEditor(this.data);
                this.add(this.expr_edit, "Center");
                break;
        }
    }

    @Override
    public Data getData() {
        switch(this.curr_mode_idx){
            case 0:
                return null;
            case 1:
                return new ActionData(this.dispatch_edit.getData(), this.task_edit.getData(), null, null, null);
            case 2:
                return this.expr_edit.getData();
        }
        return null;
    }

    @Override
    public void reset() {
        this.combo.setSelectedIndex(this.mode_idx);
        switch(this.curr_mode_idx){
            case 1:
                this.remove(this.action_panel);
                this.remove(this.notify_edit);
                break;
            case 2:
                this.remove(this.expr_edit);
                break;
        }
        this.curr_mode_idx = this.mode_idx;
        this.addEditor();
        this.validate();
        this.repaint();
    }

    public void setData(final Data data) {
        this.data = data;
        if(data == null) this.mode_idx = 0;
        else{
            if(data.dtype == Data.DTYPE_ACTION) this.mode_idx = 1;
            else this.mode_idx = 2;
        }
        if(data == null) this.data = new ActionData(null, null, null, null, null);
        this.reset();
    }

    @Override
    public void setEditable(final boolean editable) {
        this.editable = editable;
        if(this.task_edit != null) this.task_edit.setEditable(editable);
        if(this.dispatch_edit != null) this.dispatch_edit.setEditable(editable);
        if(this.expr_edit != null) this.expr_edit.setEditable(editable);
        if(this.notify_edit != null) this.notify_edit.setEditable(editable);
    }
}