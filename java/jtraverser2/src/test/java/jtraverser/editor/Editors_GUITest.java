package jtraverser.editor;

import java.awt.GridLayout;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import jtraverser.dialogs.GraphPanel;
import jtraverser.editor.usage.AnyEditor;
import jtraverser.editor.usage.AxisEditor;
import jtraverser.editor.usage.NumericEditor;
import jtraverser.editor.usage.SignalEditor;
import mds.MdsException;
import mds.data.descriptor.ARRAY;
import mds.data.descriptor.Descriptor;
import mds.data.descriptor_a.Float32Array;
import mds.data.descriptor_a.Uint64Array;
import mds.data.descriptor_r.Param;
import mds.data.descriptor_r.Range;
import mds.data.descriptor_r.Signal;
import mds.data.descriptor_r.With_Units;
import mds.data.descriptor_r.function.CONST;
import mds.data.descriptor_s.Int32;
import mds.data.descriptor_s.StringDsc;

public class Editors_GUITest{
	private static final Range			range;
	private static final Descriptor<?>	rangewp;
	private static final ARRAY<?>		array, ndarray;
	private static final Signal			signal;
	static{
		range = new Range(1, 10000, 1);
		rangewp = new Param(new With_Units(Editors_GUITest.range, "s"), Descriptor.valueOf("help"), new Int32(1));
		ARRAY<?> tarray;
		try{
			tarray = Editors_GUITest.range.getDataA();
		}catch(final MdsException e){
			tarray = null;
		}
		array = tarray;
		ndarray = new Uint64Array(new int[]{100, 100}, Editors_GUITest.array.toLongArray());
		signal = new Signal(CONST.$VALUE, Editors_GUITest.ndarray, Editors_GUITest.range);
	}

	@AfterClass
	public static final void tearDownAfterClass() throws Exception {
		Thread.sleep(60000);
	}
	private JDialog	d;
	private JPanel	p;

	@Before
	public void setUp() throws Exception {
		this.d = new JDialog();
		this.p = new JPanel(new GridLayout(0, 1));
		this.d.add(this.p);
		this.d.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
	}

	@After
	public void tearDown() throws Exception {
		this.d.pack();
		this.d.setVisible(true);
	}

	@Test
	public void testAnyEditor() {
		final Editor e;
		this.p.add(e = new AnyEditor(true, null, this.d));
		e.setData(Editors_GUITest.signal);
	}

	@Test
	public void testArrayEditor() {
		final Editor e;
		this.p.add(e = new ArrayEditor(true, null));
		e.setData(Editors_GUITest.ndarray);
	}

	@Test
	public void testAxisEditor() {
		final Editor e;
		this.p.add(e = new AxisEditor(true, null, this.d));
		e.setData(Editors_GUITest.rangewp);
	}

	@Test
	public void testExprEditor() {
		Editor e;
		this.p.add(e = new ExprEditor(false, null, true, true));
		e.setData(Editors_GUITest.rangewp);
		this.p.add(Editor.addLabel("TEST", e = new ExprEditor(true, null, true, true)));
		e.setData(new StringDsc("teststring"));
	}

	@SuppressWarnings("static-method")
	@Test
	public void testGraphPanel() {
		GraphPanel.newPlot(new Signal(new Float32Array(0, 3, 4, 3, 0, -3, -4, -3, 0, 3), null, new Float32Array(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)), null, "my title");
	}

	@Test
	public void testNumericEditor() {
		this.p.add(new NumericEditor(Editors_GUITest.ndarray, true, null, this.d, "test"));
	}

	@Test
	public void testSignalEditor() {
		this.p.add(new SignalEditor(Editors_GUITest.signal, true, null, this.d));
	}
}
