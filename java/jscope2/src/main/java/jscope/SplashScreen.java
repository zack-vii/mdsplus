package jscope;

/* $Id$ */
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Toolkit;
import java.net.URL;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

class SplashScreen extends JLabel{
	private static final long	serialVersionUID	= 1L;
	static final String			splash_screen		= "SplashScreen.jpg";
	public static final String	VERSION;
	static{
		String version = "unknown";
		try{
			final Class<SplashScreen> clazz = SplashScreen.class;
			final String className = clazz.getSimpleName() + ".class";
			final String classPath = clazz.getResource(className).toString();
			if(classPath.startsWith("jar")){
				final String manifestPath = classPath.substring(0, classPath.lastIndexOf("!") + 1) + "/META-INF/MANIFEST.MF";
				final Manifest manifest = new Manifest(new URL(manifestPath).openStream());
				final Attributes attr = manifest.getMainAttributes();
				version = attr.getValue("Implementation-Version");
			}
		}catch(final Exception e){
			e.printStackTrace();
		}
		VERSION = new StringBuilder(64).append("Version ").append(version).toString();
	}

	public static void main(final String[] args) {
		try{
			SwingUtilities.invokeAndWait(new Runnable(){
				@Override
				public void run() {
					final JFrame frame = new JFrame();
					frame.setContentPane(new SplashScreen());
					frame.pack();
					frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
					frame.setVisible(true);
				}
			});
		}catch(final Exception e){
			e.printStackTrace();
		}
	}
	ImageIcon io = null;

	public SplashScreen(){
		try{
			this.io = new ImageIcon(this.getClass().getClassLoader().getResource(SplashScreen.splash_screen));
			this.setIcon(this.io);
		}catch(final NullPointerException e){
			e.getStackTrace();
		}
	}

	@Override
	public void paint(final Graphics gReal) {
		final Image imageBuffer = this.createImage(this.getWidth(), this.getHeight());
		final Graphics g = imageBuffer.getGraphics();
		if(this.io == null) return;
		final Image image = this.io.getImage();
		g.drawImage(image, 1, 1, null);
		Toolkit.getDefaultToolkit().sync();
		final int start = 32 + 2;
		// int top = 102 + 1;
		final int botton = 268 + 1;
		final int delta = 14;
		g.setColor(new Color(128, 128, 128));
		g.drawRect(-1, -1, this.getWidth(), this.getHeight()); // makes a bevel border likeness
		g.drawString(SplashScreen.VERSION, start, botton - 3 * delta);
		g.drawString("http://mds-control.ipp-hgw.mpg.de", start, botton - 2 * delta);
		g.drawString("JVM used :" + System.getProperty("java.version"), start, botton - delta);
		gReal.drawImage(imageBuffer, 0, 0, this);
	}

	@Override
	public void update(final Graphics g) {
		this.paint(g);
	}
}
