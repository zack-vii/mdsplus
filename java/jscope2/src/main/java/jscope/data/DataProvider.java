package jscope.data;

import java.awt.BorderLayout;
/* $Id$ */
import java.io.IOException;
import java.util.Map;
import java.util.Vector;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import jscope.data.framedata.FrameData;
import jscope.data.wavedata.WaveData;

/**
 * The DataProvider interface is used by jScope to deal with signals, regardless their origin.
 * DataProvider defines a set of method for providing the signal and supporting several features
 * which may be enabled or disabled depending on the current implementation of the DataProvider
 * interface. Signals are not directly returned by DataProvider access methods as vectors (as it was done
 * in earlier jScope versions). Rather DataProvider GetWaveData method returns an implementation of
 * WaveData. Interface WaveData defines the methods for collecting signal data.
 *
 * @see WaveData
 */
public interface DataProvider{
	static class LabeledCheckBox extends LabeledProperty{
		private static final long	serialVersionUID	= 1L;
		private JCheckBox			comp;

		public LabeledCheckBox(final String label, final boolean def){
			super(label);
			this.add(this.comp = new JCheckBox());
			this.comp.setSelected(def);
		}

		@Override
		public final String getValue() {
			return Boolean.toString(this.comp.isSelected());
		}

		@Override
		public void setValue(final String value) {
			this.comp.setSelected(Boolean.parseBoolean(value));
		}
	}
	static abstract class LabeledProperty extends JPanel{
		private static final long	serialVersionUID	= 1L;
		private JLabel				label;

		public LabeledProperty(final String label){
			super(new BorderLayout());
			this.add(this.label = new JLabel(label), BorderLayout.WEST);
		}

		@Override
		public final String getName() {
			return this.label.getText();
		}

		public abstract String getValue();

		public abstract void setValue(String value);
	}
	static class LabeledTextField extends LabeledProperty{
		private static final long	serialVersionUID	= 1L;
		private JTextField			comp;

		public LabeledTextField(final String label, final String def){
			super(label);
			this.add(this.comp = new JTextField());
			this.comp.setText(def);
		}

		@Override
		public final String getValue() {
			return this.comp.getText();
		}

		@Override
		public final void setValue(final String value) {
			this.comp.setText(value);
		}
	}
	static final int LOGIN_OK = 1, LOGIN_ERROR = 2, LOGIN_CANCEL = 3;

	/**
	 * Method AddConnectionListener is called by jScope after the instantiation of a DataProvider
	 * implementation, in order to be notified of the status of data transfer. If a DataProvider
	 * implementation has to handle the transfer of large chunks of data (e.g. frame sequences)
	 * requiring a relatively long time, it should be able to communicate the current status of
	 * the transfer to jScope which displays then a status bar. To do this, the DataProvider
	 * implementation calls ConnectionListener's method processConnectionEvent, passing an istance of
	 * ConnectionEvent as argument.
	 * Class ConnectionEvent defines the following information: <br>
	 * - int total_size: total size of the data to be transferred <br>
	 * - int current_size: the size of the data transferred so far <br>
	 * - String info: an additional information string which is displayed by jScope <br>
	 * If the DataProvider implementation does not support transfer notification, method AddConnectionListener
	 * is empty.
	 *
	 * @param l
	 *            The ProviderEventListener which has to be notified by means of method handleProviderEventListener()
	 * @see ProviderEventListener
	 */
	public void addProviderEventListener(ProviderEventListener l);

	/**
	 * Method AddUpdateEventListener is called by jScope when asynchronous update on event is
	 * defined. To connect data visualization to asynchronous event an event name has to be defined
	 * in the Update event field of the setup data form popup dialog.
	 * DataProvider implementations supporting asynchronous signal updating should
	 * notify event occurrence to all registered listeners by calling method processUpdateEvent(UpdateEvent e).
	 * UpdateEvent defines the following field, in addition to those defined in the AWTEvent
	 * superclass: <br>
	 * - String name: the name of the event <br>
	 * The possibility of connecting data display to asynchronous events is useful when jScope
	 * is used during data acquisition, for an automatic display update when a signal has been
	 * acquired. In this case the data acquisition system would generate an event when the
	 * signal has been acquired, and the DataProvider implementation would call method
	 * processUpdateEvent (defined in UpdateEventListener interface) to request the display update to
	 * jScope. <br>
	 * If the DataProvider implementation does not support asynchronous events, the method is defined
	 * empty. <br>
	 *
	 * @param l
	 *            The passed instance of UpdateEventListener.
	 * @exception java.io.IOException
	 * @see UpdateEventListener
	 */
	public void addUpdateEventListener(UpdateEventListener l, String name) throws IOException;

	/**
	 * Method checkProvider is called by jScope to verify the connection to the data provider.
	 *
	 * @return true if connected
	 */
	public boolean checkProvider();

	/**
	 * Method Dispose is called by jScope each time a DataProvider is no more used. Unlike Object.finalize(),
	 * method Dispose is guaranteed to be called at the time the DataProvider implementation is no more
	 * used by jScope.
	 */
	public void dispose();

	/**
	 * If an error is encountered in the evaluation of a signal (GetWaveData or
	 * GetResampledWaveData returning null or generation IOException), jScope calls ErrorString
	 * method to retrieve the description of the error just occurred.
	 *
	 * @return A verbose description of the last error.
	 */
	public String errorString();

	/**
	 * returns the default Browser to be used if none specified in the properties file
	 *
	 * @return the SignalBrowser class
	 */
	@SuppressWarnings("rawtypes")
	public Class getDefaultBrowser();

	/**
	 * Method GetFloat is called by jScope to evaluate x min, x max, y min, y max when defined
	 * in the setup data source pop-up form. The argument is in fact the string typed by the user in
	 * the form.
	 * In its simplest implementation, method GetFloat converts the string into a float value and
	 * returns it. Other data providers, such as MdsDataProvider, evaluate, possibly remotely, the
	 * passed string which may therefore be represented by an expression.
	 *
	 * @param in
	 *            The specification of the value.
	 * @return The evaluated value.
	 * @exception java.io.IOException
	 */
	public float getFloat(String in) throws IOException;

	/**
	 * GetFrameData is called by jScope to retrieve and display a frame sequence. The frame sequence
	 * is returned by means of an object implementing the FramesData interface.
	 * The methods defined in the FrameData interface are the following: <br>
	 * - int GetFrameType() returning the type of the corresponding frames, as returned by GetFrameAt, which can be: <br>
	 * -FramesData.BITMAP if the frames are returned by method GetFrameAt as a byte matrix <br>
	 * -FrameData.AWT_IMAGE if the frames are returned by method GetFrameAt as the <br>
	 * content of a gif or jpg file <br>
	 * -FrameData.JAI_IMAGE if the frames are returned by method GetFrameAt as the content of
	 * every other file format supported by JAI. <br>
	 * - int GetNumFrames() returning the number of frames in the sequence <br>
	 * - Dimension GetFrameDimension() returning the dimension of the single frame <br>
	 * - float [] GetFrameTimes returning the times associated with each frame <br>
	 * - byte[] GetFrameAt(int idx) returning the corresponding frame
	 *
	 * @param in_frame
	 *            The frame sequence specification as defined in the frames field of the setup data popup dialog
	 * @param start_time
	 *            Initial considered time for the frame sequence.
	 * @param end_time
	 *            Final considered time for the frame sequence.
	 * @param in_times
	 *            The definition of the time specification for the frame list as defined in the times field
	 *            of the setup data popup dialog.
	 * @return An implementation of the FrameData interface representing the corresponding frame sequence
	 * @exception java.io.IOException
	 * @see FrameData
	 */
	public FrameData getFrameData(String in_frame, String in_times, float start_time, float end_time) throws IOException;

	public Vector<LabeledProperty> getLabeledProperties();

	/**
	 * Format of the Legend entry.
	 *
	 * @param l
	 *            The previously registered ConnectionListener.
	 * @see ConnectionListener
	 */
	public String getLegendString(String s);

	/**
	 * Evaluate the passed string to provide am array of shot numbers.
	 * For MDSplus data provider it will carry out the execution of the corresponding expression
	 */
	public int[] getShots(String in) throws IOException;

	/**
	 * GetString is called by jScope to evaluate title, X label and Y label. These labels are defined
	 * as string in the Setup data source popup form. In the simplest case labels are exactly
	 * the same as defined, and therefore GetString simply returns its argument.
	 * In any case the data provider is free to interpret differently the argument.
	 *
	 * @param in
	 *            The specification of the label or title.
	 * @return The evaluated label or title.
	 * @exception java.io.IOException
	 */
	public String getString(String in) throws IOException;

	/**
	 * Method GetWaveData is called by jScope when a waveform has to be evaluated and only the Y
	 * axis is defined. In this case jScope assumes that the specification is enough and it is up
	 * to the data provider implementation to retrieve X and Y axis.
	 * The evaluated signal is not directly returned as a vector, rather as a object implementing
	 * the WaveData interface. The WaveData interface defines the following methods: <br>
	 * - int GetNumDimensions() returns the number of dimensions. Currently only signals (dimension=1)
	 * are supported. <br>
	 * - float[] GetFloatData() returns the Y axis of the signal as a float array. For bidimensional
	 * signals (not yet supported) the array is organized in row order; <br>
	 * - float[] GetXData() returns the X axis of the signal; <br>
	 * - float[] GetYData() returns the Y axis for a bidimensional signal; <br>
	 * - String GetTitle() returns the associated title (if no title is defined in the setup data popup dialog); <br>
	 * - String GetXLabel() returns the associated X label(if no X label is defined in the setup data popup dialog); <br>
	 * - String GetYLabel() returns the associated Y label(if no Y label is defined in the setup data popup dialog); <br>
	 * - String GetZLabel() returns the associated Z label(if no Z label is defined in the setup data popup dialog);
	 *
	 * @param in
	 *            The specification of the signal, typed in the Y axis field of the setup data source
	 *            popup form, or in the lower right window of jScope.
	 * @return The evaluated signal, embedded in a WaveData object, or null if an error is encountered.
	 * @see WaveData
	 */
	public WaveData getWaveData(String in);

	/**
	 * Method GetWaveData is called by jScope when a waveform has to be evaluated and both X and
	 * Y axis are defined.
	 * The evaluated signal is not directly returned as a vector, rather as a object implementing
	 * the WaveData interface. jScope the uses the returned object to retrieve X and Y axis.
	 *
	 * @param in_y
	 *            Y axis specification as typed in the setup data source popup dialog.
	 * @param in_x
	 *            X axis specification as typed in the setup data source popup dialog.
	 * @return The evaluated signal, embedded in a WaveData object, or null if an error is encountered.
	 * @see WaveData
	 */
	public WaveData getWaveData(String in_y, String in_x);

	/**
	 * Called by jScope when transfer notification is no more requested. Empty if the DataProvider
	 * implementation does not support transfer status notification.
	 *
	 * @param l
	 *            The previously registered ConnectionListener.
	 * @see ConnectionListener
	 */
	public void removeProviderEventListener(ProviderEventListener l);

	/**
	 * Method RemoveUpdateEventListeneris called by jScope when the display of a waveform panel is
	 * no more triggered by an event.
	 *
	 * @param l
	 *            The instance of UpdateEventListener previously registered.
	 * @param event
	 *            The event to which the listener was previously registered.
	 * @exception java.io.IOException
	 * @see UpdateEventListener
	 */
	public void removeUpdateEventListener(UpdateEventListener l, String event) throws IOException;

	/**
	 * if supported this will invoke to abort the signal download
	 */
	public void reset();

	/**
	 * As DataProvider implementations are instantiated by jScope by means of the
	 * Class.newInstance(), no arguments can be passed to the constructor method.
	 * If an additional argument is required for the proper initialization of the
	 * DataProvider implementation (e.g. the ip address for the MdsDataProvider), the argument,
	 * defined in the server_n.argument item of the property file is passed through method SetArgument
	 * called by jScope just after the DataProvider instantiation.
	 *
	 * @param arg
	 *            The argument passed to the DataProvider implementation.
	 * @exception java.io.IOException
	 */
	public int setArguments(final JFrame f, final DataServerItem si) throws IOException;

	/**
	 * Defines the default node
	 *
	 * @param in_def_node
	 */
	public void setDefault(String in_def_node);

	/**
	 * Defines the environment for data retrieval.
	 * Depending on the nature of the provider of data, some environment variable may be defined.
	 * For example the MdsDataProvider allows the definition of TDI variables which may be useful
	 * for configuring remote data access.
	 * jScope allows the definition of an arbitrary set of environment variable using the
	 * customize->public variables... option. As jScope does not make any assumption on the
	 * syntax of the defined variables, each variable definition is passed to the Data provider
	 * by means of the SetEnvironment method, whose argument is a String which defines <name value>
	 * pairs. If the DataProvider implementation does not support such a feature, it simply returns.
	 *
	 * @param exp
	 *            The variable definition expressed as <name value> pair.
	 * @exception java.io.IOException
	 */
	public void setEnvironment(Map<String, String> exp) throws IOException;

	/**
	 * Method Update is called by jScope to notify the experiment name and the shot number.
	 * Update can be called several time by jScope for the same DataProvider implementation
	 * in the case user changes either the experiment name or the shot number.
	 *
	 * @param expt
	 *            The experiment name
	 * @param shot
	 *            The shot number.
	 */
	public void update(String expt, int shot);
}
