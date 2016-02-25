// package jTraverser;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.StringTokenizer;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class PythonEditor extends JPanel implements Editor{
    static final int          OPC_FUN          = 162;
    private static final long serialVersionUID = -6921161255217870790L;
    boolean                   default_scroll;
    boolean                   editable         = true;
    String                    program;
    String                    retVar;
    int                       rows             = 7, columns = 20;
    JTextArea                 text_area;
    JTextField                text_field;

    public PythonEditor(final Data[] dataArgs){
        JScrollPane scroll_pane;
        if(this.rows > 1) this.default_scroll = true;
        if(dataArgs != null){
            this.getProgram(dataArgs);
        }else{
            this.program = "";
            this.retVar = "";
        }
        this.text_area = new JTextArea(this.rows, this.columns);
        this.text_area.setText(this.program);
        this.text_field = new JTextField(10);
        this.text_field.setText(this.retVar);
        final Dimension d = this.text_area.getPreferredSize();
        d.height += 20;
        d.width += 20;
        final JPanel jp = new JPanel();
        jp.setLayout(new BorderLayout());
        final JPanel jp1 = new JPanel();
        jp1.setLayout(new BorderLayout());
        jp1.setBorder(BorderFactory.createTitledBorder("Return Variable"));
        jp1.add(this.text_field);
        jp.add(jp1, "North");
        final JPanel jp2 = new JPanel();
        jp2.setLayout(new BorderLayout());
        jp2.setBorder(BorderFactory.createTitledBorder("Program"));
        scroll_pane = new JScrollPane(this.text_area);
        scroll_pane.setPreferredSize(d);
        jp2.add(scroll_pane);
        jp.add(jp2, "Center");
        this.setLayout(new BorderLayout());
        this.add(jp, "Center");
    }

    @Override
    public Data getData() {
        final String programTxt = this.text_area.getText();
        if(programTxt == null || programTxt.equals("")) return null;
        final StringTokenizer st = new StringTokenizer(programTxt, "\n");
        final String[] lines = new String[st.countTokens()];
        int idx = 0;
        int maxLen = 0;
        while(st.hasMoreTokens()){
            lines[idx] = st.nextToken();
            if(maxLen < lines[idx].length()) maxLen = lines[idx].length();
            idx++;
        }
        for(int i = 0; i < lines.length; i++){
            final int len = lines[i].length();
            for(int j = 0; j < maxLen - len; j++)
                lines[i] += " ";
        }
        final StringArray stArr = new StringArray(lines);
        final String retVarTxt = this.text_field.getText();
        Data retArgs[];
        if(retVarTxt == null || retVarTxt.equals("")){
            retArgs = new Data[3];
            retArgs[0] = null;
            retArgs[1] = new StringData("Py");
            retArgs[2] = stArr;
        }else{
            retArgs = new Data[4];
            retArgs[0] = null;
            retArgs[1] = new StringData("Py");
            retArgs[2] = stArr;
            retArgs[3] = new StringData(retVarTxt);
        }
        return new FunctionData(PythonEditor.OPC_FUN, retArgs);
    }

    void getProgram(final Data[] dataArgs) {
        if(dataArgs.length <= 3) this.retVar = "";
        else{
            try{
                this.retVar = dataArgs[3].getString();
            }catch(final Exception exc){
                this.retVar = "";
            }
        }
        String[] lines;
        try{
            if(dataArgs[2] instanceof StringArray) lines = dataArgs[2].getStringArray();
            else{
                lines = new String[1];
                lines[0] = dataArgs[1].getString();
            }
            this.program = "";
            for(final String line : lines){
                this.program += line + "\n";
            }
        }catch(final Exception exc){
            this.program = "";
        }
    }

    @Override
    public void reset() {
        this.text_area.setText(this.program);
        this.text_field = new JTextField(this.retVar);
    }

    @Override
    public void setEditable(final boolean editable) {
        this.editable = editable;
        if(this.text_area != null) this.text_area.setEditable(editable);
        if(this.text_field != null) this.text_field.setEditable(editable);
    }
}
