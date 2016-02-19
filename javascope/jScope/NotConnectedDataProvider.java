package jScope;

/* $Id$ */
import java.io.IOException;
import javax.swing.JFrame;

final class NotConnectedDataProvider implements DataProvider{
    final String error = "Not Connected";

    @Override
    public WaveData GetWaveData(String in) {
        return null;
    }

    @Override
    public WaveData GetWaveData(String in_y, String in_x) {
        return null;
    }

    public static WaveData GetResampledWaveData(String in, double start, double end, int n_points) {
        return null;
    }

    public static WaveData GetResampledWaveData(String in_y, String in_x, double start, double end, int n_points) {
        return null;
    }

    public void enableAsyncUpdate(boolean enable) {}

    @Override
    public void Dispose() {}

    public static boolean SupportsCompression() {
        return false;
    }

    public static void SetCompression(boolean state) {}

    public static boolean SupportsContinuous() {
        return false;
    }

    @Override
    public int InquireCredentials(JFrame f, DataServerItem server_item) {
        return DataProvider.LOGIN_OK;
    }

    public static boolean SupportsFastNetwork() {
        return false;
    }

    @Override
    public void SetArgument(String arg) {}

    @Override
    public boolean SupportsTunneling() {
        return false;
    }

    @Override
    public void SetEnvironment(String exp) {}

    @Override
    public void Update(String exp, long s) {}

    @Override
    public String GetString(String in) {
        return "";
    }

    @Override
    public double GetFloat(String in) {
        return new Double(in);
    }

    public static float[] GetFloatArray(String in_x, String in_y, float start, float end) {
        return null;
    }

    public static float[] GetFloatArray(String in) {
        return null;
    }

    @Override
    public long[] GetShots(String in) {
        return new long[]{0L};
    }

    @Override
    public String ErrorString() {
        return error;
    }

    @Override
    public void AddUpdateEventListener(UpdateEventListener l, String event) {}

    @Override
    public void RemoveUpdateEventListener(UpdateEventListener l, String event) {}

    @Override
    public void AddConnectionListener(ConnectionListener l) {}

    @Override
    public void RemoveConnectionListener(ConnectionListener l) {}

    public static void setContinuousUpdate() {}

    public static boolean DataPending() {
        return false;
    }

    @Override
    public FrameData GetFrameData(String in_y, String in_x, float time_min, float time_max) throws IOException {
        return null;
    }
}