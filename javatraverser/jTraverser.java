// package jTraverser;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

public class jTraverser extends JFrame implements ActionListener{
    static boolean            editable, readonly, model;
    static String             exp_name, shot_name;
    private static final long serialVersionUID = 3044553985814315370L;
    static JLabel             status           = new JLabel("jTaverser started");
    static Tree               tree;

    public static String getExperimentName() {
        return jTraverser.exp_name;
    }

    public static boolean isEditable() {
        return jTraverser.editable;
    }

    /**
     * Constructor.
     */
    public static void main(final String args[]) {
        /* the original font is either ugly or defaults to an ugly font if not found
        if(System.getProperty("os.name").equals("Linux"))
        {
            UIManager.put("Label.font", new FontUIResource(new Font("FreeSerif", Font.BOLD, 11)));
            UIManager.put("ComboBox.font", new FontUIResource(new Font("FreeSerif", Font.BOLD, 11)));
            UIManager.put("Button.font", new FontUIResource(new Font("FreeSerif", Font.BOLD, 11)));
        }*/
        if(args.length >= 3) FrameRepository.frame = new jTraverser(args[0], args[1], args[2]);
        else if(args.length == 2) FrameRepository.frame = new jTraverser(args[0], args[1], null);
        else if(args.length == 1) FrameRepository.frame = new jTraverser(args[0], null, null);
        else FrameRepository.frame = new jTraverser(null, null, null);
    }

    public static void stderr(final String line, final Exception exc) {
        jTraverser.status.setText("ERROR: " + line + " (" + exc.getMessage() + ")");
        System.err.println(line + "\n" + exc);
    }

    public static void stdout(final String line) {
        jTraverser.status.setText(line);
    }
    JMenuItem   add_action_b, add_dispatch_b, add_numeric_b, add_signal_b, add_task_b, add_text_b, add_window_b, add_axis_b, add_device_b, add_child_b, add_subtree_b, delete_node_b, modify_tags_b, rename_node_b, turn_on_b, turn_off_b, display_data_b,
            display_nci_b, display_tags_b, modify_data_b, set_default_b, setup_device_b, do_action_b, outline_b, tree_b, copy_b, paste_b;
    DisplayData display_data;
    TreeDialog  display_data_d, modify_data_d, display_nci_d, display_tags_d;
    DisplayNci  display_nci;
    DisplayTags display_tags;
    JMenu       file_m, edit_m, data_m, customize_m;
    ModifyData  modify_data;
    JMenuItem   open, close, quit;

    public jTraverser(final String exp_name, final String shot_name, final String access){
        jTraverser.exp_name = exp_name;
        jTraverser.shot_name = shot_name;
        this.setTitle("jTraverser - no tree open");
        Boolean edit = false;
        Boolean readonly = false;
        if(access != null){
            edit = (new String(access).equals("-edit"));
            readonly = (new String(access).equals("-readonly"));
        }
        final JMenuBar menu_bar = new JMenuBar();
        this.setJMenuBar(menu_bar);
        JMenu curr_menu = new JMenu("File");
        this.file_m = curr_menu;
        menu_bar.add(curr_menu);
        this.open = new JMenuItem("Open");
        curr_menu.add(this.open);
        this.open.addActionListener(this);
        this.close = new JMenuItem("Close");
        curr_menu.add(this.close);
        this.close.addActionListener(this);
        this.quit = new JMenuItem("Quit");
        curr_menu.add(this.quit);
        this.quit.addActionListener(this);
        this.edit_m = curr_menu = new JMenu("Edit");
        menu_bar.add(curr_menu);
        JMenuItem jm = new JMenu("Add Node");
        jm.add(this.add_action_b = new JMenuItem("Action"));
        this.add_action_b.addActionListener(this);
        jm.add(this.add_dispatch_b = new JMenuItem("Dispatch"));
        this.add_dispatch_b.addActionListener(this);
        jm.add(this.add_numeric_b = new JMenuItem("Numeric"));
        this.add_numeric_b.addActionListener(this);
        jm.add(this.add_signal_b = new JMenuItem("Signal"));
        this.add_signal_b.addActionListener(this);
        jm.add(this.add_task_b = new JMenuItem("Task"));
        this.add_task_b.addActionListener(this);
        jm.add(this.add_text_b = new JMenuItem("Text"));
        this.add_text_b.addActionListener(this);
        jm.add(this.add_window_b = new JMenuItem("Window"));
        this.add_window_b.addActionListener(this);
        jm.add(this.add_axis_b = new JMenuItem("Axis"));
        this.add_axis_b.addActionListener(this);
        curr_menu.add(jm);
        curr_menu.add(this.add_device_b = new JMenuItem("Add Device"));
        this.add_device_b.addActionListener(this);
        curr_menu.add(this.add_child_b = new JMenuItem("Add Child"));
        this.add_child_b.addActionListener(this);
        curr_menu.add(this.add_subtree_b = new JMenuItem("Add Subtree"));
        this.add_subtree_b.addActionListener(this);
        curr_menu.add(this.delete_node_b = new JMenuItem("Delete Node"));
        this.delete_node_b.addActionListener(this);
        curr_menu.add(this.modify_tags_b = new JMenuItem("Modify tags"));
        this.modify_tags_b.addActionListener(this);
        curr_menu.add(this.rename_node_b = new JMenuItem("Rename node"));
        this.rename_node_b.addActionListener(this);
        curr_menu.add(this.copy_b = new JMenuItem("Copy"));
        this.copy_b.addActionListener(this);
        curr_menu.add(this.paste_b = new JMenuItem("Paste"));
        this.paste_b.addActionListener(this);
        this.data_m = curr_menu = new JMenu("Data");
        menu_bar.add(curr_menu);
        curr_menu.add(this.turn_on_b = new JMenuItem("Turn On"));
        this.turn_on_b.addActionListener(this);
        curr_menu.add(this.turn_off_b = new JMenuItem("Turn Off"));
        this.turn_off_b.addActionListener(this);
        curr_menu.add(this.display_data_b = new JMenuItem("Display Data"));
        this.display_data_b.addActionListener(this);
        curr_menu.add(this.display_nci_b = new JMenuItem("Display Nci"));
        this.display_nci_b.addActionListener(this);
        curr_menu.add(this.display_tags_b = new JMenuItem("Display Tags"));
        this.display_tags_b.addActionListener(this);
        curr_menu.add(this.modify_data_b = new JMenuItem("Modify Data"));
        this.modify_data_b.addActionListener(this);
        curr_menu.add(this.set_default_b = new JMenuItem("Set Default"));
        this.set_default_b.addActionListener(this);
        curr_menu.add(this.setup_device_b = new JMenuItem("Setup Device"));
        this.setup_device_b.addActionListener(this);
        curr_menu.add(this.do_action_b = new JMenuItem("Do Action"));
        this.do_action_b.addActionListener(this);
        curr_menu = new JMenu("Customize");
        menu_bar.add(curr_menu);
        jm = new JMenu("Display Mode");
        jm.add(this.outline_b = new JMenuItem("Outline"));
        this.outline_b.addActionListener(this);
        jm.add(this.tree_b = new JMenuItem("Tree"));
        this.tree_b.addActionListener(this);
        curr_menu.add(jm);
        jTraverser.tree = new Tree(this);
        if(exp_name != null) jTraverser.tree.open(exp_name.toUpperCase(), (shot_name == null) ? -1 : Integer.parseInt(shot_name), edit, readonly, false);
        this.getContentPane().add(jTraverser.tree);
        this.getContentPane().add(jTraverser.status, BorderLayout.PAGE_END);
        this.addWindowListener(new WindowAdapter(){
            @Override
            public void windowClosing(final WindowEvent e) {
                System.exit(0);
            }
        });
        this.pack();
        this.setVisible(true);
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        final Object source = e.getSource();
        if(source == this.open) jTraverser.tree.open();
        if(source == this.close) jTraverser.tree.close();
        if(source == this.quit) jTraverser.tree.quit();
        if(source == this.tree_b) jTraverser.tree.setAngled(true);
        if(source == this.outline_b) jTraverser.tree.setAngled(false);
        if(source == this.add_action_b) jTraverser.tree.addNode(NodeInfo.USAGE_ACTION);
        if(source == this.add_dispatch_b) jTraverser.tree.addNode(NodeInfo.USAGE_DISPATCH);
        if(source == this.add_numeric_b) jTraverser.tree.addNode(NodeInfo.USAGE_NUMERIC);
        if(source == this.add_signal_b) jTraverser.tree.addNode(NodeInfo.USAGE_SIGNAL);
        if(source == this.add_task_b) jTraverser.tree.addNode(NodeInfo.USAGE_TASK);
        if(source == this.add_text_b) jTraverser.tree.addNode(NodeInfo.USAGE_TEXT);
        if(source == this.add_child_b) jTraverser.tree.addNode(NodeInfo.USAGE_STRUCTURE);
        if(source == this.add_window_b) jTraverser.tree.addNode(NodeInfo.USAGE_WINDOW);
        if(source == this.add_axis_b) jTraverser.tree.addNode(NodeInfo.USAGE_AXIS);
        if(source == this.add_subtree_b) jTraverser.tree.addSubtree();
        if(source == this.delete_node_b) Tree.deleteNode();
        if(source == this.modify_tags_b) jTraverser.tree.modifyTags();
        if(source == this.rename_node_b) Tree.dialogs.rename.show();
        if(source == this.add_device_b) jTraverser.tree.addDevice();
        if(source == this.copy_b) TreeNode.copy();
        if(source == this.paste_b) TreeNode.paste();
        // Node related
        final Node currnode = Tree.getCurrentNode();
        if(currnode == null) return;
        if(source == this.turn_on_b){
            currnode.turnOn();
            Tree.reportChange();
        }
        if(source == this.turn_off_b){
            currnode.turnOff();
            Tree.reportChange();
        }
        if(source == this.display_data_b){
            if(this.display_data_d == null){
                this.display_data_d = new TreeDialog(this.display_data = new DisplayData());
                this.display_data.setFrame(this.display_data_d);
            }
            this.display_data.setNode(currnode);
            this.display_data_d.pack();
            this.display_data_d.setLocation(this.dialogLocation());
            this.display_data_d.setVisible(true);
        }
        if(source == this.display_nci_b){
            if(this.display_nci_d == null){
                this.display_nci_d = new TreeDialog(this.display_nci = new DisplayNci());
                this.display_nci.setFrame(this.display_nci_d);
            }
            this.display_nci.setNode(currnode);
            this.display_nci_d.pack();
            this.display_nci_d.setLocation(this.dialogLocation());
            this.display_nci_d.setVisible(true);
        }
        if(source == this.display_tags_b){
            if(this.display_tags_d == null){
                this.display_tags_d = new TreeDialog(this.display_tags = new DisplayTags());
                this.display_tags.setFrame(this.display_tags_d);
            }
            this.display_tags.setNode(currnode);
            this.display_tags_d.pack();
            this.display_tags_d.setLocation(this.dialogLocation());
            this.display_tags_d.setVisible(true);
        }
        if(source == this.modify_data_b){
            if(this.modify_data_d == null){
                this.modify_data_d = new TreeDialog(this.modify_data = new ModifyData());
                this.modify_data.setFrame(this.modify_data_d);
            }
            this.modify_data.setNode(currnode);
            this.modify_data_d.pack();
            this.modify_data_d.setLocation(this.dialogLocation());
            this.modify_data_d.setVisible(true);
        }
        if(source == this.set_default_b){
            try{
                currnode.setDefault();
            }catch(final Exception exc){
                jTraverser.stderr("Error setting default", exc);
            }
            Tree.reportChange();
        }
        if(source == this.setup_device_b) currnode.setupDevice();
    }

    @Override
    public Component add(final Component component) {
        return this.getContentPane().add(component);
    }

    public Point dialogLocation() {
        return new Point(this.getLocation().x + 32, this.getLocation().y + 32);
    }

    void reportChange(final String exp, final int shot, final boolean editable, final boolean readonly) {
        jTraverser.model = shot < 0;
        jTraverser.editable = editable;
        jTraverser.readonly = readonly;
        jTraverser.exp_name = exp;
        String title;
        if(exp != null){
            title = "Tree: " + exp;
            if(editable) title = title + " (edit) ";
            if(readonly) title = title + " (readonly) ";
            title = title + " Shot: " + shot;
            jTraverser.stdout(title);
        }else{
            title = "no tree open";
            this.edit_m.setEnabled(false);
            this.data_m.setEnabled(false);
        }
        this.setTitle("jTraverser - " + title);
        if(exp == null) return;
        this.data_m.setEnabled(true);
        if(editable){
            this.edit_m.setEnabled(true);
            this.modify_data_b.setEnabled(true);
            this.turn_on_b.setEnabled(true);
            this.turn_off_b.setEnabled(true);
        }
        if(readonly){
            this.edit_m.setEnabled(false);
            this.modify_data_b.setEnabled(false);
            this.turn_on_b.setEnabled(false);
            this.turn_off_b.setEnabled(false);
        }
        if(!editable && !readonly){
            this.edit_m.setEnabled(false);
            this.modify_data_b.setEnabled(true);
            this.turn_on_b.setEnabled(true);
            this.turn_off_b.setEnabled(true);
        }
    }
}
