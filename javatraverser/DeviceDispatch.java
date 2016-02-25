import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.ComboBoxEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;

public class DeviceDispatch extends DeviceComponent{
    class DispatchComboEditor implements ComboBoxEditor{
        int    idx;
        JLabel label = new JLabel("  Dispatch");
        String name;

        @Override
        public void addActionListener(final ActionListener l) {}

        @Override
        public Component getEditorComponent() {
            return this.label;
        }

        @Override
        public Object getItem() {
            return this.label;
        }

        @Override
        public void removeActionListener(final ActionListener l) {}

        @Override
        public void selectAll() {}

        @Override
        public void setItem(final Object obj) {}
    }
    private static final long serialVersionUID = 5261317294707988323L;
    Data                      actions[];
    JDialog                   dialog           = null;
    DeviceDispatchField       dispatch_fields[], active_field;
    int                       i, j, num_actions;
    protected boolean         initializing     = false;
    JComboBox                 menu;

    public DeviceDispatch(){
        this.menu = new JComboBox();
        this.menu.setEditor(new DispatchComboEditor());
        this.menu.setEditable(true);
        this.menu.setBorder(new LineBorder(Color.black, 1));
        this.add(this.menu);
    }

    protected void activateForm(final DeviceDispatchField field, final String name) {
        if(this.dialog == null){
            this.dialog = new JDialog(FrameRepository.frame);
            this.dialog.getContentPane().setLayout(new BorderLayout());
            this.dialog.getContentPane().add(field, "Center");
            final JPanel jp = new JPanel();
            final JButton button = new JButton("Done");
            button.addActionListener(new ActionListener(){
                @Override
                public void actionPerformed(ActionEvent e) {
                    DeviceDispatch.this.dialog.dispose();
                }
            });
            jp.add(button);
            this.dialog.getContentPane().add(jp, "South");
            this.active_field = field;
        }else{
            this.dialog.getContentPane().remove(this.active_field);
            this.dialog.getContentPane().add(field, "Center");
            this.active_field = field;
        }
        this.dialog.setTitle("Dispatch info for " + name);
        this.dialog.pack();
        this.dialog.repaint();
        this.dialog.setLocation(FrameRepository.frame.getLocationOnScreen());
        this.dialog.setVisible(true);
    }

    @Override
    public void apply() throws Exception {
        if(this.dispatch_fields == null) return;
        for(final DeviceDispatchField dispatch_field : this.dispatch_fields)
            dispatch_field.apply();
    }

    @Override
    public void apply(final int currBaseNid) {}

    @Override
    protected void displayData(final Data data, final boolean is_on) {}

    @Override
    protected Data getData() {
        return null;
    }

    @Override
    protected boolean getState() {
        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void initializeData(final Data data, final boolean is_on)
    // data and is_on arguments are meaningless in this context
    // The class will search actions stored in the device
    // and create and manage their dispatch configurations
    {
        this.initializing = true;
        NodeInfo nodeInfo;
        if(this.subtree == null) return;
        try{
            nodeInfo = this.subtree.getInfo(this.nidData, Tree.context);
        }catch(final Exception e){
            System.out.println("Cannot read device NCI: " + e);
            return;
        }
        NidData currNid = new NidData(this.baseNidData.getInt());
        int num_components = nodeInfo.getConglomerateNids();
        final NodeInfo nodeInfos[] = new NodeInfo[num_components + 1];
        for(this.i = this.num_actions = 0; this.i < num_components; this.i++){
            try{
                nodeInfos[this.i] = this.subtree.getInfo(currNid, Tree.context);
            }catch(final Exception e){
                System.out.println("Cannot read device NCI 1: " + e + " " + currNid.getInt() + " " + this.num_actions + " " + num_components);
                num_components = this.i;
                break;
            }
            if(nodeInfos[this.i].getUsage() == NodeInfo.USAGE_ACTION) this.num_actions++;
            currNid.incrementNid();
        }
        this.actions = new Data[this.num_actions];
        this.dispatch_fields = new DeviceDispatchField[this.num_actions];
        currNid = new NidData(this.nidData.getInt());
        for(this.i = this.j = this.num_actions = 0; this.i < num_components; this.i++){
            if(nodeInfos[this.i].getUsage() == NodeInfo.USAGE_ACTION){
                try{
                    this.actions[this.j] = this.subtree.getData(currNid, Tree.context);
                }catch(final Exception e){
                    System.out.println("Cannot read device actions: " + e);
                    return;
                }
                this.dispatch_fields[this.j] = new DeviceDispatchField();
                this.dispatch_fields[this.j].setSubtree(this.subtree);
                this.dispatch_fields[this.j].setOffsetNid(this.i);
                this.dispatch_fields[this.j].configure(this.nidData.getInt());
                this.j++;
            }
            currNid.incrementNid();
        }
        for(this.i = 0; this.i < num_components; this.i++){
            if(nodeInfos[this.i].getUsage() == NodeInfo.USAGE_ACTION){
                final String name = nodeInfos[this.i].getName();
                this.menu.addItem(name);
            }
        }
        this.menu.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                final int idx = DeviceDispatch.this.menu.getSelectedIndex();
                if(idx < 0 || idx >= DeviceDispatch.this.dispatch_fields.length) return;
                DeviceDispatch.this.activateForm(DeviceDispatch.this.dispatch_fields[DeviceDispatch.this.menu.getSelectedIndex()], (String)DeviceDispatch.this.menu.getSelectedItem());
            }
        });
        this.initializing = false;
    }

    @Override
    public void reset() {
        if(this.dispatch_fields == null) return;
        for(final DeviceDispatchField dispatch_field : this.dispatch_fields)
            dispatch_field.reset();
    }
}
