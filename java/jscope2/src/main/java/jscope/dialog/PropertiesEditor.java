package jscope.dialog;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
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

public final class PropertiesEditor extends JDialog{
	private static final long	serialVersionUID	= 1L;
	File						prFile;
	JEditorPane					text;

	public PropertiesEditor(final JFrame owner, final File propertiesFile){
		super(owner);
		this.setTitle("jScope properties file editor : " + propertiesFile);
		this.prFile = propertiesFile;
		this.text = new JEditorPane();
		this.text.setEditable(true);
		try{
			this.text.setPage("file:" + propertiesFile);
		}catch(final IOException exc){/**/}
		final JScrollPane scroller = new JScrollPane();
		final JViewport vp = scroller.getViewport();
		vp.add(this.text);
		this.getContentPane().add(scroller, BorderLayout.CENTER);
		final JPanel p = new JPanel();
		final JButton save = new JButton("Save");
		save.setSelected(true);
		p.add(save);
		save.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(final ActionEvent e) {
				try{
					@SuppressWarnings("resource")
					final FileWriter fw = new FileWriter(PropertiesEditor.this.prFile);
					try{
						PropertiesEditor.this.text.write(fw);
					}finally{
						fw.close();
					}
					JOptionPane.showMessageDialog(PropertiesEditor.this, "The changes will take effect the next time you restart jScope.", "Info", JOptionPane.WARNING_MESSAGE);
				}catch(final IOException exc){
					exc.printStackTrace();
				}
			}
		});
		final JButton close = new JButton("Close");
		close.setSelected(true);
		p.add(close);
		close.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(final ActionEvent e) {
				PropertiesEditor.this.setVisible(false);
				PropertiesEditor.this.dispose();
			}
		});
		this.getContentPane().add(p, BorderLayout.SOUTH);
		this.pack();
		this.setSize(680, 700);
	}
}