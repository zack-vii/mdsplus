import java.util.Date;

class Action{
    static final int ABORTED                  = 5;
    static final int DISPATCHED               = 2;
    static final int DOING                    = 3;
    static final int DONE                     = 4;
    static final int NOT_DISPATCHED           = 1;
    static final int ServerCANT_HAPPEN        = 0xfe1801a;
    static final int ServerINVALID_DEPENDENCY = 0xfe18012;
    static final int ServerNOT_DISPATCHED     = 0xfe18008;
    ActionData       action;
    int              dispatch_status;
    boolean          essential                = false;
    boolean          manual                   = false;
    String           name;
    int              nid;
    boolean          on;
    String           server_address           = "DUMMY";
    int              status;
    int              timestamp;

    public Action(final ActionData action, final int nid, final String name, final boolean on, final boolean essential, final String server_address){
        this.action = action;
        this.nid = nid;
        this.name = name;
        this.on = on;
        this.dispatch_status = Action.NOT_DISPATCHED;
        this.status = 0;
        this.server_address = server_address;
        this.essential = essential;
    }

    public synchronized ActionData getAction() {
        return this.action;
    }

    public synchronized int getDispatchStatus() {
        return this.dispatch_status;
    }

    public synchronized String getName() {
        return this.name;
    }

    public synchronized int getNid() {
        return this.nid;
    }

    public synchronized String getServerAddress() {
        return this.server_address;
    }

    public synchronized int getStatus() {
        return this.status;
    }

    public synchronized boolean isEssential() {
        return this.essential;
    }

    public synchronized boolean isManual() {
        return this.manual;
    }

    public synchronized boolean isOn() {
        return this.on;
    }

    public synchronized void setManual(final boolean manual) {
        this.manual = manual;
    }

    public synchronized void setServerAddress(final String server_address) {
        this.server_address = server_address;
    }

    synchronized void setStatus(final int status) {
        this.status = status;
    }

    synchronized void setStatus(final int dispatch_status, final int status, final boolean verbose) {
        String server;
        this.status = status;
        this.dispatch_status = dispatch_status;
        if(verbose){
            try{
                server = ((DispatchData)this.action.getDispatch()).getIdent().getString();
            }catch(final Exception e){
                server = "";
            }
            switch(dispatch_status){
                case DISPATCHED:
                    System.out.println("" + new Date() + " Dispatching node " + this.name + "(" + this.nid + ")" + " to " + server);
                    break;
                case DOING:
                    System.out.println("" + new Date() + " " + server + " is beginning action " + this.name);
                    break;
                case DONE:
                    if((status & 1) != 0) System.out.println("" + new Date() + " Action " + this.name + " completed  ");
                    else System.out.println("" + new Date() + " Action " + this.name + " failed  " + MdsHelper.getErrorString(status));
                    break;
                case ABORTED:
                    System.out.println("" + new Date() + " Action " + this.name + " aborted");
                    break;
            }
        }
    }

    // public int getTimestamp() {return timestamp; }
    public synchronized void setTimestamp(final int timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public synchronized String toString() {
        return this.name;
    }
}
