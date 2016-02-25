import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteTree extends Remote{
    public NidData addDevice(String path, String model, int ctx) throws RemoteException, DatabaseException;

    public NidData addNode(String name, int usage, int ctx) throws RemoteException, DatabaseException;

    public void clearFlags(NidData nid, int flags) throws RemoteException, DatabaseException;

    public void close(int ctx) throws RemoteException, DatabaseException;

    public int create(int shot) throws RemoteException, DatabaseException;

    public Data dataFromExpr(String expr) throws RemoteException;

    public String dataToString(Data data) throws RemoteException;

    public int doAction(NidData nid, int ctx) throws RemoteException, DatabaseException;

    public void doDeviceMethod(NidData nid, String method, int ctx) throws RemoteException, DatabaseException;

    public Data evaluateData(Data data, int ctx) throws RemoteException, DatabaseException;

    public Data evaluateData(NidData nid, int ctx) throws RemoteException, DatabaseException;

    public void executeDelete(int ctx) throws RemoteException, DatabaseException;

    public int getCurrentShot() throws RemoteException;

    public int getCurrentShot(String experiment) throws RemoteException;

    public Data getData(NidData nid, int ctx) throws RemoteException, DatabaseException;

    public NidData getDefault(int ctx) throws RemoteException, DatabaseException;

    public int getFlags(NidData nid) throws RemoteException, DatabaseException;

    // public native DatabaseInfo getInfo(); throws DatabaseException;
    public NodeInfo getInfo(NidData nid, int ctx) throws RemoteException, DatabaseException;

    public NidData[] getMembers(NidData nid, int ctx) throws RemoteException, DatabaseException;

    public String getName() throws RemoteException;

    public int getShot() throws RemoteException;

    public NidData[] getSons(NidData nid, int ctx) throws RemoteException, DatabaseException;

    public String[] getTags(NidData nid, int ctx) throws RemoteException, DatabaseException;

    public NidData[] getWild(int usage_mask, int ctx) throws RemoteException, DatabaseException;

    boolean isEditable() throws RemoteException;

    public boolean isOn(NidData nid, int ctx) throws RemoteException, DatabaseException;

    boolean isOpen() throws RemoteException;

    boolean isReadonly() throws RemoteException;

    boolean isRealtime() throws RemoteException;

    /* Low level MDS database management routines, will be  masked by the Node class*/
    public int open() throws RemoteException, DatabaseException;

    public void putData(NidData nid, Data data, int ctx) throws RemoteException, DatabaseException;

    public void putRow(NidData nid, Data data, long time, int ctx) throws RemoteException, DatabaseException;

    public void quit(int ctx) throws RemoteException, DatabaseException;

    public void renameNode(NidData nid, String name, int ctx) throws RemoteException, DatabaseException;

    public NidData resolve(PathData pad, int ctx) throws RemoteException, DatabaseException;

    public void setCurrentShot(int shot) throws RemoteException;

    public void setCurrentShot(String experiment, int shot) throws RemoteException;

    public void setDefault(NidData nid, int ctx) throws RemoteException, DatabaseException;

    void setEditable(boolean editable) throws RemoteException;

    public void setEvent(String event) throws RemoteException, DatabaseException;

    public void setFlags(NidData nid, int flags) throws RemoteException, DatabaseException;

    public void setOn(NidData nid, boolean on, int ctx) throws RemoteException, DatabaseException;

    void setReadonly(boolean readonly) throws RemoteException;

    void setRealtime(boolean realtime) throws RemoteException;

    public void setSubtree(NidData nid, int ctx) throws RemoteException, DatabaseException;

    public void setTags(NidData nid, String tags[], int ctx) throws RemoteException, DatabaseException;

    public void setTree(String experiment, int shot) throws RemoteException;

    public NidData[] startDelete(NidData nid[], int ctx) throws RemoteException, DatabaseException;

    public void write(int ctx) throws RemoteException, DatabaseException;
}
