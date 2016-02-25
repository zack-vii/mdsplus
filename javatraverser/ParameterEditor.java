// package jTraverser;
import java.awt.BorderLayout;
import javax.swing.JPanel;

public class ParameterEditor extends JPanel implements Editor{
    private static final long serialVersionUID = -3338106916551980199L;
    ExprEditor                expr, help, validity;

    public ParameterEditor(){
        this(new ExprEditor(null, false), new ExprEditor(null, true), new ExprEditor(null, false));
    }

    public ParameterEditor(final ExprEditor expr, final ExprEditor help, final ExprEditor validity){
        this.expr = expr;
        this.help = help;
        this.validity = validity;
        this.setLayout(new BorderLayout());
        this.add(new LabeledExprEditor("Data", expr), "North");
        this.add(new LabeledExprEditor("Help", help), "Center");
        this.add(new LabeledExprEditor("Validity", validity), "South");
    }

    @Override
    public Data getData() {
        return new ParameterData(this.expr.getData(), this.help.getData(), this.validity.getData());
    }

    @Override
    public void reset() {
        this.expr.reset();
        this.help.reset();
        this.validity.reset();
    }

    public void setData(final Data data) {
        if(data instanceof ParameterData){
            this.expr.setData(((ParameterData)data).getDatum());
            this.help.setData(((ParameterData)data).getHelp());
            this.validity.setData(((ParameterData)data).getValidation());
        }
    }

    @Override
    public void setEditable(final boolean editable) {
        this.expr.setEditable(editable);
        this.help.setEditable(editable);
        this.validity.setEditable(editable);
    }
}
