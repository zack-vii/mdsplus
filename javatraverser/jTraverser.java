// package jTraverser;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

public final class jTraverser extends JFrame{
    static boolean            editable, readonly, model;
    static String             exp_name, shot_name;
    public static jTraverser  instance;
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
    JMenu     file_m, edit_m, data_m, customize_m;
    JMenuItem open, close, quit;

    public jTraverser(final String exp_name, final String shot_name, final String access){
        jTraverser.instance = this;
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
        JMenuItem item;
        final JMenuItem menu, smenu;
        menu_bar.add(this.file_m = new JMenu("File"));
        Tree.addFileMenu(this.file_m);
        menu_bar.add(this.edit_m = new JMenu("Edit"));
        Tree.addEditMenu(this.edit_m);
        menu_bar.add(this.data_m = new JMenu("Data"));
        Tree.addDataMenu(this.data_m);
        menu = new JMenu("Customize");
        menu_bar.add(menu);
        menu.add(smenu = new JMenu("Display Mode"));
        smenu.add(item = new JMenuItem("Outline"));
        item.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(final ActionEvent e) {
                jTraverser.tree.setAngled(false);
            }
        });
        smenu.add(item = new JMenuItem("Tree"));
        item.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(final ActionEvent e) {
                jTraverser.tree.setAngled(true);
            }
        });
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
    public Component add(final Component component) {
        return this.getContentPane().add(component);
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
            /* TODO: Readonly&Edit */
            // this.modify_data_b.setEnabled(true);
            // this.turn_on_b.setEnabled(true);
            // this.turn_off_b.setEnabled(true);
        }
        if(readonly){
            this.edit_m.setEnabled(false);
            /* TODO: Readonly&Edit */
            // this.modify_data_b.setEnabled(false);
            // this.turn_on_b.setEnabled(false);
            // this.turn_off_b.setEnabled(false);
        }
        if(!editable && !readonly){
            this.edit_m.setEnabled(false);
            /* TODO: Readonly&Edit */
            // this.modify_data_b.setEnabled(true);
            // this.turn_on_b.setEnabled(true);
            // this.turn_off_b.setEnabled(true);
        }
    }
}
