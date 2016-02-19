package jScope;

/* $Id$ */
import jScope.DataProvider;
import jScope.FrameData;
import jScope.DataServerItem;
import jScope.ConnectionListener;
import java.io.*;
import javax.swing.JFrame;

final class NotConnectedDataProvider implements DataProvider
{
    final String error = "Not Connected";
    public WaveData GetWaveData(String in){return null;}
    public WaveData GetWaveData(String in_y, String in_x){return null;}
    public WaveData GetResampledWaveData(String in, double start, double end, int n_points){return null;}
    public WaveData GetResampledWaveData(String in_y, String in_x, double start, double end, int n_points){return null;}
    public void enableAsyncUpdate(boolean enable){}
    public void    Dispose(){}
    public boolean SupportsCompression(){return false;}
    public void    SetCompression(boolean state){}
    public boolean SupportsContinuous() { return false; }
    public int     InquireCredentials(JFrame f, DataServerItem server_item){return DataProvider.LOGIN_OK;}
    public boolean SupportsFastNetwork(){return false;}
    public void    SetArgument(String arg){}
    public boolean SupportsTunneling() {return false; }
    public void    SetEnvironment(String exp){}
    public void    Update(String exp, long s){}
    public String  GetString(String in){return "";}
    public double  GetFloat(String in){return new Double(in);}
    public float[] GetFloatArray(String in_x, String in_y, float start, float end){return null;}
    public float[] GetFloatArray(String in){return null;}
    public long[]  GetShots(String in){return new long[]{0L};}
    public String  ErrorString(){return error;}
    public void    AddUpdateEventListener(UpdateEventListener l, String event){}
    public void    RemoveUpdateEventListener(UpdateEventListener l, String event){}
    public void    AddConnectionListener(ConnectionListener l){}
    public void    RemoveConnectionListener(ConnectionListener l){}
    public void    setContinuousUpdate(){}
    public boolean DataPending(){return false;}
    public FrameData GetFrameData(String in_y, String in_x, float time_min, float time_max) throws IOException{return null;}
}