import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Vector;

class TreeServer extends UnicastRemoteObject implements RemoteTree{
    private static final long serialVersionUID = 7424995021534493197L;

    public static void main(final String args[]) {
        // if (System.getSecurityManager() == null)
        System.setSecurityManager(new SecurityManager(){
            @Override
            public void checkLink(final String lib) {}
        });
        RemoteTree server = null;
        try{
            server = new TreeServer();
        }catch(final Exception exc){
            jTraverser.stderr("Error creating TreeServer", exc);
            System.exit(0);
        }
        try{
            Naming.rebind("TreeServer", server);
        }catch(final Exception exc){
            jTraverser.stderr("Error in Naming.rebind", exc);
        }
    }
    Vector<Long>       contexts = new Vector<Long>();
    String             experiment;
    int                lastCtx  = 0;
    int                shot;
    transient Database tree;

    public TreeServer() throws RemoteException{
        super();
    }

    @Override
    public NidData addDevice(final String path, final String model, final int ctx) throws DatabaseException {
        this.setContext(ctx);
        final NidData nid = this.tree.addDevice(path, model, 0);
        return nid;
    }

    @Override
    public NidData addNode(final String name, final int usage, final int ctx) throws DatabaseException {
        this.setContext(ctx);
        final NidData nid = this.tree.addNode(name, usage, 0);
        return nid;
    }

    @Override
    public void clearFlags(final NidData nid, final int flags) throws DatabaseException {
        this.tree.clearFlags(nid, flags);
    }

    @Override
    public void close(final int ctx) throws DatabaseException {
        long lctx = this.contexts.elementAt(ctx).longValue();
        this.tree.restoreContext(lctx);
        this.contexts.setElementAt(new Long(0), ctx);
        this.tree.close(0);
        lctx = this.tree.saveContext();
        this.contexts.setElementAt(new Long(lctx), ctx);
    }

    @Override
    public int create(final int shot) throws DatabaseException {
        this.tree.create(shot);
        this.contexts.insertElementAt(new Long(this.tree.saveContext()), this.lastCtx);
        return this.lastCtx++;
    }

    @Override
    public Data dataFromExpr(final String expr) {
        return Data.fromExpr(expr);
    }

    @Override
    public String dataToString(final Data data) {
        return data.toString();
    }

    @Override
    public int doAction(final NidData nid, final int ctx) throws DatabaseException {
        this.setContext(ctx);
        return this.tree.doAction(nid, 0);
    }

    @Override
    public void doDeviceMethod(final NidData nid, final String method, final int ctx) throws DatabaseException {
        this.setContext(ctx);
        this.tree.doDeviceMethod(nid, method, 0);
    }

    @Override
    public Data evaluateData(final Data inData, final int ctx) throws DatabaseException {
        this.setContext(ctx);
        final Data data = this.tree.evaluateData(inData, 0);
        return data;
    }

    @Override
    public Data evaluateData(final NidData nid, final int ctx) throws DatabaseException {
        this.setContext(ctx);
        final Data data = this.tree.evaluateData(nid, 0);
        return data;
    }

    @Override
    public void executeDelete(final int ctx) throws DatabaseException {
        this.setContext(ctx);
        this.tree.executeDelete(0);
    }

    @Override
    public int getCurrentShot() throws RemoteException {
        return this.tree.getCurrentShot();
    }

    @Override
    public int getCurrentShot(final String experiment) throws RemoteException {
        return this.tree.getCurrentShot(experiment);
    }

    @Override
    public Data getData(final NidData nid, final int ctx) throws DatabaseException {
        this.setContext(ctx);
        final Data data = this.tree.getData(nid, 0);
        return data;
    }

    @Override
    public NidData getDefault(final int ctx) throws DatabaseException {
        this.setContext(ctx);
        final NidData nid = this.tree.getDefault(0);
        return nid;
    }

    @Override
    public int getFlags(final NidData nid) throws DatabaseException {
        return this.tree.getFlags(nid);
    }

    // public native DatabaseInfo getInfo(); throws DatabaseException;
    @Override
    public NodeInfo getInfo(final NidData nid, final int ctx) throws DatabaseException {
        this.setContext(ctx);
        final NodeInfo info = this.tree.getInfo(nid, 0);
        return info;
    }

    @Override
    public NidData[] getMembers(final NidData nid, final int ctx) throws DatabaseException {
        this.setContext(ctx);
        final NidData members[] = this.tree.getMembers(nid, 0);
        return members;
    }

    @Override
    public String getName() {
        return this.tree.getName();
    }

    @Override
    public int getShot() {
        return this.tree.getShot();
    }

    @Override
    public NidData[] getSons(final NidData nid, final int ctx) throws DatabaseException {
        this.setContext(ctx);
        final NidData[] sons = this.tree.getSons(nid, 0);
        return sons;
    }

    @Override
    public String[] getTags(final NidData nid, final int ctx) throws DatabaseException {
        this.setContext(ctx);
        final String tags[] = this.tree.getTags(nid, 0);
        return tags;
    }

    @Override
    public NidData[] getWild(final int usage_mask, final int ctx) throws DatabaseException {
        this.setContext(ctx);
        final NidData nids[] = this.tree.getWild(usage_mask, 0);
        return nids;
    }

    @Override
    public boolean isEditable() {
        return this.tree.isEditable();
    }

    @Override
    public boolean isOn(final NidData nid, final int ctx) throws DatabaseException {
        this.setContext(ctx);
        final boolean on = this.tree.isOn(nid, 0);
        return on;
    }

    @Override
    public boolean isOpen() {
        return this.tree.isOpen();
    }

    @Override
    public boolean isReadonly() {
        return this.tree.isReadonly();
    }

    @Override
    public boolean isRealtime() {
        return this.tree.isRealtime();
    }

    /* Low level MDS database management routines, will be  masked by the Node class*/
    @Override
    public int open() throws DatabaseException {
        jTraverser.stdout("Server: start open lastCtx = " + this.lastCtx);
        this.tree.open();
        this.contexts.insertElementAt(new Long(this.tree.saveContext()), this.lastCtx);
        return this.lastCtx++;
    }

    @Override
    public void putData(final NidData nid, final Data data, final int ctx) throws DatabaseException {
        this.setContext(ctx);
        this.tree.putData(nid, data, 0);
    }

    @Override
    public void putRow(final NidData nid, final Data data, final long time, final int ctx) throws DatabaseException {
        this.setContext(ctx);
        this.tree.putRow(nid, data, time, 0);
    }

    @Override
    public void quit(final int ctx) throws DatabaseException {
        long lctx = this.contexts.elementAt(ctx).longValue();
        this.tree.restoreContext(lctx);
        this.contexts.setElementAt(new Long(0), ctx);
        this.tree.quit(0);
        lctx = this.tree.saveContext();
        this.contexts.setElementAt(new Long(lctx), ctx);
    }

    @Override
    public void renameNode(final NidData nid, final String name, final int ctx) throws DatabaseException {
        this.setContext(ctx);
        this.tree.renameNode(nid, name, 0);
    }

    @Override
    public NidData resolve(final PathData pad, final int ctx) throws DatabaseException {
        this.setContext(ctx);
        final NidData nid = this.tree.resolve(pad, 0);
        return nid;
    }

    private void setContext(final int ctx) {
        long lctx = this.contexts.elementAt(ctx).longValue();
        this.tree.restoreContext(lctx);
        lctx = this.tree.saveContext();
        this.contexts.setElementAt(new Long(lctx), ctx);
    }

    @Override
    public void setCurrentShot(final int shot) throws RemoteException {
        this.tree.setCurrentShot(shot);
    }

    @Override
    public void setCurrentShot(final String experiment, final int shot) {
        this.tree.setCurrentShot(experiment, shot);
    }

    @Override
    public void setDefault(final NidData nid, final int ctx) throws DatabaseException {
        long lctx = this.contexts.elementAt(ctx).longValue();
        this.tree.restoreContext(lctx);
        this.tree.setDefault(nid, 0);
        lctx = this.tree.saveContext();
        this.contexts.setElementAt(new Long(lctx), ctx);
    }

    @Override
    public void setEditable(final boolean editable) {
        this.tree.setEditable(editable);
    }

    @Override
    public void setEvent(final String event) throws DatabaseException {
        this.tree.setEvent(event);
    }

    @Override
    public void setFlags(final NidData nid, final int flags) throws DatabaseException {
        this.tree.setFlags(nid, flags);
    }

    @Override
    public void setOn(final NidData nid, final boolean on, final int ctx) throws DatabaseException {
        this.setContext(ctx);
        this.tree.setOn(nid, on, 0);
    }

    @Override
    public void setReadonly(final boolean readonly) {
        this.tree.setReadonly(readonly);
    }

    @Override
    public void setRealtime(final boolean realtime) {
        this.tree.setRealtime(realtime);
    }

    @Override
    public void setSubtree(final NidData nid, final int ctx) throws DatabaseException {
        this.setContext(ctx);
        this.tree.setSubtree(nid, 0);
    }

    @Override
    public void setTags(final NidData nid, final String tags[], final int ctx) throws DatabaseException {
        this.setContext(ctx);
        this.tree.setTags(nid, tags, 0);
    }

    @Override
    public void setTree(final String experiment, final int shot) throws RemoteException {
        this.tree = new Database(experiment, shot);
    }

    @Override
    public NidData[] startDelete(final NidData nid[], final int ctx) throws DatabaseException {
        this.setContext(ctx);
        final NidData nids[] = this.tree.startDelete(nid, 0);
        return nids;
    }

    @Override
    public void write(final int ctx) throws DatabaseException {
        long lctx = this.contexts.elementAt(ctx).longValue();
        this.tree.restoreContext(lctx);
        this.contexts.setElementAt(new Long(0), ctx);
        this.tree.write(0);
        lctx = this.tree.saveContext();
        this.contexts.setElementAt(new Long(lctx), ctx);
    }
}
