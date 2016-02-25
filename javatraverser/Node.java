// package jTraverser;
import java.awt.Dimension;
import java.rmi.RemoteException;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

public class Node{
    static Node copiedNode;

    public static void copySubtreeContent(final Node fromNode, final Node toNode) {
        try{
            fromNode.expand();
            toNode.expand();
        }catch(final Exception exc){
            jTraverser.stderr("Error expanding nodes", exc);
        }
        try{
            final Data data = fromNode.getData();
            if(data != null){
                if(!(data instanceof ActionData)) toNode.setData(data);
            }
        }catch(final Throwable exc){}
        for(int i = 0; i < fromNode.sons.length; i++)
            Node.copySubtreeContent(fromNode.sons[i], toNode.sons[i]);
        for(int i = 0; i < fromNode.members.length; i++)
            Node.copySubtreeContent(fromNode.members[i], toNode.members[i]);
    }

    public static String getUniqueName(String name, final String[] usedNames) {
        int i;
        for(i = 0; i < usedNames.length && !name.equals(usedNames[i]); i++);
        if(i == usedNames.length) return name;
        for(i = name.length() - 1; i > 0 && (name.charAt(i) >= '0' && name.charAt(i) <= '9'); i--);
        name = name.substring(0, i + 1);
        String prevName;
        if(name.length() < 10) prevName = name;
        else prevName = name.substring(0, 9);
        for(i = 1; i < 1000; i++){
            final String newName = prevName + i;
            int j;
            for(j = 0; j < usedNames.length && !newName.equals(usedNames[j]); j++);
            if(j == usedNames.length) return newName;
        }
        return "XXXXXXX"; // Dummy name, hopefully will never reach this
    }

    static public void pasteSubtree(final Node fromNode, final Node toNode, final boolean isMember) {
        final DefaultMutableTreeNode savedTreeNode = Tree.getCurrTreeNode();
        try{
            fromNode.expand();
            final String[] usedNames = new String[toNode.sons.length + toNode.members.length];
            // collect names used so far
            int idx = 0;
            for(final Node son : toNode.sons)
                usedNames[idx++] = son.getName();
            for(final Node member : toNode.members)
                usedNames[idx++] = member.getName();
            if(fromNode.getUsage() == NodeInfo.USAGE_DEVICE){
                final ConglomData conglom = (ConglomData)fromNode.getData();
                final Node newNode = Tree.addDevice((isMember ? ":" : ".") + Node.getUniqueName(fromNode.getName(), usedNames), conglom.getModel().getString(), toNode);
                newNode.expand();
                Node.copySubtreeContent(fromNode, newNode);
            }else{
                final Node newNode = Tree.addNode(fromNode.getUsage(), (isMember ? ":" : ".") + Node.getUniqueName(fromNode.getName(), usedNames), toNode);
                if(newNode == null) return;
                newNode.expand();
                try{
                    final Data data = fromNode.getData();
                    if(data != null && fromNode.getUsage() != NodeInfo.USAGE_ACTION) newNode.setData(data);
                }catch(final Exception exc){}
                for(final Node son : fromNode.sons){
                    Node.pasteSubtree(son, newNode, false);
                }
                for(final Node member : fromNode.members){
                    Node.pasteSubtree(member, newNode, true);
                }
            }
        }catch(final Exception exc){
            JOptionPane.showMessageDialog(FrameRepository.frame, "" + exc, "Error copying subtree", JOptionPane.WARNING_MESSAGE);
        }
        Tree.setCurrTreeNode(savedTreeNode);
    }

    public static void updateCell() {}
    NodeBeanInfo                   bean_info;
    Data                           data;
    RemoteTree                     experiment;
    Tree                           hierarchy;
    NodeInfo                       info;
    boolean                        is_member;
    boolean                        is_on;
    Node[]                         members;
    boolean                        needsOnCheck = true;
    NidData                        nid;
    Node                           parent;
    Node[]                         sons;
    JLabel                         tree_label;
    private DefaultMutableTreeNode treenode;

    public Node(final RemoteTree experiment, final Tree hierarchy) throws DatabaseException, RemoteException{
        this.experiment = experiment;
        this.hierarchy = hierarchy;
        if(experiment.isRealtime()) this.nid = new NidData(1);
        else this.nid = new NidData(0);
        this.info = experiment.getInfo(this.nid, Tree.context);
        this.parent = null;
        this.is_member = false;
        this.sons = new Node[0];
        this.members = new Node[0];
    }

    public Node(final RemoteTree experiment, final Tree hierarchy, final Node parent, final boolean is_member, final NidData nid){
        this.experiment = experiment;
        this.hierarchy = hierarchy;
        this.parent = parent;
        this.is_member = is_member;
        this.nid = nid;
        try{
            this.info = experiment.getInfo(nid, Tree.context);
        }catch(final Exception exc){
            jTraverser.stderr("Error getting info", exc);
        }
        this.sons = new Node[0];
        this.members = new Node[0];
    }

    public Node addChild(String name) throws DatabaseException, RemoteException {
        final NidData prev_default = this.experiment.getDefault(Tree.context);
        NidData new_nid;
        this.experiment.setDefault(this.nid, Tree.context);
        if(this.info == null) this.info = this.experiment.getInfo(this.nid, Tree.context);
        if(!name.startsWith(":") && !name.startsWith(".")) name = "." + name;
        new_nid = this.experiment.addNode(name, NodeInfo.USAGE_STRUCTURE, Tree.context);
        this.experiment.setDefault(prev_default, Tree.context);
        return new Node(this.experiment, this.hierarchy, this, true, new_nid);
    }

    public Node addDevice(final String name, final String type) throws DatabaseException, RemoteException {
        final NidData prev_default = this.experiment.getDefault(Tree.context);
        NidData new_nid = null;
        this.experiment.setDefault(this.nid, Tree.context);
        try{
            if(this.info == null) this.info = this.experiment.getInfo(this.nid, Tree.context);
            new_nid = this.experiment.addDevice(name, type, Tree.context);
        }finally{
            this.experiment.setDefault(prev_default, Tree.context);
        }
        final Node newNode = new Node(this.experiment, this.hierarchy, this, true, new_nid);
        if(name.charAt(0) == '.'){
            final Node[] newNodes = new Node[this.sons.length + 1];
            for(int i = 0; i < this.sons.length; i++)
                newNodes[i] = this.sons[i];
            newNodes[this.sons.length] = newNode;
            this.sons = newNodes;
        }else{
            final Node[] newNodes = new Node[this.members.length + 1];
            for(int i = 0; i < this.members.length; i++)
                newNodes[i] = this.members[i];
            newNodes[this.members.length] = newNode;
            this.members = newNodes;
        }
        return newNode;
    }

    public Node addNode(final int usage, String name) throws DatabaseException, RemoteException {
        final NidData prev_default = this.experiment.getDefault(Tree.context);
        NidData new_nid = null;
        this.experiment.setDefault(this.nid, Tree.context);
        try{
            if(this.info == null) this.info = this.experiment.getInfo(this.nid, Tree.context);
            if(usage == NodeInfo.USAGE_STRUCTURE && !name.startsWith(".") && !name.startsWith(":")) name = "." + name;
            new_nid = this.experiment.addNode(name, usage, Tree.context);
        }finally{
            this.experiment.setDefault(prev_default, Tree.context);
        }
        final Node newNode = new Node(this.experiment, this.hierarchy, this, true, new_nid);
        if(name.charAt(0) == '.'){
            final Node[] newNodes = new Node[this.sons.length + 1];
            for(int i = 0; i < this.sons.length; i++)
                newNodes[i] = this.sons[i];
            newNodes[this.sons.length] = newNode;
            this.sons = newNodes;
        }else{
            final Node[] newNodes = new Node[this.members.length + 1];
            for(int i = 0; i < this.members.length; i++)
                newNodes[i] = this.members[i];
            newNodes[this.members.length] = newNode;
            this.members = newNodes;
        }
        return newNode;
    }

    private boolean changePath(final Node newParent, final String newName) {
        if((newParent == this.parent) && (newName == this.getName())) return false; // nothing to do
        if(newName.length() > 12 || newName.length() == 0){
            JOptionPane.showMessageDialog(FrameRepository.frame, "Node name lengh must be between 1 and 12 characters", "Error renaming node: " + newName.length(), JOptionPane.WARNING_MESSAGE);
            return false;
        }
        try{
            final String sep = this.is_member ? ":" : ".";
            this.experiment.renameNode(this.nid, newParent.getFullPath() + sep + newName, Tree.context);
            this.info = this.experiment.getInfo(this.nid, Tree.context);
        }catch(final Exception exc){
            JOptionPane.showMessageDialog(FrameRepository.frame, "Error changing node path: " + exc, "Error changing node path", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        if(newParent != this.parent){
            this.parent = newParent;
            final DefaultTreeModel tree_model = (DefaultTreeModel)Tree.curr_tree.getModel();
            tree_model.removeNodeFromParent(this.getTreeNode());
            Tree.addNodeToParent(this.getTreeNode(), this.parent.getTreeNode());
        }
        return true;
    }

    public void clearFlag(final byte idx) throws DatabaseException, RemoteException {
        this.experiment.clearFlags(this.nid, 1 << idx);
        this.info.setFlags(this.experiment.getFlags(this.nid));
    }

    public void doAction() throws DatabaseException, RemoteException {
        try{
            this.experiment.doAction(this.nid, Tree.context);
        }catch(final Exception e){
            JOptionPane.showMessageDialog(null, e.getMessage(), "Error executing message", JOptionPane.WARNING_MESSAGE);
        }
    }

    public void executeDelete() {
        try{
            this.experiment.executeDelete(Tree.context);
        }catch(final Exception exc){
            jTraverser.stderr("Error executing delete", exc);
        }
    }

    public void expand() throws DatabaseException, RemoteException {
        int i;
        NidData sons_nid[] = this.experiment.getSons(this.nid, Tree.context);
        if(sons_nid == null) sons_nid = new NidData[0];
        NidData members_nid[] = this.experiment.getMembers(this.nid, Tree.context);
        if(members_nid == null) members_nid = new NidData[0];
        this.sons = new Node[sons_nid.length];
        this.members = new Node[members_nid.length];
        for(i = 0; i < sons_nid.length; i++)
            this.sons[i] = new Node(this.experiment, this.hierarchy, this, false, sons_nid[i]);
        for(i = 0; i < members_nid.length; i++)
            this.members[i] = new Node(this.experiment, this.hierarchy, this, true, members_nid[i]);
    }

    public NodeBeanInfo getBeanInfo() {
        if(this.bean_info == null) this.bean_info = new NodeBeanInfo(this.experiment, this.getUsage(), this.getName());
        return this.bean_info;
    }

    public final int getConglomerateElt() {
        return this.info.getConglomerateElt();
    }

    public final int getConglomerateNids() {
        return this.info.getConglomerateNids();
    }

    public Data getData() throws DatabaseException, RemoteException {
        this.data = this.experiment.getData(this.nid, Tree.context);
        return this.data;
    }

    public final String getDate() {
        return this.info.getDate();
    }

    public final byte getDClass() {
        return this.info.getDClass();
    }

    // info interface
    public final byte getDType() {
        return this.info.getDType();
    }

    public int getFlags() {
        try{
            this.info.setFlags(this.experiment.getFlags(this.nid));
        }catch(final Exception exc){
            jTraverser.stderr("Error updating flags", exc);
        }
        return this.info.getFlags();
    }

    public final String getFullPath() {
        return this.info.getFullPath();
    }

    public JLabel getIcon(final boolean isSelected) {
        if(this.info == null) return null;
        ImageIcon icon = null;
        switch(this.getUsage()){
            case NodeInfo.USAGE_NONE:
                icon = this.loadIcon("structure.gif");
                break;
            case NodeInfo.USAGE_ACTION:
                icon = this.loadIcon("action.gif");
                break;
            case NodeInfo.USAGE_DEVICE:
                icon = this.loadIcon("device.gif");
                break;
            case NodeInfo.USAGE_DISPATCH:
                icon = this.loadIcon("dispatch.gif");
                break;
            case NodeInfo.USAGE_ANY:
            case NodeInfo.USAGE_NUMERIC:
                icon = this.loadIcon("numeric.gif");
                break;
            case NodeInfo.USAGE_TASK:
                icon = this.loadIcon("task.gif");
                break;
            case NodeInfo.USAGE_TEXT:
                icon = this.loadIcon("text.gif");
                break;
            case NodeInfo.USAGE_WINDOW:
                icon = this.loadIcon("window.gif");
                break;
            case NodeInfo.USAGE_AXIS:
                icon = this.loadIcon("axis.gif");
                break;
            case NodeInfo.USAGE_SIGNAL:
                icon = this.loadIcon("signal.gif");
                break;
            case NodeInfo.USAGE_SUBTREE:
                icon = this.loadIcon("subtree.gif");
                break;
            case NodeInfo.USAGE_COMPOUND_DATA:
                icon = this.loadIcon("compound.gif");
                break;
        }
        this.tree_label = new TreeNode(this, this.getName(), icon, isSelected);
        return this.tree_label;
    }

    public NodeInfo getInfo() throws DatabaseException, RemoteException {
        try{
            this.info = this.experiment.getInfo(this.nid, Tree.context);
        }catch(final Exception exc){
            jTraverser.stderr("Error checking info", exc);
        }
        return this.info;
    }

    public final int getLength() {
        return this.info.getLength();
    }

    public final Node[] getMembers() {
        return this.members;
    }

    public final String getMinPath() {
        return this.info.getMinPath();
    }

    public final String getName() {
        return this.info.getName();
    }

    public final int getOwner() {
        return this.info.getOwner();
    }

    public final String getPath() {
        return this.info.getPath();
    }

    public final Node[] getSons() {
        return this.sons;
    }

    public String[] getTags() {
        try{
            return this.experiment.getTags(this.nid, Tree.context);
        }catch(final Exception exc){
            return null;
        }
    }

    public DefaultMutableTreeNode getTreeNode() {
        return this.treenode;
    }

    public final byte getUsage() {
        return this.info.getUsage();
    }

    public final boolean isCached() {
        return this.info.isCached();
    }

    public final boolean isCompressible() {
        return this.info.isCompressible();
    }

    public final boolean isCompressOnPut() {
        return this.info.isCompressOnPut();
    }

    public final boolean isCompressSegments() {
        return this.info.isCompressSegments();
    }

    public boolean isDefault() {
        NidData curr_nid = null;
        try{
            curr_nid = this.experiment.getDefault(Tree.context);
        }catch(final Exception exc){
            jTraverser.stderr("Error getting default", exc);
            return false;
        }
        return curr_nid.datum == this.nid.datum;
    }

    public final boolean isDoNotCompress() {
        return this.info.isDoNotCompress();
    }

    public final boolean isEssential() {
        return this.info.isEssential();
    }

    public final boolean isIncludeInPulse() {
        return this.info.isIncludeInPulse();
    }

    public final boolean isNidReference() {
        return this.info.isNidReference();
    }

    public final boolean isNoWriteModel() {
        return this.info.isNoWriteModel();
    }

    public final boolean isNoWriteShot() {
        return this.info.isNoWriteShot();
    }

    public boolean isOn() {
        if(this.needsOnCheck){
            this.needsOnCheck = false;
            try{
                this.is_on = this.experiment.isOn(this.nid, Tree.context);
            }catch(final Exception exc){
                jTraverser.stderr("Error checking state", exc);
            }
        }
        return this.is_on;
    }

    public final boolean isParentState() {
        return this.info.isParentState();
    }

    public final boolean isPathReference() {
        return this.info.isPathReference();
    }

    public final boolean isSegmented() {
        return this.info.isSegmented();
    }

    public final boolean isSetup() {
        return this.info.isSetup();
    }

    public final boolean isState() {
        return this.info.isState();
    }

    public final boolean isVersion() {
        return this.info.isVersion();
    }

    public final boolean isWriteOnce() {
        return this.info.isWriteOnce();
    }

    private ImageIcon loadIcon(final String gifname) {
        final String base = System.getProperty("icon_base");
        if(base == null) return new ImageIcon(this.getClass().getClassLoader().getResource(gifname));
        else return new ImageIcon(base + "/" + gifname);
    }

    boolean move(final Node newParent) {
        return this.changePath(newParent, this.getName());
    }

    boolean rename(final String newName) {
        return this.changePath(this.parent, newName);
    }

    void setAllOnUnchecked() {
        Node currNode = this;
        while(currNode.parent != null)
            currNode = currNode.parent;
        currNode.setOnUnchecked();
    }

    public void setData(final Data data) throws DatabaseException, RemoteException {
        this.data = data;
        this.experiment.putData(this.nid, data, Tree.context);
    }

    public void setDefault() throws DatabaseException, RemoteException {
        this.experiment.setDefault(this.nid, Tree.context);
    }

    public final void setFlag(final byte idx) throws DatabaseException, RemoteException {
        this.experiment.setFlags(this.nid, 1 << idx);
        this.info.setFlags(this.experiment.getFlags(this.nid));
    }

    public void setInfo(final NodeInfo info) throws DatabaseException, RemoteException {}

    void setOnUnchecked() {
        this.needsOnCheck = true;
        for(final Node son : this.sons)
            son.setOnUnchecked();
        for(final Node member : this.members)
            member.setOnUnchecked();
    }

    public void setSubtree() throws DatabaseException, RemoteException {
        this.experiment.setSubtree(this.nid, Tree.context);
        try{
            this.info = this.experiment.getInfo(this.nid, Tree.context);
            this.tree_label = null;
        }catch(final Exception exc){
            jTraverser.stderr("Error getting info", exc);
        }
    }

    public void setTags(final String[] tags) throws DatabaseException, RemoteException {
        this.experiment.setTags(this.nid, tags, Tree.context);
    }

    public void setTreeNode(final DefaultMutableTreeNode treenode) {
        this.treenode = treenode;
    }

    public void setupDevice() {
        ConglomData conglom = null;
        try{
            conglom = (ConglomData)this.experiment.getData(this.nid, Tree.context);
        }catch(final Exception e){
            JOptionPane.showMessageDialog(FrameRepository.frame, e.getMessage(), "Error in device setup 1", JOptionPane.WARNING_MESSAGE);
        }
        if(conglom != null){
            final Data model = conglom.getModel();
            if(model != null){
                try{
                    DeviceSetup ds = DeviceSetup.getDevice(this.nid.getInt());
                    if(ds == null){
                        final String deviceClassName = model.getString() + "Setup";
                        final Class deviceClass = Class.forName(deviceClassName);
                        ds = (DeviceSetup)deviceClass.newInstance();
                        final Dimension prevDim = ds.getSize();
                        ds.addDataChangeListener(this.hierarchy);
                        ds.configure(this.experiment, this.nid.getInt(), this);
                        if(ds.getContentPane().getLayout() != null) ds.pack();
                        ds.setLocation(this.hierarchy.getMousePosition());
                        ds.setSize(prevDim);
                        ds.setVisible(true);
                    }else ds.setVisible(true);
                    return;
                }catch(final Exception e){
                    try{
                        this.experiment.doDeviceMethod(this.nid, "dw_setup", Tree.context);
                    }catch(final Exception exc){
                        JOptionPane.showMessageDialog(FrameRepository.frame, e.getMessage(), "Error in device setup: " + e, JOptionPane.WARNING_MESSAGE);
                        e.printStackTrace();
                        return;
                    }
                }
            }
        }
        JOptionPane.showMessageDialog(null, "Missing model in descriptor", "Error in device setup 3", JOptionPane.WARNING_MESSAGE);
    }

    public int startDelete() {
        final NidData[] nids = {this.nid};
        try{
            return this.experiment.startDelete(nids, Tree.context).length;
        }catch(final Exception exc){
            jTraverser.stderr("Error starting delete", exc);
        }
        return 0;
    }

    public void toggle() throws DatabaseException, RemoteException {
        if(this.experiment.isOn(this.nid, Tree.context)) this.experiment.setOn(this.nid, false, Tree.context);
        else this.experiment.setOn(this.nid, true, Tree.context);
        this.setOnUnchecked();
    }

    @Override
    public final String toString() {
        return this.getName();
    }

    public void turnOff() {
        try{
            this.experiment.setOn(this.nid, false, Tree.context);
        }catch(final Exception exc){
            jTraverser.stderr("Error turning off", exc);
        }
        this.setOnUnchecked();
    }

    public void turnOn() {
        try{
            this.experiment.setOn(this.nid, true, Tree.context);
        }catch(final Exception exc){
            jTraverser.stderr("Error turning on", exc);
        }
        this.setOnUnchecked();
    }

    public void updateData() throws DatabaseException, RemoteException {
        this.data = this.experiment.getData(this.nid, Tree.context);
    }

    public void updateInfo() throws DatabaseException, RemoteException {
        this.info = this.experiment.getInfo(this.nid, Tree.context);
    }
}
