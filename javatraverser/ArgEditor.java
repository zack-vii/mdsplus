// package jTraverser;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class ArgEditor extends JScrollPane{
    private static final long serialVersionUID = 6964842722799941711L;
    ExprEditor[]              args;
    int                       num_args;
    Dimension                 preferred;

    public ArgEditor(){
        this(null, 9, new Dimension(220, 89));
    }

    public ArgEditor(final Data[] data){
        this(data, 9, new Dimension(220, 89));
    }

    public ArgEditor(Data[] data, final int num_args, final Dimension preferred){
        if(data == null) data = new Data[num_args];
        this.preferred = preferred;
        this.num_args = num_args;
        final JPanel jp = new JPanel();
        jp.setLayout(new GridLayout(num_args, 1));
        this.args = new ExprEditor[num_args];
        for(int i = 0; i < num_args; i++){
            if(i < data.length) this.args[i] = new ExprEditor(data[i], false);
            else this.args[i] = new ExprEditor(null, false);
            jp.add(new LabeledExprEditor("Argument " + (i + 1), this.args[i]));
        }
        final JPanel jp2 = new JPanel();
        jp2.setLayout(new BorderLayout());
        jp2.add(jp, BorderLayout.NORTH);
        this.setViewportView(jp2);
        this.setPreferredSize(preferred);
        this.getVerticalScrollBar().setUnitIncrement(43);
    }

    public Data[] getData() {
        final Data data[] = new Data[this.num_args];
        for(int i = 0; i < this.num_args; i++)
            data[i] = this.args[i].getData();
        return data;
    }

    public void reset() {
        for(int i = 0; i < this.num_args; i++)
            this.args[i].reset();
    }

    public void setData(final Data[] data) {
        int min_len = 0, i = 0;
        if(data != null){
            if(data.length < this.num_args) min_len = data.length;
            else min_len = this.num_args;
            for(; i < min_len; i++)
                this.args[i].setData(data[i]);
        }
        for(; i < this.num_args; i++)
            this.args[i].setData(null);
    }

    public void setEditable(final boolean editable) {
        for(int i = 0; i < this.num_args; i++)
            this.args[i].setEditable(editable);
    }
}