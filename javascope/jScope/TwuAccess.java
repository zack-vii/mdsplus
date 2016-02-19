package jScope;

/* $Id$ */
import java.io.IOException;
import java.util.StringTokenizer;

final public class TwuAccess implements DataAccess{
    String          ip_addr    = null;
    String          shot_str   = null;
    String          signal     = null;
    String          experiment = null;
    TwuDataProvider tw         = null;

    public static void main(String args[]) {
        TwuAccess access = new TwuAccess();
        String url = "twu://ipptwu.ipp.kfa-juelich.de/textor/all/86858/RT2/IVD/IBT2P-star";
        try{
            float y[] = access.getY(url);
            float x[] = access.getX(url);
            for(int i = 0; i < x.length; i++)
                System.out.println(x[i] + "  " + y[i]);
            System.out.println("Num. points: " + y.length);
        }catch(IOException exc){}
    }

    @Override
    public boolean supports(String url) {
        StringTokenizer st = new StringTokenizer(url, ":");
        if(st.countTokens() < 2) return false;
        return st.nextToken().equals("twu");
    }

    @Override
    public void setProvider(String url) throws IOException {
        signal = "http" + url.substring(url.indexOf(":"));
        StringTokenizer st = new StringTokenizer(url, "/");
        st.nextToken(); // skip
        st.nextToken(); // skip
        st.nextToken(); // skip
        st.nextToken(); // skip
        shot_str = st.nextToken();
        if(tw == null){
            tw = new TwuDataProvider("jScope applet (Version 7.2.2)");
        }
    }

    @Override
    public String getShot() {
        return shot_str;
    }

    @Override
    public String getSignalName() {
        return signal;
    }

    @Override
    public String getExperiment() {
        return experiment;
    }

    @Override
    public DataProvider getDataProvider() {
        return tw;
    }

    @Override
    public void close() {}

    @Override
    public void setPassword(String encoded_credentials) {}

    @Override
    public float[] getX(String url) throws IOException {
        setProvider(url);
        if(signal == null) return null;
        return tw.GetFloatArray(signal, true);
    }

    @Override
    public float[] getY(String url) throws IOException {
        setProvider(url);
        if(signal == null) return null;
        return tw.GetFloatArray(signal, false);
    }

    @Override
    public Signal getSignal(String url) throws IOException {
        setProvider(url);
        if(signal == null) return null;
        Signal s = null;
        float y[] = tw.GetFloatArray(signal, false);
        float x[] = tw.GetFloatArray(signal, true);
        if(x == null || y == null) return null;
        s = new Signal(x, y);
        s.setName(tw.GetSignalProperty("SignalName", signal));
        // System.out.println(tw.getSignalProperty("SignalName", signal));
        return s;
    }

    @Override
    public String getError() {
        if(tw == null) return("Cannot create TwuDataProvider");
        return tw.ErrorString();
    }

    @Override
    public FrameData getFrameData(String url) {
        return null;
    }
}
