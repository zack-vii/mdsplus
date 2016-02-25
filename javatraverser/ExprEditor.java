// package jTraverser;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class ExprEditor extends JPanel implements ActionListener, Editor{
    private static final long serialVersionUID = -5215643316421671225L;
    Data                      data;
    boolean                   default_scroll;
    boolean                   default_to_string;
    boolean                   editable         = true;
    String                    expr;
    JButton                   left             = null, right = null;
    JPanel                    pl, pr;
    boolean                   quotes_added;
    int                       rows, columns;
    JTextArea                 text_area;
    JTextField                text_field;

    public ExprEditor(final boolean default_to_string){
        this(null, default_to_string, 1, 20);
    }

    public ExprEditor(final Data data, final boolean default_to_string){
        this(data, default_to_string, 1, 20);
    }

    public ExprEditor(final Data data, final boolean default_to_string, final int rows, final int columns){
        boolean quotes_needed;
        JScrollPane scroll_pane;
        this.rows = rows;
        this.columns = columns;
        this.default_to_string = default_to_string;
        if(rows > 1) this.default_scroll = true;
        if(data != null) this.expr = Tree.dataToString(data);
        else this.expr = null;
        quotes_needed = (default_to_string && (this.expr == null || this.expr.charAt(0) == '\"'));
        if(quotes_needed){
            this.quotes_added = true;
            this.left = new JButton("\"");
            this.right = new JButton("\"");
            this.left.setMargin(new Insets(0, 0, 0, 0));
            this.right.setMargin(new Insets(0, 0, 0, 0));
            this.left.addActionListener(this);
            this.right.addActionListener(this);
            if(this.expr != null) this.expr = this.expr.substring(1, this.expr.length() - 1);
        }else this.quotes_added = false;
        this.setLayout(new BorderLayout());
        if(this.default_scroll){
            this.text_area = new JTextArea(rows, columns);
            final Dimension d = this.text_area.getPreferredSize();
            this.text_area.setText(this.expr);
            d.height += 20;
            d.width += 20;
            scroll_pane = new JScrollPane(this.text_area);
            scroll_pane.setPreferredSize(d);
            if(quotes_needed){
                this.pl = new JPanel();
                this.pl.setLayout(new BorderLayout());
                this.pl.add(this.left, "North");
                this.add(this.pl, "East");
            }
            this.add(scroll_pane, "Center");
            if(quotes_needed){
                this.pr = new JPanel();
                this.pr.setLayout(new BorderLayout());
                this.pr.add(this.right, "North");
                this.add(this.pr, "West");
            }
        }else{
            if(quotes_needed) this.add(this.left, BorderLayout.LINE_START);
            this.text_field = new JTextField(columns);
            this.text_field.setText(this.expr);
            this.add(this.text_field);
            if(quotes_needed) this.add(this.right, BorderLayout.LINE_END);
        }
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        if(!this.editable) return;
        this.quotes_added = false;
        if(this.default_scroll){
            this.remove(this.pl);
            this.remove(this.pr);
        }else{
            this.remove(this.left);
            this.remove(this.right);
        }
        this.left = this.right = null;
        if(this.default_scroll) this.expr = this.text_area.getText();
        else this.expr = this.text_field.getText();
        this.expr = "\"" + this.expr + "\"";
        if(this.default_scroll) this.text_area.setText(this.expr);
        else this.text_field.setText(this.expr);
        this.validate();
        this.repaint();
    }

    @Override
    public Data getData() {
        if(this.default_scroll) this.expr = this.text_area.getText();
        else this.expr = this.text_field.getText();
        if(this.quotes_added) return Tree.dataFromExpr("\"" + this.expr + "\"");
        else return Tree.dataFromExpr(this.expr);
    }

    @Override
    public void reset() {
        if(this.data == null) this.expr = "";
        else this.expr = Tree.dataToString(this.data);
        if(this.default_to_string){
            final int len = this.expr.length();
            if(len >= 2) this.expr = this.expr.substring(1, len - 1);
        }
        if(this.default_scroll) this.text_area.setText(this.expr);
        else this.text_field.setText(this.expr);
    }

    public void setData(final Data data) {
        this.data = data;
        this.reset();
    }

    @Override
    public void setEditable(final boolean editable) {
        this.editable = editable;
        if(this.text_area != null) this.text_area.setEditable(editable);
        if(this.text_field != null) this.text_field.setEditable(editable);
    }
}
