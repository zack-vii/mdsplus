// package jTraverser;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JComboBox;
import javax.swing.JPanel;

public class AxisEditor extends JPanel implements ActionListener, Editor{
    private static final long serialVersionUID = 8765062933985562610L;
    JComboBox                 combo;
    Data                      data;
    TreeDialog                dialog;
    boolean                   editable         = true;
    LabeledExprEditor         expr_edit, units_edit;
    int                       mode_idx, curr_mode_idx;
    JPanel                    mode_panel;
    RangeData                 range;
    RangeEditor               range_edit;
    Data                      units;

    @SuppressWarnings("unchecked")
    public AxisEditor(Data data, final TreeDialog dialog){
        this.dialog = dialog;
        this.data = data;
        if(data == null){
            this.mode_idx = 0;
            data = null;
            this.range = null;
            this.units = null;
        }else{
            if(data.dtype == Data.DTYPE_WITH_UNITS){
                this.units = ((WithUnitsData)data).getUnits();
                this.data = ((WithUnitsData)data).getDatum();
            }else this.data = data;
            if(this.data.dtype == Data.DTYPE_RANGE){
                this.mode_idx = 1;
                this.range = (RangeData)this.data;
                this.data = null;
            }else this.mode_idx = 2;
        }
        this.curr_mode_idx = this.mode_idx;
        final String names[] = {"Undefined", "Range", "Expression"};
        this.combo = new JComboBox(names);
        this.combo.setEditable(false);
        this.combo.setSelectedIndex(this.mode_idx);
        this.combo.addActionListener(this);
        this.mode_panel = new JPanel();
        this.mode_panel.add(this.combo);
        this.setLayout(new BorderLayout());
        this.add(this.mode_panel, "North");
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
                this.mode_panel.remove(this.units_edit);
                this.remove(this.range_edit);
                break;
            case 2:
                this.mode_panel.remove(this.units_edit);
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
                this.range_edit = new RangeEditor(this.range);
                this.units_edit = new LabeledExprEditor("Units", new ExprEditor(this.units, true));
                this.mode_panel.add(this.units_edit);
                this.add(this.range_edit, "Center");
                break;
            case 2:
                this.expr_edit = new LabeledExprEditor(this.data);
                this.units_edit = new LabeledExprEditor("Units", new ExprEditor(this.units, true));
                this.mode_panel.add(this.units_edit);
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
                Data units = this.units_edit.getData();
                if(units != null) return new WithUnitsData(this.range_edit.getData(), units);
                else return this.range_edit.getData();
            case 2:
                units = this.units_edit.getData();
                if(units != null) return new WithUnitsData(this.expr_edit.getData(), units);
                else return this.expr_edit.getData();
        }
        return null;
    }

    @Override
    public void reset() {
        switch(this.curr_mode_idx){
            case 1:
                this.mode_panel.remove(this.units_edit);
                this.units_edit = null;
                this.remove(this.range_edit);
                this.range_edit = null;
                break;
            case 2:
                this.mode_panel.remove(this.units_edit);
                this.remove(this.expr_edit);
                this.expr_edit = null;
                break;
        }
        this.curr_mode_idx = this.mode_idx;
        this.addEditor();
        this.validate();
        this.repaint();
    }

    public void setData(Data data) {
        this.data = data;
        if(data == null){
            this.mode_idx = 0;
            data = null;
            this.range = null;
            this.units = null;
        }else{
            if(data.dtype == Data.DTYPE_WITH_UNITS){
                this.units = ((WithUnitsData)data).getUnits();
                this.data = ((WithUnitsData)data).getDatum();
            }else this.data = data;
            if(this.data.dtype == Data.DTYPE_RANGE){
                this.mode_idx = 1;
                this.range = (RangeData)this.data;
                this.data = null;
            }else this.mode_idx = 2;
        }
        this.reset();
    }

    @Override
    public void setEditable(final boolean editable) {
        this.editable = editable;
        if(this.expr_edit != null) this.expr_edit.setEditable(editable);
        if(this.range_edit != null) this.range_edit.setEditable(editable);
        if(this.units_edit != null) this.units_edit.setEditable(editable);
    }
}
