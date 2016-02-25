// package jTraverser;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JComboBox;
import javax.swing.JPanel;

public class DataEditor extends JPanel implements ActionListener, Editor{
    private static final long serialVersionUID = -7570715708392389979L;
    JComboBox                 combo;
    Data                      data;
    TreeDialog                dialog;
    boolean                   editable         = true;
    LabeledExprEditor         expr_edit, units_edit;
    int                       mode_idx, curr_mode_idx;
    JPanel                    panel;
    ParameterEditor           param_edit;
    PythonEditor              python_edit;
    Data                      units;

    @SuppressWarnings("unchecked")
    public DataEditor(final Data data, final TreeDialog dialog){
        this.dialog = dialog;
        this.data = data;
        if(data == null){
            this.mode_idx = 0;
            this.data = null;
            this.units = null;
        }else{
            if(data instanceof ParameterData) this.mode_idx = 2;
            else if(data instanceof FunctionData && ((FunctionData)data).opcode == PythonEditor.OPC_FUN){
                final Data[] args = ((FunctionData)data).getArgs();
                try{
                    if(args != null && args.length > 2 && args[1] != null && (args[1] instanceof StringData) && args[1].getString() != null && args[1].getString().toUpperCase().equals("PY")) this.mode_idx = 3;
                    else this.mode_idx = 1;
                }catch(final Exception exc){
                    this.mode_idx = 1;
                }
            }else this.mode_idx = 1;
            if(data.dtype == Data.DTYPE_WITH_UNITS){
                this.data = ((WithUnitsData)data).getDatum();
                this.units = ((WithUnitsData)data).getUnits();
            }else{
                this.data = data;
                this.units = null;
            }
        }
        this.curr_mode_idx = this.mode_idx;
        final String names[] = {"Undefined", "Expression", "Parameter", "Python Expression"};
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
        this.remove(this.panel);
        this.curr_mode_idx = idx;
        this.addEditor();
        this.validate();
        this.dialog.repack();
    }

    private void addEditor() {
        this.panel = new JPanel();
        this.panel.setLayout(new BorderLayout());
        switch(this.curr_mode_idx){
            case 0:
                return;
            case 1:
                // final boolean default_to_string = (this.data != null && this.data.dtype == Data.DTYPE_T);
                this.panel.add(this.expr_edit = new LabeledExprEditor(this.data));
                break;
            case 2:
                Data _data, _help = null, _validation = null;
                if(this.data != null && this.data instanceof ParameterData){
                    _data = ((ParameterData)this.data).getDatum();
                    _help = ((ParameterData)this.data).getHelp();
                    _validation = ((ParameterData)this.data).getValidation();
                }else _data = this.data;
                this.param_edit = new ParameterEditor(new ExprEditor(_data, false, 3, 20), new ExprEditor(_help, true, 4, 20), new ExprEditor(_validation, false, 1, 20));
                this.panel.add(this.param_edit);
                break;
            case 3:
                if(this.data != null && this.data instanceof FunctionData){
                    this.python_edit = new PythonEditor(((FunctionData)this.data).getArgs());
                }else{
                    this.python_edit = new PythonEditor(null);
                }
                this.panel.add(this.python_edit);
                break;
        }
        this.units_edit = new LabeledExprEditor("Units", new ExprEditor(this.units, true));
        this.panel.add(this.units_edit, BorderLayout.NORTH);
        this.add(this.panel, BorderLayout.CENTER);
    }

    @Override
    public Data getData() {
        Data units;
        switch(this.curr_mode_idx){
            case 0:
                return null;
            case 1:
                units = this.units_edit.getData();
                if(units != null){
                    if(units instanceof StringData && ((StringData)units).datum.equals("")) return this.expr_edit.getData();
                    else return new WithUnitsData(this.expr_edit.getData(), units);
                }else return this.expr_edit.getData();
            case 2:
                units = this.units_edit.getData();
                if(units != null){
                    if(units instanceof StringData && ((StringData)units).datum.equals("")) return this.param_edit.getData();
                    else return new WithUnitsData(this.param_edit.getData(), units);
                }else return this.param_edit.getData();
            case 3:
                units = this.units_edit.getData();
                if(units != null){
                    if(units instanceof StringData && ((StringData)units).datum.equals("")) return this.python_edit.getData();
                    else return new WithUnitsData(this.python_edit.getData(), units);
                }else return this.python_edit.getData();
        }
        return null;
    }

    @Override
    public void reset() {
        if(this.curr_mode_idx > 0) this.remove(this.panel);
        this.curr_mode_idx = this.mode_idx;
        this.combo.setSelectedIndex(this.mode_idx);
        this.addEditor();
        this.validate();
        this.dialog.repack();
    }

    public void setData(final Data data) {
        this.data = data;
        if(data == null){
            this.mode_idx = 0;
            this.data = null;
            this.units = null;
        }else{
            if(data instanceof ParameterData) this.mode_idx = 2;
            else if(data instanceof FunctionData && ((FunctionData)data).opcode == PythonEditor.OPC_FUN){
                final Data[] args = ((FunctionData)data).getArgs();
                try{
                    if(args != null && args.length > 2 && args[1] != null && (args[1] instanceof StringData) && args[1].getString() != null && args[1].getString().toUpperCase().equals("PY")) this.mode_idx = 3;
                    else this.mode_idx = 1;
                }catch(final Exception exc){
                    this.mode_idx = 1;
                }
            }else this.mode_idx = 1;
            if(data.dtype == Data.DTYPE_WITH_UNITS){
                this.data = ((WithUnitsData)data).getDatum();
                this.units = ((WithUnitsData)data).getUnits();
            }else{
                this.data = data;
                this.units = null;
            }
        }
        this.reset();
    }

    @Override
    public void setEditable(final boolean editable) {
        this.editable = editable;
        if(this.expr_edit != null) this.expr_edit.setEditable(editable);
        if(this.python_edit != null) this.python_edit.setEditable(editable);
        if(this.units_edit != null) this.units_edit.setEditable(editable);
    }
}