package jscope.dialog;

/* $Id$ */
import java.awt.BorderLayout;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Vector;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import debug.DEBUG;

public class HelpDialog extends JDialog{
	private static final long	serialVersionUID	= 1L;
	protected JEditorPane		html;
	protected String			mime_type;
	URLConnection				url_con;
	protected Vector<URL>		url_list			= new Vector<URL>();

	public HelpDialog(final JFrame owner){
		super(owner);
		this.html = new JEditorPane();
		this.html.setEditable(false);
		final JScrollPane scroller = new JScrollPane();
		final JViewport vp = scroller.getViewport();
		vp.add(this.html);
		this.getContentPane().add(scroller, BorderLayout.CENTER);
		this.pack();
		this.setSize(1024, 600);
	}

	public void connectToBrowser(final URL url) throws Exception {
		if(DEBUG.M) System.out.println("connectToBrowser(" + url + ")");
		if(url != null){
			this.url_list.addElement(url);
			this.setPage(url);
		}
	}

	@SuppressWarnings("static-method")
	public String getDefaultURL() {
		return null;
	}

	protected void setPage(final URL url) throws IOException {
		this.url_con = url.openConnection();
		this.mime_type = this.url_con.getContentType();
		// Assume (like browsers) that missing mime-type indicates text/html.
		if(this.mime_type == null || this.mime_type.indexOf("text") != -1) this.html.setPage(url);
		else{
			final String path = "TWU_image_message.html";
			final URL u = this.getClass().getClassLoader().getResource(path);
			this.html.setPage(u);
		}
	}
}
