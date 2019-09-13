package jscope.data;

/* $Id$ */
import java.io.IOException;
import java.util.Map;
import java.util.Vector;
import javax.swing.JFrame;
import jscope.SignalBrowser;
import jscope.data.framedata.FrameData;
import jscope.data.wavedata.WaveData;

public final class NotConnectedDataProvider implements DataProvider{
	private static final String error = "Not Connected";

	@Override
	public void addProviderEventListener(final ProviderEventListener l) {/**/}

	@Override
	public void addUpdateEventListener(final UpdateEventListener l, final String name) {/**/}

	@Override
	public boolean checkProvider() {
		return true;
	}

	@Override
	public void dispose() {/**/}

	@Override
	public String errorString() {
		return NotConnectedDataProvider.error;
	}

	@Override
	public Class<SignalBrowser> getDefaultBrowser() {
		return null;
	}

	@Override
	public float getFloat(final String in) {
		return Float.parseFloat(in);
	}

	@Override
	public FrameData getFrameData(final String in_y, final String in_x, final float time_min, final float time_max) throws IOException {
		return null;
	}

	@Override
	public Vector<LabeledProperty> getLabeledProperties() {
		return null;
	}

	@Override
	public final String getLegendString(final String s) {
		return s;
	}

	@Override
	public int[] getShots(final String in) {
		return new int[]{-1};
	}

	@Override
	public String getString(final String in) {
		return in;
	}

	@Override
	public WaveData getWaveData(final String in) {
		return null;
	}

	@Override
	public WaveData getWaveData(final String in_y, final String in_x) {
		return null;
	}

	@Override
	public void removeProviderEventListener(final ProviderEventListener l) {/**/}

	@Override
	public void removeUpdateEventListener(final UpdateEventListener l, final String event) {/**/}

	@Override
	public void reset() {/**/}

	@Override
	public int setArguments(final JFrame f, final DataServerItem si) throws IOException {
		return DataProvider.LOGIN_OK;
	}

	@Override
	public void setDefault(final String in_def_node) {/**/}

	@Override
	public void setEnvironment(final Map<String, String> env) {/**/}

	@Override
	public void update(final String exp, final int s) {/**/}
}