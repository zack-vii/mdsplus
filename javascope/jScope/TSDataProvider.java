package jScope;

/* $Id$ */
import java.io.IOException;
import javax.swing.JFrame;

final class TSDataProvider extends MdsDataProvider{
    public TSDataProvider(){
        super();
    }

    public TSDataProvider(String provider) throws IOException{
        super(provider);
    }

    @Override
    public void SetArgument(String arg) throws IOException {
        mds.setProvider(arg);
        mds.setUser("mdsplus");
    }

    @Override
    public synchronized void Update(String exp, long s) {
        error = null;
        shot = (int)s;
    }

    protected String ParseExpression(String in) {
        // if(in.startsWith("DIM_OF("))
        // return in;
        String res = MdsplusParser.parseFun(in, "GetTsBase(" + shot + ", \"", "\")");
        /*
         * StringTokenizer st = new StringTokenizer(in, ":"); String res = "GetTSData(\""; try{ String name = st.nextToken();
         */
        /*
         * String rang0 = st.nextToken(); String rang1 = st.nextToken(); res = "GetTSData(\"" + name + "\", " + shot + ", " + rang0 + ", " + rang1 + ")";
         */
        // res = "GetTsBase(" + shot + ", \"" + name + "\")";
        /*
         * }catch(Exception e) { error = "Wrong signal format: must be <signal_name>:<rangs[0]>:<rangs[1]>"; return null; }
         */
        // System.out.println(res);
        return res;
    }

    @Override
    public synchronized int[] GetIntArray(String in) throws IOException {
        String parsed = ParseExpression(in);
        if(parsed == null) return null;
        return super.GetIntArray(parsed);
    }

    @Override
    public synchronized float[] GetFloatArray(String in) throws IOException {
        String parsed = ParseExpression(in);
        if(parsed == null) return null;
        error = null;
        float[] out_array = super.GetFloatArray(parsed);
        if(out_array == null && error == null) error = "Cannot evaluate " + in + " for shot " + shot;
        if(out_array != null && out_array.length <= 1){
            error = "Cannot evaluate " + in + " for shot " + shot;
            return null;
        }
        return out_array;
    }

    protected String GetDefaultXLabel(String in_y) throws IOException {
        error = null;
        return GetString("GetTSUnit(0)");
    }

    protected String GetDefaultYLabel() throws IOException {
        error = null;
        return GetString("GetTSUnit(1)");
    }

    public static boolean SupportsCompression() {
        return false;
    }

    @Override
    public void SetCompression(boolean state) {}

    public static boolean SupportsContinuous() {
        return false;
    }

    @SuppressWarnings("static-method")
    public boolean DataPending() {
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
    public int[] GetNumDimensions(String spec) {
        return new int[]{1};
    }
}
