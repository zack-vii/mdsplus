// package jTraverser;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.MethodDescriptor;
import java.beans.PropertyDescriptor;
import java.beans.PropertyEditor;
import java.io.FileDescriptor;
import java.rmi.Naming;
import java.util.Stack;
import java.util.Vector;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.ToolTipManager;
import javax.swing.TransferHandler;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

public class Tree extends JScrollPane implements TreeSelectionListener, MouseListener, ActionListener, KeyListener, DataChangeListener{
    static class dialogs{
        static class flags{
            private static JButton     close_b;
            private static JDialog     dialog;
            private static JCheckBox[] flag;
            private static boolean[]   settable_flag;
            private static JButton     update_b;

            public static void close() {
                if(flags.dialog != null) flags.dialog.setVisible(false);
            }

            private static void construct() {
                flags.dialog = new JDialog(Tree.frame);
                flags.dialog.setFocusableWindowState(false);
                final JPanel jp = new JPanel();
                jp.setLayout(new BorderLayout());
                final JPanel jp1 = new JPanel();
                jp1.setLayout(new GridLayout(8, 4));
                flags.flag = new JCheckBox[32];
                jp1.add(flags.flag[13] = new JCheckBox("PathReference"));
                jp1.add(flags.flag[14] = new JCheckBox("NidReference"));
                jp1.add(flags.flag[5] = new JCheckBox("Segmented"));
                jp1.add(flags.flag[8] = new JCheckBox("Compressible"));
                jp1.add(flags.flag[1] = new JCheckBox("ParentOff"));
                jp1.add(flags.flag[4] = new JCheckBox("Versions"));
                jp1.add(flags.flag[16] = new JCheckBox("CompressSegments"));
                jp1.add(flags.flag[9] = new JCheckBox("DoNotCompress"));
                jp1.add(flags.flag[0] = new JCheckBox("Off"));
                jp1.add(flags.flag[6] = new JCheckBox("Setup"));
                jp1.add(flags.flag[2] = new JCheckBox("Essential"));
                jp1.add(flags.flag[10] = new JCheckBox("CompressOnPut"));
                jp1.add(flags.flag[11] = new JCheckBox("NoWriteModel"));
                jp1.add(flags.flag[12] = new JCheckBox("NoWriteShot"));
                jp1.add(flags.flag[7] = new JCheckBox("WriteOnce"));
                jp1.add(flags.flag[15] = new JCheckBox("IncludeInPulse"));
                jp1.add(flags.flag[3] = new JCheckBox("Cached"));
                for(byte i = 17; i < 31; i++)
                    jp1.add(flags.flag[i] = new JCheckBox("UndefinedFlag" + (i)));
                jp1.add(flags.flag[31] = new JCheckBox("Error"));
                flags.settable_flag = new boolean[]{true, false, true, true, false, false, true, true, false, true, true, true, true, false, false, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, false};
                flags.flag[0].addActionListener(new ActionListener(){
                    @Override
                    public void actionPerformed(final ActionEvent e) {
                        final Node currnode = Tree.getCurrentNode();
                        if(currnode == null) return;
                        if(flags.flag[0].isSelected()) currnode.turnOff();
                        else currnode.turnOn();
                        Tree.reportChange();
                    }
                });
                for(byte i = 1; i < 32; i++)
                    if(flags.flag[i] != null){
                        final byte ii = i;
                        flags.flag[i].addActionListener(new ActionListener(){
                            @Override
                            public void actionPerformed(final ActionEvent e) {
                                flags.editFlag(ii);
                                Tree.reportChange();
                            }
                        });
                    }
                jp.add(jp1);
                final JPanel jp3 = new JPanel();
                jp3.setLayout(new GridLayout(1, 2));
                jp3.add(flags.close_b = new JButton("Close"));
                flags.close_b.addActionListener(new ActionListener(){
                    @Override
                    public void actionPerformed(final ActionEvent e) {
                        flags.close();
                    }
                });
                jp3.add(flags.update_b = new JButton("Refresh"));
                flags.update_b.addActionListener(new ActionListener(){
                    @Override
                    public void actionPerformed(final ActionEvent e) {
                        flags.show();
                    }
                });
                jp.add(jp3, "South");
                flags.dialog.getContentPane().add(jp);
                flags.dialog.addKeyListener(new KeyAdapter(){
                    @Override
                    public void keyTyped(final KeyEvent e) {
                        if(e.getKeyCode() == KeyEvent.VK_ESCAPE) flags.dialog.setVisible(false);
                    }
                });
                flags.dialog.pack();
                flags.dialog.setResizable(false);
            }

            private static void editFlag(final byte idx) {
                final Node currnode = Tree.getCurrentNode();
                if(currnode == null) return;
                if(flags.flag[idx].isSelected()) try{
                    currnode.setFlag(idx);
                }catch(final Exception exc){
                    JOptionPane.showMessageDialog(Tree.frame, exc.getMessage(), "Error setting flag" + idx, JOptionPane.WARNING_MESSAGE);
                }
                else try{
                    currnode.clearFlag(idx);
                }catch(final Exception exc){
                    JOptionPane.showMessageDialog(Tree.frame, exc.getMessage(), "Error clearing flag " + idx, JOptionPane.WARNING_MESSAGE);
                }
                flags.show();
            }

            private static boolean[] readFlags() throws Exception {
                int iflags = 0;
                final boolean[] bflags = new boolean[32];
                final Node currnode = Tree.getCurrentNode();
                if(currnode == null){
                    bflags[31] = true;
                    return bflags;
                }
                iflags = currnode.getFlags();
                if(iflags < 0) jTraverser.stderr("MdsJava returned -1.", null);
                for(byte i = 0; i < 32; i++)
                    bflags[i] = (iflags & (1 << i)) != 0;
                return bflags;
            }

            public static void show() {
                if(flags.dialog == null) flags.construct();
                boolean[] bflags;
                try{
                    bflags = flags.readFlags();
                }catch(final Exception exc){
                    jTraverser.stderr("Error getting flags", exc);
                    flags.close();
                    return;
                }
                final Node currnode = Tree.getCurrentNode();
                final boolean is_ok = !(jTraverser.readonly || (currnode == null));
                for(int i = 0; i < 32; i++){
                    flags.flag[i].setSelected(bflags[i]);
                    flags.flag[i].setEnabled(is_ok && flags.settable_flag[i]);
                }
                if(currnode == null) flags.dialog.setTitle("Flags of <none selected>");
                else flags.dialog.setTitle("Flags of " + currnode.getFullPath());
                if(!flags.dialog.isVisible()){
                    flags.dialog.setLocation(Tree.frame.dialogLocation());
                    flags.dialog.setVisible(true);
                }
            }

            public static void update() {
                if(flags.dialog == null) return;
                if(!flags.dialog.isVisible()) return;
                flags.show();
            }
        }
        static class rename{
            private static JDialog    dialog;
            private static JTextField new_name;

            public static void close() {
                if(rename.dialog != null) rename.dialog.setVisible(false);
            }

            private static void commit() {
                final Node currnode = Tree.getCurrentNode();
                if(currnode == null) return;
                final String name = rename.new_name.getText();
                if(name == null || name.length() == 0) return;
                try{
                    currnode.rename(name);
                }catch(final Exception exc){
                    JOptionPane.showMessageDialog(Tree.frame, exc.getMessage(), "Error renaming Node", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                Tree.curr_tree.treeDidChange();
                rename.dialog.setVisible(false);
            }

            private static void construct() {
                rename.dialog = new JDialog();
                final JPanel mjp = new JPanel();
                mjp.setLayout(new BorderLayout());
                JPanel jp = new JPanel();
                jp.add(new JLabel("New Name: "));
                jp.add(rename.new_name = new JTextField(12));
                mjp.add(jp, "North");
                jp = new JPanel();
                final JButton ok_b = new JButton("Ok");
                ok_b.addActionListener(new ActionListener(){
                    @Override
                    public void actionPerformed(final ActionEvent e) {
                        rename.commit();
                    }
                });
                jp.add(ok_b);
                final JButton cancel_b = new JButton("Cancel");
                cancel_b.addActionListener(new ActionListener(){
                    @Override
                    public void actionPerformed(final ActionEvent e) {
                        rename.dialog.setVisible(false);
                    }
                });
                jp.add(cancel_b);
                mjp.add(jp, "South");
                rename.dialog.getContentPane().add(mjp);
                rename.dialog.addKeyListener(new KeyAdapter(){
                    @Override
                    public void keyTyped(final KeyEvent e) {
                        if(e.getKeyCode() == KeyEvent.VK_ENTER) rename.commit();
                    }
                });
                rename.dialog.pack();
                rename.dialog.setResizable(false);
            }

            public static void show() {
                final Node currnode = Tree.getCurrentNode();
                if(currnode == null) return;
                if(rename.dialog == null) rename.construct();
                rename.dialog.setTitle("Rename node " + currnode.getFullPath());
                rename.dialog.setLocation(Tree.frame.dialogLocation());
                rename.new_name.setText("");
                rename.dialog.setVisible(true);
            }

            public static void update() {// don't update; close instead
                rename.close();
            }
        }

        static void update() {
            flags.update();
            rename.update();
        }
    }
    class DialogSet{
        Vector<TreeDialog> dialogs = new Vector<TreeDialog>();

        TreeDialog getDialog(final Class ed_class, final Node node) {
            int idx;
            TreeDialog curr_dialog = null;
            NodeEditor curr_editor;
            for(idx = 0; idx < this.dialogs.size(); idx++){
                curr_dialog = this.dialogs.elementAt(idx);
                if(!curr_dialog.inUse()) break;
            }
            if(curr_dialog == null){
                try{
                    curr_editor = (NodeEditor)((PropertyEditor)ed_class.newInstance()).getCustomEditor();
                    curr_dialog = new TreeDialog(curr_editor);
                    curr_editor.setFrame(curr_dialog);
                    this.dialogs.addElement(curr_dialog);
                }catch(final Exception exc){
                    jTraverser.stderr("Error creating node editor", exc);
                    return null;
                }
            }
            curr_dialog.setUsed(true);
            curr_dialog.getEditor().setNode(node);
            return curr_dialog;
        }
    }
    // Inner class FromTranferHandler managed drag operation
    class FromTransferHandler extends TransferHandler{
        private static final long serialVersionUID = -8913781547508869425L;

        @Override
        public Transferable createTransferable(final JComponent comp) {
            if(Tree.curr_tree == null) return null;
            try{
                return new StringSelection(Tree.this.topExperiment + ":" + Tree.getCurrentNode().getFullPath());
            }catch(final Exception exc){
                return null;
            }
        }

        @Override
        public int getSourceActions(final JComponent comp) {
            return TransferHandler.COPY_OR_MOVE;
        }
    }
    class MDSCellRenderer extends DefaultTreeCellRenderer{
        private static final long serialVersionUID = -6705089616475773220L;

        @Override
        public Component getTreeCellRendererComponent(final JTree tree, final Object value, final boolean isSelected, final boolean expanded, final boolean hasFocus, final int row, final boolean leaf) {
            final Object usrObj = ((DefaultMutableTreeNode)value).getUserObject();
            Node node;
            if(usrObj instanceof String){
                node = Tree.curr_node;
                if(node.getTreeNode() == value){
                    final String newName = (((String)usrObj).trim()).toUpperCase();
                    if(Tree.this.lastName == null || !Tree.this.lastName.equals(newName)){
                        Tree.this.lastName = newName;
                        node.rename(newName);
                    }
                    node.getTreeNode().setUserObject(node);
                }
            }else node = (Node)usrObj;
            if(isSelected) Tree.dialogs.update();
            return node.getIcon(isSelected);
        }
    }
    static int                context;
    static int                curr_dialog_idx;
    static RemoteTree         curr_experiment;
    static Node               curr_node;
    static JTree              curr_tree;
    static jTraverser         frame;
    static boolean            is_remote;
    private static final long serialVersionUID = 8170809028459276731L;

    public static Node addDevice(final String name, final String type) {
        return Tree.addDevice(name, type, Tree.getCurrentNode());
    }

    public static Node addDevice(final String name, final String type, final Node toNode) {
        DefaultMutableTreeNode new_tree_node = null;
        if(name == null || name.length() == 0 || name.length() > 12){
            JOptionPane.showMessageDialog(Tree.frame, "Name length must range between 1 and 12 characters", "Error adding Node", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        if(type == null || type.length() == 0){
            JOptionPane.showMessageDialog(Tree.frame, "Missing device type", "Error adding Node", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        Node new_node = null;
        try{
            new_node = toNode.addDevice(name, type);
            final int num_children = toNode.getTreeNode().getChildCount();
            int i;
            if(num_children > 0){
                String curr_name;
                for(i = 0; i < num_children; i++){
                    curr_name = Tree.getNode(toNode.getTreeNode().getChildAt(i)).getName();
                    if(name.compareTo(curr_name) < 0) break;
                }
                new_node.setTreeNode(new_tree_node = new DefaultMutableTreeNode(new_node));
                final DefaultTreeModel tree_model = (DefaultTreeModel)Tree.curr_tree.getModel();
                tree_model.insertNodeInto(new_tree_node, Tree.getCurrTreeNode(), i);
                Tree.curr_tree.makeVisible(new TreePath(new_tree_node.getPath()));
                return new_node;
            }
        }catch(final Throwable e){
            JOptionPane.showMessageDialog(Tree.frame, "Add routine for the selected device cannot be activated:\n" + e.getMessage(), "Error adding Device", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return new_node;
    }

    public static Node addNode(final int usage, final String name) {
        return Tree.addNode(usage, name, Tree.getCurrentNode());
    }

    public static Node addNode(final int usage, final String name, final Node toNode) {
        Node new_node;
        DefaultMutableTreeNode new_tree_node;
        final DefaultMutableTreeNode toTreeNode = toNode.getTreeNode();
        if(name == null || name.length() == 0 || name.length() > 12){
            JOptionPane.showMessageDialog(Tree.frame, "Name length must range between 1 and 12 characters", "Error adding Node", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        try{
            new_node = toNode.addNode(usage, name);
            new_tree_node = new DefaultMutableTreeNode(new_node);
            new_node.setTreeNode(new_tree_node);
            Tree.addNodeToParent(new_tree_node, toTreeNode);
        }catch(final Exception e){
            JOptionPane.showMessageDialog(Tree.frame, e.getMessage(), "Error adding Node", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return new_node;
    }

    static void addNodeToParent(final DefaultMutableTreeNode TreeNode, final DefaultMutableTreeNode toTreeNode) {
        final int num_children = toTreeNode.getChildCount();
        int i = 0;
        if(num_children > 0){
            final String name = Tree.getNode(TreeNode).getName();
            String curr_name;
            for(i = 0; i < num_children; i++){
                curr_name = ((Node)((DefaultMutableTreeNode)toTreeNode.getChildAt(i)).getUserObject()).getName();
                if(name.compareTo(curr_name) < 0) break;
            }
        }
        final DefaultTreeModel tree_model = (DefaultTreeModel)Tree.curr_tree.getModel();
        tree_model.insertNodeInto(TreeNode, toTreeNode, i);
        Tree.curr_tree.expandPath(new TreePath(TreeNode.getPath()));
        Tree.curr_tree.treeDidChange();
    }

    static Data dataFromExpr(final String expr) {
        if(Tree.is_remote) try{
            return Tree.curr_experiment.dataFromExpr(expr);
        }catch(final Exception exc){
            return null;
        }
        else return Data.fromExpr(expr);
    }

    static String dataToString(final Data data) {
        if(Tree.is_remote) try{
            return Tree.curr_experiment.dataToString(data);
        }catch(final Exception exc){
            return exc.toString();
        }
        else return data.toString();
    }

    static void deleteNode() {
        Tree.deleteNode(Tree.getCurrentNode());
        Tree.curr_node = null;
    }

    static void deleteNode(final Node delNode) {
        if(delNode == null) return;
        final Node del_node = delNode;
        final int n_children = del_node.startDelete();
        String msg = "You are about to delete node " + del_node.getName().trim();
        if(n_children > 0) msg += " which has " + n_children + " descendents.\n Please confirm";
        else msg += "\n Please confirm";
        final int n = JOptionPane.showConfirmDialog(Tree.frame, msg, "Delete node(s)", JOptionPane.YES_NO_OPTION);
        if(n == JOptionPane.YES_OPTION){
            final DefaultTreeModel tree_model = (DefaultTreeModel)Tree.curr_tree.getModel();
            tree_model.removeNodeFromParent(delNode.getTreeNode());
            del_node.executeDelete();
        }
    }

    public static Node getCurrentNode() {
        if(Tree.curr_tree == null) return null;
        final Object usrObj = ((DefaultMutableTreeNode)Tree.curr_tree.getLastSelectedPathComponent()).getUserObject();
        if(!(usrObj instanceof Node)) return null;
        Tree.curr_node = (Node)usrObj;
        return Tree.curr_node;
    }

    public static DefaultMutableTreeNode getCurrTreeNode() {
        return Tree.getCurrentNode().getTreeNode();
    }

    public static Node getNode(final DefaultMutableTreeNode treenode) {
        return (Node)treenode.getUserObject();
    }

    public static Node getNode(final javax.swing.tree.TreeNode treenode) {
        return Tree.getNode((DefaultMutableTreeNode)treenode);
    }

    public static boolean isRemote() {
        return Tree.is_remote;
    }

    public static void reportChange() {
        if(Tree.curr_tree != null) Tree.curr_tree.treeDidChange();
        dialogs.update();
    }

    public static void setCurrTreeNode(final DefaultMutableTreeNode treenode) {
        Tree.curr_node = Tree.getNode(treenode);
    }
    // Temporary, to overcome Java's bugs on inner classes
    JMenuItem                        add_action_b, add_dispatch_b, add_numeric_b, add_signal_b, add_task_b, add_text_b, add_window_b, add_axis_b, add_device_b, add_child_b, add_subtree_b, delete_node_b, modify_tags_b;
    private JDialog                  add_device_dialog;
    private JTextField               add_device_type, add_device_name;
    private JTextField               add_node_name, add_node_tag, add_subtree_name;
    private int                      add_node_usage;
    private JTextField               curr_tag_selection;
    private DefaultListModel<String> curr_taglist_model;
    private DialogSet                dialog_sets[];
    private final Stack<RemoteTree>  experiments;
    boolean                          is_angled_style;
    private String                   lastName;
    private JMenuItem                menu_items[];
    private JDialog                  modify_tags_dialog;
    private JList<String>            modify_tags_list;
    JButton                          ok_cb, add_node_ok;
    private JDialog                  open_dialog, add_node_dialog, add_subtree_dialog;
    private JTextField               open_exp, open_shot;
    private JRadioButton             open_readonly, open_edit, open_normal;
    private JPopupMenu               pop;
    private String[]                 tags;
    private DefaultMutableTreeNode   top;
    private String                   topExperiment;
    private final Stack<JTree>       trees;

    public Tree(final JFrame _frame){
        this((jTraverser)_frame);
    }

    public Tree(final jTraverser _frame){
        Tree.frame = _frame;
        this.trees = new Stack<JTree>();
        this.experiments = new Stack<RemoteTree>();
        this.setPreferredSize(new Dimension(300, 400));
        this.setBackground(Color.white);
        Tree.curr_tree = null;
        Tree.curr_experiment = null;
        final String def_tree = System.getProperty("tree");
        if(def_tree != null){
            final String def_shot = System.getProperty("shot");
            int shot;
            if(def_shot != null) shot = Integer.parseInt(def_shot);
            else shot = -1;
            this.open(def_tree, shot, false, false, false);
        }
    }

    // temporary: to overcome java's bugs for inner classes
    @Override
    public void actionPerformed(final ActionEvent e) {
        final Object jb = e.getSource();
        if(jb == this.ok_cb) this.open_ok();
        if(jb == this.add_action_b) this.addNode(NodeInfo.USAGE_ACTION);
        if(jb == this.add_dispatch_b) this.addNode(NodeInfo.USAGE_DISPATCH);
        if(jb == this.add_numeric_b) this.addNode(NodeInfo.USAGE_NUMERIC);
        if(jb == this.add_signal_b) this.addNode(NodeInfo.USAGE_SIGNAL);
        if(jb == this.add_task_b) this.addNode(NodeInfo.USAGE_TASK);
        if(jb == this.add_text_b) this.addNode(NodeInfo.USAGE_TEXT);
        if(jb == this.add_window_b) this.addNode(NodeInfo.USAGE_WINDOW);
        if(jb == this.add_axis_b) this.addNode(NodeInfo.USAGE_AXIS);
        if(jb == this.add_node_ok) this.addNode();
        if(jb == this.add_child_b) this.addNode(NodeInfo.USAGE_STRUCTURE);
        if(jb == this.add_subtree_b) this.addSubtree();
        if(jb == this.add_device_b) this.addDevice();
        if(jb == this.delete_node_b) Tree.deleteNode();
        if(jb == this.modify_tags_b) this.modifyTags();
    }

    public void addDevice() {
        final Node currnode = Tree.getCurrentNode();
        if(currnode == null) return;
        if(this.add_device_dialog == null){
            this.add_device_dialog = new JDialog(Tree.frame);
            final JPanel jp = new JPanel();
            jp.setLayout(new BorderLayout());
            JPanel jp1 = new JPanel();
            jp1.add(new JLabel("Device: "));
            jp1.add(this.add_device_type = new JTextField(12));
            jp.add(jp1, "North");
            jp1 = new JPanel();
            jp1.add(new JLabel("Name:   "));
            jp1.add(this.add_device_name = new JTextField(12));
            jp.add(jp1, "South");
            jp1 = new JPanel();
            JButton ok_button;
            jp1.add(ok_button = new JButton("Ok"));
            ok_button.addActionListener(new ActionListener(){
                @Override
                public void actionPerformed(final ActionEvent e) {
                    if(Tree.addDevice(Tree.this.add_device_name.getText().toUpperCase(), Tree.this.add_device_type.getText()) == null) ;
                    Tree.this.add_device_dialog.setVisible(false);
                }
            });
            final JButton cancel_b = new JButton("Cancel");
            cancel_b.addActionListener(new ActionListener(){
                @Override
                public void actionPerformed(final ActionEvent e) {
                    Tree.this.add_device_dialog.setVisible(false);
                }
            });
            jp1.add(cancel_b);
            JPanel jp2;
            jp2 = new JPanel();
            jp2.setLayout(new BorderLayout());
            jp2.add(jp, "North");
            jp2.add(jp1, "South");
            this.add_device_dialog.getContentPane().add(jp2);
            this.add_device_dialog.addKeyListener(new KeyAdapter(){
                @Override
                public void keyTyped(final KeyEvent e) {
                    if(e.getKeyCode() == KeyEvent.VK_ENTER) if(Tree.addDevice(Tree.this.add_device_name.getText().toUpperCase(), Tree.this.add_device_type.getText()) == null) ;
                    Tree.this.add_device_dialog.setVisible(false);
                }
            });
            this.add_device_dialog.pack();
        }
        this.add_device_name.setText("");
        this.add_device_type.setText("");
        this.add_device_dialog.setTitle("Add device to: " + currnode.getFullPath());
        this.add_device_dialog.setLocation(Tree.frame.dialogLocation());
        this.add_device_dialog.setVisible(true);
    }

    public void addNode() {
        final Node newNode = Tree.addNode(this.add_node_usage, this.add_node_name.getText().toUpperCase());
        if(!this.add_node_tag.getText().trim().equals("")){
            try{
                newNode.setTags(new String[]{this.add_node_tag.getText().trim().toUpperCase()});
            }catch(final Exception exc){
                jTraverser.stderr("Error adding tag", exc);
            }
        }
        this.add_node_dialog.setVisible(false);
    }

    public void addNode(final int usage) {
        final Node currnode = Tree.getCurrentNode();
        if(currnode == null) return;
        this.add_node_usage = usage;
        if(this.add_node_dialog == null){
            this.add_node_dialog = new JDialog(Tree.frame);
            final JPanel jp = new JPanel();
            jp.setLayout(new BorderLayout());
            JPanel jp1 = new JPanel();
            jp1.add(new JLabel("Node name: "));
            jp1.add(this.add_node_name = new JTextField(12));
            jp.add(jp1, "North");
            jp1 = new JPanel();
            jp1.add(new JLabel("Node tag: "));
            jp1.add(this.add_node_tag = new JTextField(12));
            jp.add(jp1, "Center");
            jp1 = new JPanel();
            jp1.add(this.add_node_ok = new JButton("Ok"));
            this.add_node_ok.addActionListener(this);
            final JButton cancel_b = new JButton("Cancel");
            cancel_b.addActionListener(new ActionListener(){
                @Override
                public void actionPerformed(final ActionEvent e) {
                    Tree.this.add_node_dialog.setVisible(false);
                }
            });
            jp1.add(cancel_b);
            jp.add(jp1, "South");
            this.add_node_dialog.getContentPane().add(jp);
            this.add_node_dialog.addKeyListener(new KeyAdapter(){
                @Override
                public void keyTyped(final KeyEvent e) {
                    if(e.getKeyCode() == KeyEvent.VK_ENTER) Tree.this.addNode();
                }
            });
            this.add_node_dialog.pack();
            this.add_node_dialog.setVisible(true);
        }
        this.add_node_name.setText("");
        this.add_node_tag.setText("");
        this.add_node_dialog.setTitle("Add to: " + currnode.getFullPath());
        this.add_node_dialog.setLocation(Tree.frame.dialogLocation());
        this.add_node_dialog.setVisible(true);
    }

    public void addSubtree() {
        final Node currnode = Tree.getCurrentNode();
        if(currnode == null) return;
        if(this.add_subtree_dialog == null){
            this.add_subtree_dialog = new JDialog(Tree.frame);
            final JPanel jp = new JPanel();
            jp.setLayout(new BorderLayout());
            JPanel jp1 = new JPanel();
            jp1.add(new JLabel("Node name: "));
            jp1.add(this.add_subtree_name = new JTextField(12));
            jp.add(jp1, "North");
            jp1 = new JPanel();
            final JButton ok = new JButton("Ok");
            ok.addActionListener(new ActionListener(){
                @Override
                public void actionPerformed(final ActionEvent e) {
                    final Node node = Tree.addNode(NodeInfo.USAGE_STRUCTURE, "." + Tree.this.add_subtree_name.getText().toUpperCase());
                    try{
                        node.setSubtree();
                    }catch(final Exception exc){
                        jTraverser.stderr("Error setting subtree", exc);
                    }
                    Tree.this.add_subtree_dialog.setVisible(false);
                }
            });
            jp1.add(ok);
            final JButton cancel_b = new JButton("Cancel");
            cancel_b.addActionListener(new ActionListener(){
                @Override
                public void actionPerformed(final ActionEvent e) {
                    Tree.this.add_subtree_dialog.setVisible(false);
                }
            });
            jp1.add(cancel_b);
            jp.add(jp1, "South");
            this.add_subtree_dialog.getContentPane().add(jp);
            this.add_subtree_dialog.pack();
            this.add_subtree_dialog.setVisible(true);
        }
        this.add_subtree_dialog.setTitle("Add Subtree to: " + currnode.getFullPath());
        this.add_subtree_dialog.setLocation(Tree.frame.dialogLocation());
        this.add_subtree_dialog.setVisible(true);
    }

    public void addTag() {
        final String[] out_tags = new String[this.curr_taglist_model.getSize()];
        for(int i = 0; i < this.curr_taglist_model.getSize(); i++){
            out_tags[i] = this.curr_taglist_model.getElementAt(i);
        }
        try{
            Tree.getCurrentNode().setTags(out_tags);
        }catch(final Exception exc){
            JOptionPane.showMessageDialog(Tree.frame, exc.getMessage(), "Error adding tags", JOptionPane.WARNING_MESSAGE);
        }
        this.modify_tags_dialog.setVisible(false);
    }

    void close() {
        if(Tree.curr_tree == null) return;
        try{
            Tree.curr_experiment.close(Tree.context);
        }catch(final Exception e){
            boolean editable = false;
            String name = null;
            try{
                editable = Tree.curr_experiment.isEditable();
                name = Tree.curr_experiment.getName().trim();
            }catch(final Exception exc){}
            if(editable){
                final int n = JOptionPane.showConfirmDialog(Tree.frame, "Tree " + name + " open in edit mode has been changed: Write it before closing?", "Closing Tree ", JOptionPane.YES_NO_OPTION);
                if(n == JOptionPane.YES_OPTION){
                    try{
                        Tree.curr_experiment.write(Tree.context);
                        Tree.curr_experiment.close(Tree.context);
                    }catch(final Exception exc){
                        JOptionPane.showMessageDialog(Tree.frame, "Error closing tree", exc.getMessage(), JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                }else{
                    try{
                        Tree.curr_experiment.quit(Tree.context);
                    }catch(final Exception exce){
                        JOptionPane.showMessageDialog(Tree.frame, "Error quitting tree", exce.getMessage(), JOptionPane.WARNING_MESSAGE);
                    }
                }
            }else{
                JOptionPane.showMessageDialog(Tree.frame, "Error closing tree", e.getMessage(), JOptionPane.WARNING_MESSAGE);
                return;
            }
        }
        this.trees.pop();
        this.experiments.pop();
        if(!this.trees.empty()){
            Tree.curr_tree = this.trees.peek();
            Tree.curr_experiment = this.experiments.peek();
            this.setViewportView(Tree.curr_tree);
            try{
                Tree.frame.reportChange(Tree.curr_experiment.getName(), Tree.curr_experiment.getShot(), Tree.curr_experiment.isEditable(), Tree.curr_experiment.isReadonly());
                if(jTraverser.editable != Tree.curr_experiment.isEditable()) this.pop = null;
                jTraverser.editable = Tree.curr_experiment.isEditable();
            }catch(final Exception exc){
                jTraverser.stderr("Error in RMI communication", exc);
            }
        }else{
            Tree.curr_tree = null;
            Tree.curr_experiment = null;
            this.setViewportView(new JPanel());
            Tree.frame.reportChange(null, 0, false, false);
        }
        DeviceSetup.closeOpenDevices();
        Tree.curr_node = null;
        dialogs.update();
        Tree.frame.pack();
        this.repaint();
    }

    @Override
    public void dataChanged(final DataChangeEvent e) {
        Tree.reportChange();
    }

    @Override
    public void keyPressed(final KeyEvent e) {}

    @Override
    public void keyReleased(final KeyEvent e) {}

    @Override
    public void keyTyped(final KeyEvent e) {}

    public void modifyTags() {
        final Node currnode = Tree.getCurrentNode();
        if(currnode == null) return;
        try{
            this.tags = currnode.getTags();
        }catch(final Exception exc){
            jTraverser.stderr("Error getting tags", exc);
            this.tags = new String[0];
        }
        this.curr_taglist_model = new DefaultListModel<String>();
        for(final String tag : this.tags){
            this.curr_taglist_model.addElement(tag);
        }
        if(this.modify_tags_dialog == null){
            this.modify_tags_dialog = new JDialog(Tree.frame);
            final JPanel jp = new JPanel();
            jp.setLayout(new BorderLayout());
            final JPanel jp1 = new JPanel();
            jp1.setLayout(new BorderLayout());
            this.modify_tags_list = new JList<String>();
            this.modify_tags_list.addListSelectionListener(new ListSelectionListener(){
                @Override
                public void valueChanged(final ListSelectionEvent e) {
                    final int idx = Tree.this.modify_tags_list.getSelectedIndex();
                    if(idx != -1) Tree.this.curr_tag_selection.setText(Tree.this.curr_taglist_model.getElementAt(idx));
                }
            });
            final JScrollPane scroll_list = new JScrollPane(this.modify_tags_list);
            jp1.add(new JLabel("Tag List:"), "North");
            jp1.add(scroll_list, "Center");
            final JPanel jp2 = new JPanel();
            jp2.setLayout(new GridLayout(2, 1));
            final JButton add_tag = new JButton("Add Tag");
            add_tag.addActionListener(new ActionListener(){
                @Override
                public void actionPerformed(final ActionEvent e) {
                    final String curr_tag = Tree.this.curr_tag_selection.getText().toUpperCase();
                    if(curr_tag == null || curr_tag.length() == 0) return;
                    for(int i = 0; i < Tree.this.curr_taglist_model.getSize(); i++)
                        if(curr_tag.equals(Tree.this.curr_taglist_model.getElementAt(i))) return;
                    Tree.this.curr_taglist_model.addElement(curr_tag);
                }
            });
            jp2.add(add_tag);
            final JButton remove_tag = new JButton("Remove Tag");
            remove_tag.addActionListener(new ActionListener(){
                @Override
                public void actionPerformed(final ActionEvent e) {
                    int idx;
                    if((idx = Tree.this.modify_tags_list.getSelectedIndex()) != -1){
                        Tree.this.curr_taglist_model.removeElementAt(idx);
                    }
                }
            });
            jp2.add(remove_tag);
            final JPanel jp4 = new JPanel();
            jp4.add(jp2);
            jp1.add(jp4, "East");
            this.curr_tag_selection = new JTextField(30);
            final JPanel jp5 = new JPanel();
            jp5.add(new JLabel("Current Selection: "));
            jp5.add(this.curr_tag_selection);
            jp1.add(jp5, "South");
            jp.add(jp1, "North");
            final JPanel jp3 = new JPanel();
            final JButton ok_b = new JButton("Ok");
            ok_b.addActionListener(new ActionListener(){
                @Override
                public void actionPerformed(final ActionEvent e) {
                    Tree.this.addTag();
                }
            });
            jp3.add(ok_b);
            final JButton reset_b = new JButton("Reset");
            reset_b.addActionListener(new ActionListener(){
                @Override
                public void actionPerformed(final ActionEvent e) {
                    Tree.this.curr_taglist_model = new DefaultListModel<String>();
                    for(final String tag : Tree.this.tags)
                        Tree.this.curr_taglist_model.addElement(tag);
                    Tree.this.modify_tags_list.setModel(Tree.this.curr_taglist_model);
                }
            });
            jp3.add(reset_b);
            final JButton cancel_b = new JButton("Cancel");
            cancel_b.addActionListener(new ActionListener(){
                @Override
                public void actionPerformed(final ActionEvent e) {
                    Tree.this.modify_tags_dialog.setVisible(false);
                }
            });
            jp3.add(cancel_b);
            jp.add(jp3, "South");
            this.modify_tags_dialog.getContentPane().add(jp);
            this.modify_tags_dialog.addKeyListener(new KeyAdapter(){
                @Override
                public void keyTyped(final KeyEvent e) {
                    if(e.getKeyCode() == KeyEvent.VK_ENTER) Tree.this.addTag();
                }
            });
            this.modify_tags_dialog.pack();
            this.modify_tags_dialog.setVisible(true);
        }
        this.modify_tags_dialog.setTitle("Modify tags of " + currnode.getFullPath());
        this.modify_tags_list.setModel(this.curr_taglist_model);
        this.curr_tag_selection.setText("");
        this.modify_tags_dialog.setLocation(Tree.frame.dialogLocation());
        this.modify_tags_dialog.setVisible(true);
    }

    @Override
    public void mouseClicked(final MouseEvent e) {}

    @Override
    public void mouseEntered(final MouseEvent e) {}

    @Override
    public void mouseExited(final MouseEvent e) {}

    @Override
    public void mousePressed(final MouseEvent e) {
        if(Tree.curr_tree == null) return;
        int item_idx;
        final DefaultMutableTreeNode curr_tree_node = (DefaultMutableTreeNode)Tree.curr_tree.getClosestPathForLocation(e.getX(), e.getY()).getLastPathComponent();
        final Node currnode = Tree.curr_node = Tree.getNode(curr_tree_node);
        if((e.getModifiers() & InputEvent.BUTTON3_MASK) != 0){
            final NodeBeanInfo nbi = currnode.getBeanInfo();
            final PropertyDescriptor[] node_properties = nbi.getPropertyDescriptors();
            final MethodDescriptor[] node_methods = nbi.getMethodDescriptors();
            if(this.pop == null){
                JMenuItem mitem;
                this.dialog_sets = new DialogSet[node_properties.length];
                for(int i = 0; i < node_properties.length; i++)
                    this.dialog_sets[i] = new DialogSet();
                this.pop = new JPopupMenu();
                this.menu_items = new JMenuItem[node_properties.length + node_methods.length];
                if(jTraverser.editable){
                    final JMenuItem jm = new JMenu("Add Node");
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
                    this.pop.add(jm);
                    this.pop.add(this.add_device_b = new JMenuItem("Add Device"));
                    this.add_device_b.addActionListener(this);
                    this.pop.add(this.add_child_b = new JMenuItem("Add Child"));
                    this.add_child_b.addActionListener(this);
                    this.pop.add(this.add_subtree_b = new JMenuItem("Add Subtree"));
                    this.add_subtree_b.addActionListener(this);
                    this.pop.add(this.delete_node_b = new JMenuItem("Delete Node"));
                    this.delete_node_b.addActionListener(this);
                    this.pop.add(this.modify_tags_b = new JMenuItem("Modify tags"));
                    this.modify_tags_b.addActionListener(this);
                    this.pop.add(mitem = new JMenuItem("Rename node"));
                    mitem.addActionListener(new ActionListener(){
                        @Override
                        public void actionPerformed(final ActionEvent e1) {
                            dialogs.rename.show();
                        }
                    });
                    this.pop.addSeparator();
                }
                item_idx = 0;
                for(int i = 0; i < node_properties.length; i++){
                    this.pop.add(this.menu_items[item_idx] = new JMenuItem(node_properties[i].getShortDescription()));
                    this.menu_items[item_idx].addActionListener(new ActionListener(){
                        @Override
                        public void actionPerformed(final ActionEvent e1) {
                            int idx;
                            for(idx = 0; idx < node_properties.length && !e1.getActionCommand().equals(node_properties[idx].getShortDescription()); idx++);
                            if(idx < node_properties.length){
                                Tree.curr_dialog_idx = idx;
                                final TreeDialog curr_dialog = Tree.this.dialog_sets[idx].getDialog(node_properties[idx].getPropertyEditorClass(), Tree.getCurrentNode());
                                if(curr_dialog == null) return;
                                curr_dialog.pack();
                                curr_dialog.setLocation(Tree.frame.dialogLocation());
                                curr_dialog.setVisible(true);
                            }
                        }
                    });
                    item_idx++;
                }
                for(int i = 0; i < node_methods.length; i++){
                    this.pop.add(this.menu_items[item_idx] = new JMenuItem(node_methods[i].getShortDescription()));
                    this.menu_items[item_idx].addActionListener(new ActionListener(){
                        @Override
                        public void actionPerformed(final ActionEvent e1) {
                            int idx;
                            for(idx = 0; idx < node_methods.length && !e1.getActionCommand().equals(node_methods[idx].getShortDescription()); idx++);
                            if(idx < node_methods.length){
                                try{
                                    node_methods[idx].getMethod().invoke(Tree.getCurrentNode());
                                }catch(final Exception exc){
                                    System.out.println("Error executing " + exc);
                                }
                                Tree.curr_tree.expandPath(new TreePath(curr_tree_node.getPath()));
                                Tree.curr_tree.treeDidChange();
                            }
                        }
                    });
                    item_idx++;
                }
                this.pop.add(mitem = new JMenuItem("Flags"));
                mitem.addActionListener(new ActionListener(){
                    @Override
                    public void actionPerformed(final ActionEvent e1) {
                        dialogs.flags.show();
                    }
                });
                this.pop.addSeparator();
                this.pop.add(mitem = new JMenuItem("Open"));
                mitem.addActionListener(new ActionListener(){
                    @Override
                    public void actionPerformed(final ActionEvent e1) {
                        Tree.this.open();
                    }
                });
                this.pop.add(mitem = new JMenuItem("Close"));
                mitem.addActionListener(new ActionListener(){
                    @Override
                    public void actionPerformed(final ActionEvent e1) {
                        Tree.this.close();
                    }
                });
                this.pop.add(mitem = new JMenuItem("Quit"));
                mitem.addActionListener(new ActionListener(){
                    @Override
                    public void actionPerformed(final ActionEvent e1) {
                        Tree.this.quit();
                    }
                });
            }
            item_idx = 0;
            for(int i = 0; i < node_properties.length; i++){
                if(!nbi.isSupported(node_properties[i].getShortDescription())) this.menu_items[item_idx].setEnabled(false);
                else this.menu_items[item_idx].setEnabled(true);
                item_idx++;
            }
            for(int i = 0; i < node_methods.length; i++){
                if(!nbi.isSupported(node_methods[i].getShortDescription())) this.menu_items[item_idx].setEnabled(false);
                else this.menu_items[item_idx].setEnabled(true);
                item_idx++;
            }
            this.pop.show(Tree.curr_tree, e.getX(), e.getY());
        }
        Tree.curr_tree.treeDidChange();
        dialogs.update();
    }

    @Override
    public void mouseReleased(final MouseEvent e) {}

    void open() {
        if(this.open_dialog == null){
            this.open_dialog = new JDialog(Tree.frame);
            this.open_dialog.setTitle("Open new tree");
            final JPanel mjp = new JPanel();
            mjp.setLayout(new BorderLayout());
            JPanel jp = new JPanel();
            jp.setLayout(new GridLayout(2, 1));
            JPanel jpi = new JPanel();
            jpi.add(new JLabel("Tree: "));
            jpi.add(this.open_exp = new JTextField(16));
            jp.add(jpi, "East");
            jpi = new JPanel();
            jpi.add(new JLabel("Shot: "));
            jpi.add(this.open_shot = new JTextField(16));
            jp.add(jpi, "East");
            mjp.add(jp, "North");
            jp = new JPanel();
            jp.setLayout(new GridLayout(1, 3));
            jp.add(this.open_normal = new JRadioButton("normal"));
            jp.add(this.open_readonly = new JRadioButton("readonly"));
            jp.add(this.open_edit = new JRadioButton("edit/new"));
            final ButtonGroup bgMode = new ButtonGroup();
            bgMode.add(this.open_readonly);
            bgMode.add(this.open_normal);
            bgMode.add(this.open_edit);
            mjp.add(jp, "Center");
            jp = new JPanel();
            jp.add(this.ok_cb = new JButton("Ok"));
            this.ok_cb.addActionListener(this);
            this.ok_cb.setSelected(true);
            final JButton cancel = new JButton("Cancel");
            jp.add(cancel);
            cancel.addActionListener(new ActionListener(){
                @Override
                public void actionPerformed(final ActionEvent e) {
                    Tree.this.open_dialog.setVisible(false);
                }
            });
            mjp.add(jp, "South");
            this.open_dialog.getContentPane().add(mjp);
            this.open_shot.addKeyListener(new KeyListener(){
                @Override
                public void keyPressed(final KeyEvent e) {
                    if(e.getKeyCode() == KeyEvent.VK_ENTER) Tree.this.open_ok();
                }

                @Override
                public void keyReleased(final KeyEvent e) {}

                @Override
                public void keyTyped(final KeyEvent e) {}
            });
            this.open_exp.addKeyListener(new KeyListener(){
                @Override
                public void keyPressed(final KeyEvent e) {
                    if(e.getKeyCode() == KeyEvent.VK_ENTER) Tree.this.open_ok();
                }

                @Override
                public void keyReleased(final KeyEvent e) {}

                @Override
                public void keyTyped(final KeyEvent e) {}
            });
            this.open_dialog.pack();
            this.open_dialog.setResizable(false);
            if(Tree.curr_experiment != null) try{
                this.open_exp.setText(Tree.curr_experiment.getName());
                this.open_shot.setText(new Integer(Tree.curr_experiment.getShot()).toString());
            }catch(final Exception exc){}
        }
        this.open_dialog.setLocation(Tree.frame.dialogLocation());
        this.open_normal.setSelected(true);
        this.open_dialog.setVisible(true);
    }

    public void open(final String exp, int shot, boolean editable, final boolean readonly, final boolean realtime) {
        Node top_node = null;
        this.topExperiment = exp;
        // first we need to check if the tree is already open
        RemoteTree loop_exp = null;
        for(int i = 0; i < this.trees.size(); i++){
            loop_exp = this.experiments.elementAt(i);
            try{
                if(loop_exp.getName().equals(exp) && loop_exp.getShot() == shot){
                    this.trees.removeElementAt(i);
                    this.experiments.removeElementAt(i);
                    break;
                }
            }catch(final Exception exc){}
        }
        final String remote_tree_ip = System.getProperty("remote_tree.ip");
        if(remote_tree_ip == null){
            Tree.is_remote = false;
            Tree.curr_experiment = new Database();
        }else{
            Tree.is_remote = true;
            if(System.getSecurityManager() == null) System.setSecurityManager(new SecurityManager(){
                @Override
                public void checkConnect(final String host, final int port) {}

                @Override
                public void checkLink(final String lib) {}

                @Override
                public void checkPropertiesAccess() {}

                @Override
                public void checkPropertyAccess(final String property) {}

                @Override
                public void checkRead(final FileDescriptor fd) {}

                @Override
                public void checkRead(final String file) {}

                @Override
                public void checkRead(final String file, final Object context) {}
            });
            final String name = "//" + remote_tree_ip + "/TreeServer";
            try{
                Tree.curr_experiment = (RemoteTree)Naming.lookup(name);
            }catch(final Exception exc){
                JOptionPane.showMessageDialog(Tree.frame, exc.getMessage(), "Error opening remote " + exp, JOptionPane.ERROR_MESSAGE);
                return;
            }
        }
        if(shot == 0) try{
            shot = Tree.curr_experiment.getCurrentShot(exp);
        }catch(final Exception exc){
            JOptionPane.showMessageDialog(this.open_dialog, "Shot 0 not defined for " + exp, "Error opening tree", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try{
            Tree.curr_experiment.setTree(exp, shot);
            Tree.curr_experiment.setEditable(editable);
            Tree.curr_experiment.setReadonly(readonly);
            Tree.curr_experiment.setRealtime(realtime);
        }catch(final Exception exc){
            jTraverser.stderr("Error in RMI communication", exc);
        }
        try{
            Tree.context = Tree.curr_experiment.open();
            top_node = new Node(Tree.curr_experiment, this);
        }catch(final Exception exc){
            JOptionPane.showMessageDialog(Tree.frame, exc.getMessage(), "Error opening " + exp, JOptionPane.ERROR_MESSAGE);
            return;
        }
        top_node.setTreeNode(this.top = new DefaultMutableTreeNode(top_node));
        try{
            top_node.expand();
        }catch(final Exception exc){
            jTraverser.stderr("Error expanding tree", exc);
        }
        final Node members[] = top_node.getMembers();
        for(final Node member : members){
            DefaultMutableTreeNode currNode;
            this.top.add(currNode = new DefaultMutableTreeNode(member));
            member.setTreeNode(currNode);
        }
        final Node sons[] = top_node.getSons();
        for(final Node son : sons){
            DefaultMutableTreeNode currNode;
            this.top.add(currNode = new DefaultMutableTreeNode(son));
            son.setTreeNode(currNode);
        }
        Tree.curr_tree = new JTree(this.top);
        // GAB 2014 Add DragAndDrop capability
        Tree.curr_tree.setTransferHandler(new FromTransferHandler());
        Tree.curr_tree.setDragEnabled(true);
        /////////////////////////////
        ToolTipManager.sharedInstance().registerComponent(Tree.curr_tree);
        Tree.curr_tree.addKeyListener(new KeyAdapter(){
            @Override
            public void keyTyped(final KeyEvent e) {
                if(e.getKeyChar() == KeyEvent.VK_CANCEL) // i.e. Ctrl+C
                {
                    TreeNode.copyToClipboard();
                    TreeNode.copy();
                }else if(e.getKeyChar() == 24) // i.e. Ctrl+X
                TreeNode.cut();
                else if(e.getKeyChar() == 22) // i.e. Ctrl+V
                TreeNode.paste();
                else if(e.getKeyChar() == KeyEvent.VK_DELETE || e.getKeyChar() == KeyEvent.VK_BACK_SPACE) TreeNode.delete();
                Tree.reportChange();
            }
        });
        if(this.is_angled_style) Tree.curr_tree.putClientProperty("JTree.lineStyle", "Angled");
        try{
            Tree.curr_tree.setEditable(Tree.curr_experiment.isEditable());
        }catch(final Exception exc){
            Tree.curr_tree.setEditable(false);
        }
        Tree.curr_tree.setCellRenderer(new MDSCellRenderer());
        Tree.curr_tree.addTreeSelectionListener(this);
        Tree.curr_tree.addMouseListener(this);
        Tree.curr_tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        this.setViewportView(Tree.curr_tree);
        this.trees.push(Tree.curr_tree);
        this.experiments.push(Tree.curr_experiment);
        try{
            editable = Tree.curr_experiment.isEditable();
        }catch(final Exception exc){
            editable = false;
        }
        Tree.frame.reportChange(exp, shot, editable, readonly);
    }

    private void open_ok() {
        final String exp = this.open_exp.getText(), shot_t = this.open_shot.getText();
        this.topExperiment = exp;
        if(exp == null || exp.length() == 0){
            JOptionPane.showMessageDialog(this.open_dialog, "Missing experiment name", "Error opening tree", JOptionPane.WARNING_MESSAGE);
            this.repaint();
            return;
        }
        int shot;
        if(shot_t == null || shot_t.length() == 0){
            JOptionPane.showMessageDialog(this.open_dialog, "Wrong shot number", "Error opening tree", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try{
            shot = Integer.parseInt(shot_t);
        }catch(final Exception e){
            JOptionPane.showMessageDialog(Tree.curr_tree, "Wrong shot number", "Error opening tree", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if(this.open_edit.isSelected() && this.open_readonly.isSelected()){
            JOptionPane.showMessageDialog(Tree.curr_tree, "Tree cannot be open in both edit and readonly mode", "Error opening tree", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if(jTraverser.editable != this.open_edit.isSelected()) this.pop = null;
        this.open(exp.toUpperCase(), shot, this.open_edit.isSelected(), this.open_readonly.isSelected(), false);
        this.open_dialog.setVisible(false);
        dialogs.update();
        Tree.frame.pack();
        this.repaint();
    }

    void quit() {
        while(!this.experiments.empty()){
            Tree.curr_experiment = this.experiments.pop();
            try{
                Tree.curr_experiment.close(Tree.context);
            }catch(final Exception exc){
                boolean editable = false;
                String name = null;
                try{
                    editable = Tree.curr_experiment.isEditable();
                    name = Tree.curr_experiment.getName();
                }catch(final Exception exc2){}
                if(editable){
                    final int n = JOptionPane.showConfirmDialog(Tree.frame, "Tree has been changed: write it before closing?", "Tree " + name + " open in edit mode", JOptionPane.YES_NO_OPTION);
                    if(n == JOptionPane.YES_OPTION){
                        try{
                            Tree.curr_experiment.write(Tree.context);
                            Tree.curr_experiment.close(Tree.context);
                        }catch(final Exception exc2){
                            jTraverser.stderr("Error closing experiment", exc2);
                        }
                    }
                }else jTraverser.stderr("Error closing experiment", exc);
            }
        }
        System.exit(0);
    }

    void setAngled(final boolean is_angled) {
        this.is_angled_style = is_angled;
        for(int i = 0; i < this.trees.size(); i++){
            final JTree curr_tree = this.trees.elementAt(i);
            if(this.is_angled_style) curr_tree.putClientProperty("JTree.lineStyle", "Angled");
            else curr_tree.putClientProperty("JTree.lineStyle", "None");
            curr_tree.treeDidChange();
        }
    }

    @Override
    public void valueChanged(final TreeSelectionEvent e) {
        final DefaultMutableTreeNode curr_tree_node = (DefaultMutableTreeNode)e.getPath().getLastPathComponent();
        if(curr_tree_node.isLeaf()){
            final Node currnode = Tree.curr_node = Tree.getNode(curr_tree_node);
            Node sons[], members[];
            DefaultMutableTreeNode last_node = null;
            try{
                Tree.curr_node.expand();
            }catch(final Exception exc){
                jTraverser.stderr("Error expanding tree", exc);
            }
            sons = currnode.getSons();
            members = currnode.getMembers();
            for(final Node member : members){
                curr_tree_node.add(last_node = new DefaultMutableTreeNode(member));
                member.setTreeNode(last_node);
            }
            for(final Node son : sons){
                curr_tree_node.add(last_node = new DefaultMutableTreeNode(son));
                son.setTreeNode(last_node);
            }
            if(last_node != null) Tree.curr_tree.expandPath(new TreePath(last_node.getPath()));
        }
    }
}
