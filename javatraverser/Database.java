// package jTraverser;
public class Database implements RemoteTree{
    static{
        try{
            // System.loadLibrary("MdsShr");
            // System.loadLibrary("MdsIpShr");
            // System.loadLibrary("TreeShr");
            // System.loadLibrary("TdiShr");
            System.loadLibrary("JavaMds");
        }catch(final Exception exc){
            jTraverser.stderr("Cannot load library ", exc);
            exc.printStackTrace();
        }
    }
    boolean is_editable = false;
    boolean is_open     = false;
    boolean is_readonly = false;
    boolean is_realtime = false;
    String  name;
    int     shot;

    public Database(){
        super();
    }

    public Database(final String name, final int shot){
        this.name = name.toUpperCase();
        this.shot = shot;
    }

    @Override
    public native NidData addDevice(String path, String model, int ctx) throws DatabaseException;

    @Override
    public native NidData addNode(String name, int usage, int ctx) throws DatabaseException;

    @Override
    public native void clearFlags(NidData nid, int flags) throws DatabaseException;

    @Override
    public native void close(int ctx) throws DatabaseException;

    @Override
    public native int create(int shot) throws DatabaseException;

    @Override
    public Data dataFromExpr(final String expr) {
        return Data.fromExpr(expr);
    }

    @Override
    public String dataToString(final Data data) {
        return data.toString();
    }

    @Override
    public native int doAction(NidData nid, int ctx) throws DatabaseException;

    @Override
    public native void doDeviceMethod(NidData nid, String method, int ctx) throws DatabaseException;

    @Override
    public Data evaluateData(final Data data, final int ctx) throws DatabaseException {
        return this.evaluateSimpleData(data, ctx);
    }

    @Override
    public native Data evaluateData(NidData nid, int ctx) throws DatabaseException;

    public native Data evaluateSimpleData(Data data, int ctx) throws DatabaseException;

    @Override
    public native void executeDelete(int ctx) throws DatabaseException;

    @Override
    public int getCurrentShot() {
        return this.getCurrentShot(this.name);
    }

    @Override
    public native int getCurrentShot(String experiment);

    @Override
    public native Data getData(NidData nid, int ctx) throws DatabaseException;

    @Override
    public native NidData getDefault(int ctx) throws DatabaseException;

    @Override
    public native int getFlags(NidData nid) throws DatabaseException;

    @Override
    public native NodeInfo getInfo(NidData nid, int ctx) throws DatabaseException;

    public native String getMdsMessage(int status);

    @Override
    public native NidData[] getMembers(NidData nid, int ctx) throws DatabaseException;

    @Override
    final public String getName() {
        return this.name;
    }

    public native String getOriginalPartName(NidData nid) throws DatabaseException;

    @Override
    final public int getShot() {
        return this.shot;
    }

    @Override
    public native NidData[] getSons(NidData nid, int ctx) throws DatabaseException;

    @Override
    public native String[] getTags(NidData nid, int ctx);

    @Override
    public native NidData[] getWild(int usage_mask, int ctx) throws DatabaseException;

    @Override
    public boolean isEditable() {
        return this.is_editable;
    }

    @Override
    public native boolean isOn(NidData nid, int ctx) throws DatabaseException;

    @Override
    public boolean isOpen() {
        return this.is_open;
    }

    @Override
    public boolean isReadonly() {
        return this.is_readonly;
    }

    @Override
    public boolean isRealtime() {
        return this.is_realtime;
    }

    /* Low level MDS database management routines, will be  masked by the Node class*/
    @Override
    public native int open() throws DatabaseException;

    public native int openNew() throws DatabaseException;

    @Override
    public native void putData(NidData nid, Data data, int ctx) throws DatabaseException;

    @Override
    public native void putRow(NidData nid, Data data, long time, int ctx) throws DatabaseException;

    @Override
    public native void quit(int ctx) throws DatabaseException;

    @Override
    public native void renameNode(NidData nid, String name, int ctx) throws DatabaseException;

    @Override
    public native NidData resolve(PathData pad, int ctx) throws DatabaseException;

    public native void restoreContext(long context);

    public native long saveContext();

    @Override
    public void setCurrentShot(final int shot) {
        this.setCurrentShot(this.name, shot);
    }

    @Override
    public native void setCurrentShot(String experiment, int shot);

    @Override
    public native void setDefault(NidData nid, int ctx) throws DatabaseException;

    @Override
    public void setEditable(final boolean editable) {
        this.is_editable = editable;
    }

    @Override
    public native void setEvent(String event) throws DatabaseException;

    @Override
    public native void setFlags(NidData nid, int flags) throws DatabaseException;

    @Override
    public native void setOn(NidData nid, boolean on, int ctx) throws DatabaseException;

    @Override
    public void setReadonly(final boolean readonly) {
        this.is_readonly = readonly;
    }

    @Override
    public void setRealtime(final boolean realtime) {
        this.is_realtime = realtime;
    }

    @Override
    public native void setSubtree(NidData nid, int ctx) throws DatabaseException;

    @Override
    public native void setTags(NidData nid, String tags[], int ctx) throws DatabaseException;

    @Override
    public void setTree(final String name, final int shot) {
        this.name = name.toUpperCase();
        this.shot = shot;
    }

    @Override
    public native NidData[] startDelete(NidData nid[], int ctx) throws DatabaseException;

    @Override
    public native void write(int ctx) throws DatabaseException;
}
