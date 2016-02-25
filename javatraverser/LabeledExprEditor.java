// package jTraverser;
import java.awt.BorderLayout;
import javax.swing.BorderFactory;
import javax.swing.JPanel;

public class LabeledExprEditor extends JPanel implements Editor{
    private static final long serialVersionUID = -7771290623140577516L;
    ExprEditor                expr;

    public LabeledExprEditor(final Data data){
        this("Expression", new ExprEditor(data, (data != null && data.dtype == Data.DTYPE_T), 4, 20));
    }

    public LabeledExprEditor(final String label_str){
        this(label_str, new ExprEditor(null, false));
    }

    public LabeledExprEditor(final String label_str, final ExprEditor expr){
        this.expr = expr;
        this.setLayout(new BorderLayout());
        this.setBorder(BorderFactory.createTitledBorder(label_str));
        this.add(expr, "Center");
    }

    @Override
    public Data getData() {
        return this.expr.getData();
    }

    @Override
    public void reset() {
        this.expr.reset();
    }

    public void setData(final Data data) {
        this.expr.setData(data);
    }

    @Override
    public void setEditable(final boolean editable) {
        this.expr.setEditable(editable);
    }
}