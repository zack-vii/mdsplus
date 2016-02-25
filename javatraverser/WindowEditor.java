// package jTraverser;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JComboBox;
import javax.swing.JPanel;

public class WindowEditor extends JPanel implements ActionListener, Editor{
    static class WindowEdt extends JPanel{
        private static final long serialVersionUID = -1087283057943746867L;
        LabeledExprEditor         startidx_edit, endidx_edit, value0_edit;
        WindowData                window;

        public WindowEdt(){
            this(null);
        }

        public WindowEdt(final WindowData window){
            this.window = window;
            if(this.window == null){
                this.window = new WindowData(null, null, null);
            }
            final GridLayout gl = new GridLayout(3, 1);
            gl.setVgap(0);
            this.setLayout(gl);
            this.startidx_edit = new LabeledExprEditor("Start Idx", new ExprEditor(this.window.getStartIdx(), false));
            this.add(this.startidx_edit);
            this.endidx_edit = new LabeledExprEditor("End Idx", new ExprEditor(this.window.getEndIdx(), false));
            this.add(this.endidx_edit);
            this.value0_edit = new LabeledExprEditor("Time of Zero", new ExprEditor(this.window.getValueAt0(), false));
            this.add(this.value0_edit);
        }

        public Data getData() {
            return new WindowData(this.startidx_edit.getData(), this.endidx_edit.getData(), this.value0_edit.getData());
        }

        public void reset() {
            this.startidx_edit.reset();
            this.endidx_edit.reset();
            this.value0_edit.reset();
        }

        public void setEditable(final boolean editable) {
            if(this.startidx_edit != null) this.startidx_edit.setEditable(editable);
            if(this.endidx_edit != null) this.endidx_edit.setEditable(editable);
            if(this.value0_edit != null) this.value0_edit.setEditable(editable);
        }
    }
    private static final long serialVersionUID = 4057231256009434757L;
    JComboBox                 combo;
    Data                      data;
    TreeDialog                dialog;
    boolean                   editable         = true;
    ExprEditor                expr_edit;
    int                       mode_idx, curr_mode_idx;
    WindowEdt                 window_edit;

    @SuppressWarnings("unchecked")
    public WindowEditor(final Data data, final TreeDialog dialog){
        this.dialog = dialog;
        this.data = data;
        if(data == null) this.mode_idx = 0;
        else if(data.dtype == Data.DTYPE_WINDOW) this.mode_idx = 1;
        else this.mode_idx = 2;
        this.curr_mode_idx = this.mode_idx;
        final String names[] = {"Undefined", "Window", "Expression"};
        this.combo = new JComboBox(names);
        this.combo.setEditable(false);
        this.combo.setSelectedIndex(this.mode_idx);
        this.combo.addActionListener(this);
        this.setLayout(new BorderLayout());
        final JPanel jp = new JPanel();
        jp.add(this.combo);
        this.add(jp, "North");
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
                this.remove(this.window_edit);
                break;
            case 2:
                this.remove(this.expr_edit);
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
                if(this.mode_idx == 1) this.window_edit = new WindowEdt((WindowData)this.data);
                else this.window_edit = new WindowEdt(null);
                this.add(this.window_edit, "Center");
                break;
            case 2:
                if(this.mode_idx == 2) this.expr_edit = new ExprEditor(this.data, false, 8, 30);
                else this.expr_edit = new ExprEditor(null, false, 8, 30);
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
                return this.window_edit.getData();
            case 2:
                return this.expr_edit.getData();
        }
        return null;
    }

    @Override
    public void reset() {
        switch(this.curr_mode_idx){
            case 1:
                this.remove(this.window_edit);
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
        else if(data.dtype == Data.DTYPE_WINDOW) this.mode_idx = 1;
        else this.mode_idx = 2;
        this.reset();
    }

    @Override
    public void setEditable(final boolean editable) {
        this.editable = editable;
        if(this.expr_edit != null) this.expr_edit.setEditable(editable);
        if(this.window_edit != null) this.window_edit.setEditable(editable);
    }
}