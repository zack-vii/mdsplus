// package jTraverser;
import java.awt.Color;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

public class TreeNode extends JLabel{
    static Node               copied;
    static boolean            cut;
    static Font               plain_f, bold_f;
    private static final long serialVersionUID = -5991143423455919576L;

    static{
        TreeNode.plain_f = new Font("Serif", Font.PLAIN, 12);
        TreeNode.bold_f = new Font("Serif", Font.BOLD, 12);
    }

    public static void copy() {
        final Node currnode = Tree.getCurrentNode();
        if(currnode == null) return;
        TreeNode.cut = false;
        TreeNode.copied = currnode;
        jTraverser.stdout("copy: " + TreeNode.copied + " from " + TreeNode.copied.parent);
    }

    public static void copyToClipboard() {
        final Node currnode = Tree.getCurrentNode();
        if(currnode == null) return;
        try{
            final Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
            StringSelection content;
            final String path = currnode.getFullPath();
            content = new StringSelection(path);
            cb.setContents(content, null);
        }catch(final Exception exc){
            jTraverser.stderr("Cannot copy fullPath to Clipboard", exc);
        }
    }

    public static void cut() {
        final Node currnode = Tree.getCurrentNode();
        if(currnode == null) return;
        if(jTraverser.isEditable()){
            TreeNode.cut = true;
            TreeNode.copied = currnode;
            jTraverser.stdout("cut: " + TreeNode.copied + " from " + TreeNode.copied.parent);
        }else TreeNode.copy();
    }

    public static void delete() {
        final Node currnode = Tree.getCurrentNode();
        if(currnode == null) return;
        if(jTraverser.isEditable()) Tree.deleteNode(currnode);
        else jTraverser.stdout("Cannot delete " + currnode + ". Tree not in edit mode.");
    }

    public static void paste() {
        final Node currnode = Tree.getCurrentNode();
        if(currnode == null) return;
        if(jTraverser.isEditable()){
            jTraverser.stdout((TreeNode.cut ? "moved: " : "copied: ") + TreeNode.copied + " from " + TreeNode.copied.parent + " to " + currnode);
            if(TreeNode.copied != null && TreeNode.copied != currnode){
                if(TreeNode.cut){
                    if(TreeNode.copied.move(currnode)) TreeNode.copied = null;
                }else Node.pasteSubtree(TreeNode.copied, currnode, true);
            }
        }else jTraverser.stdout("Cannot paste " + TreeNode.copied + ". Tree not in edit mode.");
    }
    final Color CExclude  = new Color(128, 128, 128);
    final Color CInclude  = new Color(0, 0, 0);
    final Color CMSetup   = new Color(0, 0, 128);
    final Color CMSetupO  = new Color(96, 0, 128);
    final Color CNorm     = new Color(0, 0, 0);
    final Color CNormO    = new Color(96, 0, 96);
    final Color CNoWrite  = new Color(128, 0, 0);
    final Color CNoWriteO = new Color(192, 0, 0);
    final Color CSSetup   = new Color(128, 0, 128);
    final Color CSSetupO  = new Color(128, 0, 64);
    final Color CWrite    = new Color(0, 128, 0);
    final Color CWriteO   = new Color(96, 64, 0);
    Node        node;

    public TreeNode(final Node node, final String name, final Icon icon, final boolean isSelected){
        super((node.isDefault() ? "(" + node.getName() + ")" : node.getName()), icon, SwingConstants.LEFT);
        this.node = node;
        if(node.getUsage() == NodeInfo.USAGE_SUBTREE) this.setForeground(node.isIncludeInPulse() ? this.CInclude : this.CExclude);
        else{
            if(node.isNoWriteModel() & node.isNoWriteModel()) this.setForeground(this.CNoWrite);
            else if(node.isNoWriteModel()) this.setForeground(jTraverser.model ? (node.isWriteOnce() ? this.CNoWriteO : this.CNoWrite) : (node.isWriteOnce() ? this.CWriteO : this.CWrite));
            else if(node.isNoWriteShot()) this.setForeground(!jTraverser.model ? (node.isWriteOnce() ? this.CNoWriteO : this.CNoWrite) : (node.isWriteOnce() ? this.CWriteO : this.CWrite));
            else if(node.isSetup()) this.setForeground(jTraverser.model ? (node.isWriteOnce() ? this.CMSetupO : this.CMSetup) : (node.isWriteOnce() ? this.CSSetupO : this.CSSetup));
            else this.setForeground(node.isWriteOnce() ? this.CNormO : this.CNorm);
        }
        this.setFont(node.isOn() ? TreeNode.bold_f : TreeNode.plain_f);
        this.setBorder(BorderFactory.createLineBorder(isSelected ? Color.black : Color.white, 1));
    }

    @Override
    public String getToolTipText() {
        final String tags[] = this.node.getTags();
        if(tags.length > 0){
            String tagsStr = "";
            for(int i = 0; i < tags.length; i++){
                tagsStr += tags[i];
                if(i < tags.length - 1) tagsStr += "\n";
            }
            return tagsStr;
        }
        return null;
    }
}
