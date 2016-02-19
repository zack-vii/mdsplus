package jScope;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileWriter;
import java.io.IOException;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;

final public class PropertiesEditor extends JDialog{
    static final long serialVersionUID = 34724623452341L;
    JEditorPane       text;
    String            prFile;

    public PropertiesEditor(JFrame owner, String propetiesFile){
        super(owner);
        this.setTitle("jScope properties file editor : " + propetiesFile);
        prFile = propetiesFile;
        text = new JEditorPane();
        text.setEditable(true);
        try{
            text.setPage("file:" + propetiesFile);
        }catch(IOException exc){}
        JScrollPane scroller = new JScrollPane();
        JViewport vp = scroller.getViewport();
        vp.add(text);
        getContentPane().add(scroller, BorderLayout.CENTER);
        JPanel p = new JPanel();
        JButton save = new JButton("Save");
        save.setSelected(true);
        p.add(save);
        save.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                try{
                    text.write(new FileWriter(prFile));
                    JOptionPane.showMessageDialog(PropertiesEditor.this, "The changes will take effect the next time you restart jScope.", "Info", JOptionPane.WARNING_MESSAGE);
                }catch(IOException exc){
                    exc.printStackTrace();
                };
            }
        });
        JButton close = new JButton("Close");
        close.setSelected(true);
        p.add(close);
        close.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
                dispose();
            }
        });
        getContentPane().add(p, BorderLayout.SOUTH);
        pack();
        setSize(680, 700);
    }
}