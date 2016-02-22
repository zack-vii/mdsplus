package jScope;

import jScope.W7XDataProvider.signalaccess;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import com.toedter.calendar.JCalendar;
import de.mpg.ipp.codac.signalaccess.SignalAddress;
import de.mpg.ipp.codac.w7xtime.TimeInterval;

public final class W7XSignalBrowser extends jScopeBrowseSignals{

    public final class W7XDataBase extends W7XNode{
        private static final long                 serialVersionUID = 77777772L;
        private final String                      name;
        public final W7XDataProvider.signalaccess sa;

        public W7XDataBase(final String name, final W7XDataProvider.signalaccess sa){
            super(sa.sab.newBuilder().path("").build());
            this.name = name;
            this.sa = sa;
        }

        @Override
        public final String toString() {
            return this.name;
        }
    }
    public class W7XNode extends DefaultMutableTreeNode{
        private static final long serialVersionUID = 77777771L;
        private boolean           loaded           = false;

        public W7XNode(final SignalAddress userObject){
            super(userObject);
            this.setAllowsChildren(true);
        }

        public void addSignal() {
            if(W7XSignalBrowser.this.wave_panel != null){
                final TreeNode[] path = this.getPath();
                if(path == null || path.length < 2) return;
                final String sig_path = "/" + ((W7XDataBase)path[1]).name + this.getSignalPath();
                if(sig_path != null) W7XSignalBrowser.this.wave_panel.AddSignal(null, null, "", sig_path, true, W7XSignalBrowser.this.is_image);
            }
        }

        @SuppressWarnings("unchecked")
        public final List<SignalAddress> getChildren() {
            final TreeNode[] path = this.getPath();
            if(path == null || path.length < 2) return null;
            final TimeInterval ti = TimeInterval.ALL.withStart(W7XSignalBrowser.this.from.getDate().getTime() * 1000000L).withEnd(W7XSignalBrowser.this.upto.getDate().getTime() * 1000000L);
            return (List<SignalAddress>)((W7XDataBase)path[1]).sa.stl.listFor(ti, this.getSignalPath());
        }

        private String getSignalPath() {
            return ((SignalAddress)this.getUserObject()).toString();
        }

        @Override
        public boolean isLeaf() {
            return this.loaded && !this.getAllowsChildren();
        }

        private void loadChildren() {
            this.setChildren(this.getChildren());
        }

        private void setChildren(final List<SignalAddress> children) {
            if(children == null) return;
            this.removeAllChildren();
            this.setAllowsChildren(children.size() > 0);
            for(final SignalAddress node : children){
                this.add(new W7XNode(node));
            }
            this.loaded = true;
        }

        @Override
        public String toString() {
            return ((SignalAddress)this.getUserObject()).tail();
        }
    }

    private static final long serialVersionUID = 77777770L;

    public static void main(final String[] args) {
        EventQueue.invokeLater(() -> {
            try{
                final W7XSignalBrowser frame = new W7XSignalBrowser();
                frame.setVisible(true);
            }catch(final Exception e){
                e.printStackTrace();
            }
        });
    }
    private final JPanel                contentPane;
    private final JCalendar from,upto;
    public final boolean                is_image = false;
    private String                      server_url;
    private String                      shot;
    public final DefaultMutableTreeNode top;
    private String                      tree;
    public jScopeWaveContainer          wave_panel;

    /**
     * Create the frame.
     */
    public W7XSignalBrowser(){
        this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        this.contentPane = new JPanel();
        this.contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        this.contentPane.setLayout(new BorderLayout(0, 0));
        this.setContentPane(this.contentPane);
        final JPanel jp = new JPanel();
        this.setMinimumSize(new Dimension(464, 300));
        this.setPreferredSize(new Dimension(464, 550));
        jp.add(this.from = new JCalendar(), BorderLayout.WEST);
        jp.add(this.upto = new JCalendar(), BorderLayout.EAST);
        this.from.getCalendar().setTimeZone(TimeZone.getTimeZone("UTC"));
        this.from.getCalendar().set(Calendar.HOUR_OF_DAY, 0);
        this.from.getCalendar().set(Calendar.MINUTE, 0);
        this.from.getCalendar().set(Calendar.SECOND, 0);
        this.from.getCalendar().set(Calendar.MILLISECOND, 0);
        this.upto.getCalendar().setTimeZone(TimeZone.getTimeZone("UTC"));
        this.upto.getCalendar().set(Calendar.HOUR_OF_DAY, 23);
        this.upto.getCalendar().set(Calendar.MINUTE, 59);
        this.upto.getCalendar().set(Calendar.SECOND, 59);
        this.upto.getCalendar().set(Calendar.MILLISECOND, 999);
        this.contentPane.add(jp, BorderLayout.NORTH);
        this.top = new DefaultMutableTreeNode("DataBase");
        final JTree tree = new JTree(this.top);
        this.contentPane.add(new JScrollPane(tree));
        tree.setRootVisible(true);
        final W7XDataBase db = new W7XDataBase("ArchiveDB", signalaccess.arch);
        this.top.add(db);
        this.top.add(new W7XDataBase("Test", signalaccess.test));
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.addTreeWillExpandListener(new TreeWillExpandListener(){
            @Override
            public void treeWillCollapse(final TreeExpansionEvent event) throws ExpandVetoException {}

            @Override
            public void treeWillExpand(final TreeExpansionEvent event) throws ExpandVetoException {
                final TreePath path = event.getPath();
                if(path.getLastPathComponent() instanceof W7XNode){
                    final W7XNode node = (W7XNode)path.getLastPathComponent();
                    if(!node.loaded) node.loadChildren();
                }
            }
        });
        tree.setToggleClickCount(0);
        tree.addMouseListener(new MouseAdapter(){
            @Override
            public void mousePressed(final MouseEvent e) {
                final int selRow = tree.getRowForLocation(e.getX(), e.getY());
                final TreePath path = tree.getPathForLocation(e.getX(), e.getY());
                if(path == null) return;
                final W7XNode node = (W7XNode)path.getLastPathComponent();
                if(selRow != -1) if(e.getClickCount() == 1) tree.expandRow(selRow);
                else if(e.getClickCount() == 2 && node.isLeaf()) node.addSignal();
            }
        });
        tree.expandPath(new TreePath(this.top));
        this.pack();
    }

    @Override
    protected String getServerAddr() {
        return this.server_url;
    }

    @Override
    protected String getShot() {
        return this.shot;
    }

    @Override
    protected String getSignal(final String url_name) {
        return null;
    }

    @Override
    protected String getTree() {
        return this.tree;
    }

    @Override
    public void setWaveContainer(final jScopeWaveContainer wave_panel) {
        this.wave_panel = wave_panel;
    }
}