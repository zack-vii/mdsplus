package jScope;

/* $Id$ */
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Hashtable;
import java.util.Vector;

public class MdsConnection{
    static class EventItem{
        int                         eventid;
        Vector<UpdateEventListener> listener = new Vector<UpdateEventListener>();
        String                      name;

        public EventItem(final String name, final int eventid, final UpdateEventListener l){
            this.name = name;
            this.eventid = eventid;
            this.listener.addElement(l);
        }

        @Override
        public String toString() {
            return new String("Event name = " + this.name + " Event id = " + this.eventid);
        }
    }
    class MRT extends Thread // Mds Receive Thread
    {
        boolean    killed  = false;
        MdsMessage message;
        boolean    pending = false;

        public synchronized MdsMessage GetMessage() {
            // System.out.println("Get Message");
            while(!this.killed && this.message == null)
                try{
                    this.wait();
                }catch(final InterruptedException exc){}
            if(this.killed) return null;
            final MdsMessage msg = this.message;
            this.message = null;
            return msg;
        }

        @Override
        public void run() {
            this.setName("Mds Receive Thread");
            MdsMessage curr_message;
            try{
                while(true){
                    curr_message = new MdsMessage("", MdsConnection.this.connection_listener);
                    curr_message.Receive(MdsConnection.this.dis);
                    if(curr_message.dtype == Descriptor.DTYPE_EVENT){
                        final PMET PMdsEvent = new PMET();
                        PMdsEvent.SetEventid(curr_message.body[12]);
                        PMdsEvent.start();
                    }else{
                        MdsConnection.this.pending_count--;
                        synchronized(this){
                            this.message = curr_message;
                            if(MdsConnection.this.pending_count == 0) this.notify();
                        }
                        curr_message = null;
                        // if(pending_count == 0)
                        // MdsConnection.this.NotifyMessage();
                    }
                }
            }
            // catch(IOException e) CESARE 14/9/2015
            catch(final Exception e){
                synchronized(this){
                    this.killed = true;
                    this.notifyAll();
                }
                if(MdsConnection.this.connected){
                    this.message = null;
                    MdsConnection.this.connected = false;
                    // ConnectionEvent ce = new ConnectionEvent(MdsConnection.this, ConnectionEvent.LOST_CONNECTION, "Lost connection from : "+provider);
                    (new Thread(){
                        @Override
                        public void run() {
                            final ConnectionEvent ce = new ConnectionEvent(MdsConnection.this, ConnectionEvent.LOST_CONNECTION, "Lost connection from : " + MdsConnection.this.provider);
                            MdsConnection.this.dispatchConnectionEvent(ce);
                        }
                    }).start();
                    // MdsConnection.this.dispatchConnectionEvent(ce);
                    // MdsConnection.this.NotifyMessage();
                }
            }
        }

        public synchronized void waitExited() {
            while(!this.killed)
                try{
                    this.wait();
                }catch(final InterruptedException exc){}
        }
    } // End MRT class
    class PMET extends Thread // Process Mds Event Thread
    {
        int    eventId = -1;
        String eventName;

        @Override
        public void run() {
            this.setName("Process Mds Event Thread");
            if(jScopeFacade.busy()) return;
            if(this.eventName != null) MdsConnection.this.dispatchUpdateEvent(this.eventName);
            else if(this.eventId != -1) MdsConnection.this.dispatchUpdateEvent(this.eventId);
        }

        public void SetEventid(final int id) {
            if(DEBUG.M){
                System.out.println("Received Event ID " + id);
            }
            this.eventId = id;
            this.eventName = null;
        }

        public void SetEventName(final String name) {
            if(DEBUG.M){
                System.out.println("Received Event Name " + name);
            }
            this.eventId = -1;
            this.eventName = name;
        }
    }// end PMET class
    static public int                       DEFAULT_PORT        = 8000;
    static public String                    DEFAULT_USER        = "JAVA_USER";
    static final int                        MAX_NUM_EVENTS      = 256;
    public boolean                          connected;
    transient Vector<ConnectionListener>    connection_listener = new Vector<ConnectionListener>();
    // protected DataInputStream dis;
    protected InputStream                   dis;
    protected DataOutputStream              dos;
    public String                           error;
    transient boolean                       event_flags[]       = new boolean[MdsConnection.MAX_NUM_EVENTS];
    transient Vector<EventItem>             event_list          = new Vector<EventItem>();
    transient Hashtable<Integer, EventItem> hashEventId         = new Hashtable<Integer, EventItem>();
    transient Hashtable<String, EventItem>  hashEventName       = new Hashtable<String, EventItem>();
    protected String                        host;
    int                                     pending_count       = 0;
    protected int                           port;
    protected String                        provider;
    MRT                                     receiveThread;
    protected Socket                        sock;
    protected String                        user;

    /*
    private synchronized void NotifyMessage() {
        notify();
        System.out.printf("-- Notify");
    }
     */
    public MdsConnection(){
        this.connected = false;
        this.sock = null;
        this.dis = null;
        this.dos = null;
        this.provider = null;
        this.port = MdsConnection.DEFAULT_PORT;
        this.host = null;
        // processUdpEvent = new ProcessUdpEvent();
        // processUdpEvent.start();
    }

    public MdsConnection(final String provider){
        this.connected = false;
        this.sock = null;
        this.dis = null;
        this.dos = null;
        this.provider = provider;
        this.port = MdsConnection.DEFAULT_PORT;
        this.host = null;
        // processUdpEvent = new ProcessUdpEvent();
        // processUdpEvent.start();
    }

    public synchronized void addConnectionListener(final ConnectionListener l) {
        if(l == null) return;
        this.connection_listener.addElement(l);
    }

    public synchronized int AddEvent(final UpdateEventListener l, final String eventName) {
        int eventid = -1;
        EventItem eventItem;
        if(this.hashEventName.containsKey(eventName)){
            eventItem = this.hashEventName.get(eventName);
            if(!eventItem.listener.contains(l)) eventItem.listener.addElement(l);
        }else{
            eventid = this.getEventId();
            eventItem = new EventItem(eventName, eventid, l);
            this.hashEventName.put(eventName, eventItem);
            this.hashEventId.put(new Integer(eventid), eventItem);
        }
        return eventid;
    }

    public synchronized int ConnectToMds(final boolean use_compression) {
        try{
            if(this.provider != null){
                this.connectToServer();
                final MdsMessage message = new MdsMessage(this.user);
                message.useCompression(use_compression);
                message.Send(this.dos);
                message.Receive(this.dis);
                // NOTE Removed check, unsuccessful in UDT
                // if((message.status & 1) != 0)
                // if(true){
                this.receiveThread = new MRT();
                this.receiveThread.start();
                this.connected = true;
                return 1;
            }
            // error = "Could not get IO for : Host " + host +" Port "+ port + " User " + user;
            this.error = "Data provider host:port is <null>";
        }catch(final NumberFormatException e){
            this.error = "Data provider syntax error " + this.provider + " (host:port)";
        }catch(final UnknownHostException e){
            this.error = "Data provider: " + this.host + " port " + this.port + " unknown";
        }catch(final IOException e){
            this.error = "Could not get IO for " + this.provider + " " + e;
        }
        return 0;
    }

    public void connectToServer() throws IOException {
        this.host = this.getProviderHost();
        this.port = this.getProviderPort();
        this.user = this.getProviderUser();
        this.sock = new Socket(this.host, this.port);
        this.sock.setTcpNoDelay(true);
        this.dis = new BufferedInputStream(this.sock.getInputStream());
        this.dos = new DataOutputStream(new BufferedOutputStream(this.sock.getOutputStream()));
    }

    public int DisconnectFromMds() {
        try{
            if(this.connection_listener.size() > 0) this.connection_listener.removeAllElements();
            this.dos.close();
            this.dis.close();
            this.receiveThread.waitExited();
            this.connected = false;
        }catch(final IOException e){
            this.error.concat("Could not get IO for " + this.provider + e);
            return 0;
        }
        return 1;
    }

    protected void dispatchConnectionEvent(final ConnectionEvent e) {
        if(this.connection_listener != null) for(int i = 0; i < this.connection_listener.size(); i++)
            this.connection_listener.elementAt(i).processConnectionEvent(e);
    }

    private void dispatchUpdateEvent(final EventItem eventItem) {
        final Vector<UpdateEventListener> eventListener = eventItem.listener;
        final UpdateEvent e = new UpdateEvent(this, eventItem.name);
        for(int i = 0; i < eventListener.size(); i++)
            eventListener.elementAt(i).processUpdateEvent(e);
    }

    public synchronized void dispatchUpdateEvent(final int eventid) {
        if(this.hashEventId.containsKey(eventid)) this.dispatchUpdateEvent(this.hashEventId.get(eventid));
    }

    public synchronized void dispatchUpdateEvent(final String eventName) {
        if(this.hashEventName.containsKey(eventName)) this.dispatchUpdateEvent(this.hashEventName.get(eventName));
    }

    public synchronized Descriptor getAnswer() throws IOException {
        final Descriptor out = new Descriptor();
        // wait();//!!!!!!!!!!
        final MdsMessage message = this.receiveThread.GetMessage();
        if(message == null || message.length == 0){
            out.error = "Null response from server";
            return out;
        }
        out.status = message.status;
        switch((out.dtype = message.dtype)){
            case Descriptor.DTYPE_UBYTE:
            case Descriptor.DTYPE_BYTE:
                out.byte_data = message.body;
                break;
            case Descriptor.DTYPE_USHORT:
                out.int_data = message.ToUShortArray();
                out.dtype = Descriptor.DTYPE_LONG;
                break;
            case Descriptor.DTYPE_SHORT:
                out.short_data = message.ToShortArray();
                break;
            case Descriptor.DTYPE_LONG:
            case Descriptor.DTYPE_ULONG:
                out.int_data = message.ToIntArray();
                break;
            case Descriptor.DTYPE_ULONGLONG:
            case Descriptor.DTYPE_LONGLONG:
                out.long_data = message.ToLongArray();
                break;
            case Descriptor.DTYPE_CSTRING:
                if((message.status & 1) == 1) out.strdata = new String(message.body);
                else out.error = new String(message.body);
                break;
            case Descriptor.DTYPE_FLOAT:
                out.float_data = message.ToFloatArray();
                break;
            case Descriptor.DTYPE_DOUBLE:
                out.double_data = message.ToDoubleArray();
                break;
        }
        return out;
    }

    private int getEventId() {
        int i;
        for(i = 0; i < MdsConnection.MAX_NUM_EVENTS && this.event_flags[i]; i++);
        if(i == MdsConnection.MAX_NUM_EVENTS) return -1;
        this.event_flags[i] = true;
        return i;
    }

    // ProcessUdpEvent processUdpEvent = null;
    public String getProvider() {
        return this.provider;
    }

    public synchronized String getProviderHost() {
        if(this.provider == null) return null;
        String address = this.provider;
        final int idx = this.provider.indexOf("|");
        int idx_1 = this.provider.indexOf(":");
        if(idx_1 == -1) idx_1 = this.provider.length();
        if(idx != -1) address = this.provider.substring(idx + 1, idx_1);
        else address = this.provider.substring(0, idx_1);
        return address;
    }

    public synchronized int getProviderPort() throws NumberFormatException {
        if(this.provider == null) return MdsConnection.DEFAULT_PORT;
        int port = MdsConnection.DEFAULT_PORT;
        final int idx = this.provider.indexOf(":");
        if(idx != -1) port = Integer.parseInt(this.provider.substring(idx + 1, this.provider.length()));
        return port;
    }

    public String getProviderUser() {
        return(this.user != null ? this.user : MdsConnection.DEFAULT_USER);
    }

    public synchronized void MdsRemoveEvent(final UpdateEventListener l, final String event) {
        int eventid;
        if((eventid = this.RemoveEvent(l, event)) == -1) return;
        try{
            this.sendArg((byte)0, Descriptor.DTYPE_CSTRING, (byte)2, null, MdsMessage.EVENTCANREQUEST.getBytes());
            this.sendArg((byte)1, Descriptor.DTYPE_CSTRING, (byte)2, null, new byte[]{(byte)eventid});
        }catch(final IOException e){
            this.error = new String("Could not get IO for " + this.provider + e);
        }
    }

    public synchronized void MdsSetEvent(final UpdateEventListener l, final String event) {
        int eventid;
        if((eventid = this.AddEvent(l, event)) == -1) return;
        try{
            this.sendArg((byte)0, Descriptor.DTYPE_CSTRING, (byte)3, null, MdsMessage.EVENTASTREQUEST.getBytes());
            this.sendArg((byte)1, Descriptor.DTYPE_CSTRING, (byte)3, null, event.getBytes());
            this.sendArg((byte)2, Descriptor.DTYPE_UBYTE, (byte)3, null, new byte[]{(byte)(eventid)});
        }catch(final IOException e){
            this.error = new String("Could not get IO for " + this.provider + e);
        }
    }

    // Read either a string or a float array
    public synchronized Descriptor MdsValue(final String expr) {
        if(DEBUG.M){
            System.out.println("MdsConnection.MdsValue(\"" + expr + "\")");
        }
        try{
            final MdsMessage message = new MdsMessage(expr);
            this.pending_count++;
            message.Send(this.dos);
            return this.getAnswer();
        }catch(final IOException exc){
            return new Descriptor("Could not get IO for " + this.provider + exc);
        }
    }

    public Descriptor MdsValue(final String expr, final Vector<Descriptor> args) {
        return this.MdsValue(expr, args, true);
    }

    public synchronized Descriptor MdsValue(final String expr, Vector<Descriptor> args, final boolean wait) {
        if(DEBUG.M){
            System.out.println("MdsConnection.MdsValue(\"" + expr + "\", " + args + ", " + wait + ")");
        }
        if(args == null) args = new Vector<Descriptor>();
        final StringBuffer cmd = new StringBuffer(expr);
        final int n_args = args.size();
        byte idx = 0;
        final byte totalarg = (byte)(n_args + 1);
        Descriptor out;
        try{
            if(expr.indexOf("($") == -1) // If no $ args specified, build argument list
            {
                if(n_args > 0){
                    cmd.append("(");
                    for(int i = 0; i < n_args - 1; i++)
                        cmd.append("$,");
                    cmd.append("$)");
                }
            }
            this.sendArg(idx++, Descriptor.DTYPE_CSTRING, totalarg, null, cmd.toString().getBytes());
            Descriptor p;
            for(int i = 0; i < n_args; i++){
                p = args.elementAt(i);
                this.sendArg(idx++, p.dtype, totalarg, p.dims, p.dataToByteArray());
            }
            this.pending_count++;
            if(wait){
                out = this.getAnswer();
                if(out == null) out = new Descriptor("Could not get IO for " + this.provider);
            }else out = new Descriptor();
        }catch(final IOException e){
            out = new Descriptor("Could not get IO for " + this.provider + e);
        }
        return out;
    }

    public Descriptor MdsValueStraight(final String expr, final Vector<Descriptor> args) {
        return this.MdsValue(expr, args, false);
    }

    public void QuitFromMds() {
        try{
            if(this.connection_listener.size() > 0) this.connection_listener.removeAllElements();
            this.dos.close();
            this.dis.close();
            this.connected = false;
        }catch(final IOException e){
            this.error.concat("Could not get IO for " + this.provider + e);
        }
    }

    public synchronized void removeConnectionListener(final ConnectionListener l) {
        if(l == null) return;
        this.connection_listener.removeElement(l);
    }

    public synchronized int RemoveEvent(final UpdateEventListener l, final String eventName) {
        int eventid = -1;
        if(this.hashEventName.containsKey(eventName)){
            final EventItem eventItem = this.hashEventName.get(eventName);
            eventItem.listener.remove(l);
            if(eventItem.listener.isEmpty()){
                eventid = eventItem.eventid;
                this.event_flags[eventid] = false;
                this.hashEventName.remove(eventName);
                this.hashEventId.remove(new Integer(eventid));
            }
        }
        return eventid;
    }

    public void sendArg(final byte descr_idx, final byte dtype, final byte nargs, final int dims[], final byte body[]) throws IOException {
        final MdsMessage msg = new MdsMessage(descr_idx, dtype, nargs, dims, body);
        msg.Send(this.dos);
    }

    public void setProvider(final String provider) {
        if(this.connected) this.DisconnectFromMds();
        this.provider = provider;
        this.port = MdsConnection.DEFAULT_PORT;
        this.host = null;
    }

    public void setUser(final String user) {
        if(user == null || user.length() == 0) this.user = MdsConnection.DEFAULT_USER;
        else this.user = user;
    }
}