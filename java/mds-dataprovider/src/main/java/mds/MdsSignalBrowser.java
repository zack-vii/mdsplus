package mds;

import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.border.EtchedBorder;
import jscope.SignalBrowser;
import jscope.jScopeWaveContainer;
import jscope.data.DataProvider;
import jtraverser.TreeView;
import mds.data.TREE;

public class MdsSignalBrowser implements SignalBrowser{
	String title = "MdsSignalBrowser";

	public MdsSignalBrowser(){/**/}

	@Override
	public JComponent getComponent(final jScopeWaveContainer wc, final DataProvider dp) {
		if(dp instanceof MdsDataProvider && ((MdsDataProvider)dp).mds != null){
			MdsException me = new MdsException(MdsException.TreeNOTOPEN);
			if(wc == null) try{
				return this.getTree((MdsDataProvider)dp, "w7x", -1);
			}catch(final MdsException e){
				me = e;
			}
			else{
				final int[] shots = wc.getMainShots();
				String expt = wc.getDefaultExpt();
				if(expt == null) expt = "w7x";
				if(shots == null) try{
					return this.getTree((MdsDataProvider)dp, expt, -1);
				}catch(final MdsException e){
					me = e;
				}
				else for(final int shot : wc.getMainShots())
					try{
						return this.getTree((MdsDataProvider)dp, expt, shot);
					}catch(final MdsException e){
						me = e;
					}
			}
			return new JLabel(me.getMessage());
		}
		return null;
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(240, 500);
	}

	@Override
	public String getTitle() {
		return this.title;
	}

	private JComponent getTree(final MdsDataProvider mdp, final String expt, final int shot) throws MdsException {
		this.title = String.format("MDSplus tree %s shot %d", expt, Integer.valueOf(shot));
		if(mdp == null || mdp.mds == null) return null;
		final TREE tree = new TREE(mdp.mds, expt, shot, TREE.READONLY);
		final TreeView treeview = new TreeView(tree);
		treeview.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
		final JScrollPane scrlpane = new JScrollPane();
		scrlpane.setViewportView(treeview);
		return scrlpane;
	}
}
