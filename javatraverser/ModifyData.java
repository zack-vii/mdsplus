// package jTraverser;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

public class ModifyData extends NodeEditor{
    private static final long serialVersionUID = -1402049893755852077L;

    private static String tagList(final String[] tags) {
        if(tags == null || tags.length == 0) return "<no tags>";
        final StringBuffer sb = new StringBuffer();
        for(int i = 0; i < tags.length; i++){
            sb.append(tags[i]);
            if(i < tags.length - 1) sb.append(", ");
        }
        return new String(sb);
    }
    ActionEditor   action_edit   = null;
    Editor         curr_edit;
    DataEditor     data_edit     = null;
    TreeDialog     dialog;
    DispatchEditor dispatch_edit = null;
    boolean        is_editable;
    JButton        ok_b, apply_b, reset_b, cancel_b;
    JLabel         onoff;
    RangeEditor    range_edit    = null;
    JLabel         tags;
    TaskEditor     task_edit     = null;
    WindowEditor   window_edit   = null;

    public ModifyData(){
        this(true);
    }

    public ModifyData(final boolean editable){
        this.is_editable = editable;
        this.setLayout(new BorderLayout());
        final JPanel ip = new JPanel();
        ip.add(this.onoff = new JLabel(""));
        ip.add(new JLabel("Tags: "));
        ip.add(this.tags = new JLabel(""));
        this.add(ip, "North");
        final JPanel jp = new JPanel();
        this.add(jp, "South");
        if(this.is_editable){
            this.ok_b = new JButton("Ok");
            this.ok_b.addActionListener(new ActionListener(){
                @Override
                public void actionPerformed(ActionEvent e) {
                    ModifyData.this.ok();
                }
            });
            jp.add(this.ok_b);
            this.apply_b = new JButton("Apply");
            this.apply_b.addActionListener(new ActionListener(){
                @Override
                public void actionPerformed(ActionEvent e) {
                    ModifyData.this.apply();
                }
            });
            jp.add(this.apply_b);
            this.reset_b = new JButton("Reset");
            this.reset_b.addActionListener(new ActionListener(){
                @Override
                public void actionPerformed(ActionEvent e) {
                    ModifyData.this.reset();
                }
            });
            jp.add(this.reset_b);
            this.addKeyListener(new KeyAdapter(){
                @Override
                public void keyTyped(final KeyEvent e) {
                    if(e.getKeyCode() == KeyEvent.VK_ENTER) ModifyData.this.ok();
                }
            });
        }
        this.cancel_b = new JButton("Cancel");
        this.cancel_b.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                ModifyData.this.cancel();
            }
        });
        this.cancel_b.setSelected(true);
        jp.add(this.cancel_b);
    }

    private boolean apply() {
        try{
            this.node.setData(this.curr_edit.getData());
        }catch(final Exception e){
            JOptionPane.showMessageDialog(this.frame, e.getMessage(), "Error writing datafile", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    private void cancel() {
        this.frame.dispose();
    }

    private void ok() {
        if(this.apply()) this.cancel();
    }

    private void replace(final Editor edit) {
        if(this.curr_edit != null && this.curr_edit != edit) this.remove((Component)this.curr_edit);
        this.curr_edit = edit;
        this.add((Component)edit, "Center");
        // add(edit);
    }

    private void reset() {
        this.curr_edit.reset();
        this.validate();
        this.repaint();
    }

    @Override
    public void setNode(final Node _node) {
        Data data;
        this.node = _node;
        try{
            data = this.node.getData();
        }catch(final Exception e){
            data = null;
        }
        switch(this.node.getUsage()){
            case NodeInfo.USAGE_ACTION:
                if(this.action_edit == null) this.action_edit = new ActionEditor(data, this.frame);
                else this.action_edit.setData(data);
                this.action_edit.setEditable(this.is_editable);
                this.replace(this.action_edit);
                break;
            case NodeInfo.USAGE_DISPATCH:
                if(this.dispatch_edit == null) this.dispatch_edit = new DispatchEditor(data, this.frame);
                else this.dispatch_edit.setData(data);
                this.replace(this.dispatch_edit);
                this.dispatch_edit.setEditable(this.is_editable);
                break;
            case NodeInfo.USAGE_TASK:
                if(this.task_edit == null) this.task_edit = new TaskEditor(data, this.frame);
                else this.task_edit.setData(data);
                this.replace(this.task_edit);
                this.task_edit.setEditable(this.is_editable);
                break;
            case NodeInfo.USAGE_WINDOW:
                if(this.window_edit == null) this.window_edit = new WindowEditor(data, this.frame);
                else this.window_edit.setData(data);
                this.replace(this.window_edit);
                this.window_edit.setEditable(this.is_editable);
                break;
            case NodeInfo.USAGE_AXIS:
                if(data instanceof RangeData){
                    if(this.range_edit == null) this.range_edit = new RangeEditor((RangeData)data);
                    else this.range_edit.setData(data);
                    this.replace(this.range_edit);
                    this.range_edit.setEditable(this.is_editable);
                    break;
                }
            default:
                if(this.data_edit == null) this.data_edit = new DataEditor(data, this.frame);
                else this.data_edit.setData(data);
                this.replace(this.data_edit);
                this.data_edit.setEditable(this.is_editable);
        }
        if(this.node.isOn()) this.onoff.setText("Node is On   ");
        else this.onoff.setText("Node is Off  ");
        try{
            if(this.is_editable) this.frame.setTitle("Modify data of " + this.node.getInfo().getFullPath());
            else this.frame.setTitle("Display data of " + this.node.getInfo().getFullPath());
        }catch(final Exception exc){}
        this.tags.setText(ModifyData.tagList(this.node.getTags()));
    }
}