package jscope;

import java.awt.Dimension;
import javax.swing.JComponent;
import jscope.data.DataProvider;

/**
 * used to browse signals available from current data provider
 */
public interface SignalBrowser{
	public JComponent getComponent(final jScopeWaveContainer wc, final DataProvider dp);

	public Dimension getPreferredSize();

	public String getTitle();
}
