import java.awt.Frame;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import jScope.ConnectionEvent;
import jScope.Descriptor;
import mds.mdsConnection;

class MdsServer extends mdsConnection{
    class ListenServerConnection extends Thread{
        @Override
        public void run() {
            ReceiveServerMessage rec_srv_msg;
            // do
            {
                try{
                    MdsServer.this.read_sock = MdsServer.this.rcv_sock.accept();
                    MdsServer.this.rcv_connected = true;
                    MdsServer.this.curr_listen_sock.add(MdsServer.this.read_sock);
                    System.out.println("Receive connection from server " + MdsServer.this.provider);
                    rec_srv_msg = new ReceiveServerMessage(MdsServer.this.read_sock);
                    rec_srv_msg.start();
                }catch(final IOException e){
                    MdsServer.this.rcv_connected = false;
                }
            }
            // while(rcv_connected);
        }
    }
    class ReceiveServerMessage extends Thread{
        boolean         connected = true;
        DataInputStream dis;
        Socket          s;

        public ReceiveServerMessage(final Socket s){
            this.s = s;
        }

        @Override
        public void run() {
            try{
                MdsServerEvent se = null;
                final byte header[] = new byte[60];
                final DataInputStream dis = new DataInputStream(new BufferedInputStream(this.s.getInputStream()));
                while(true){
                    dis.readFully(header);
                    final String head = new String(header);
                    // System.out.println("Ricevuto messaggio: " + head);
                    final StringTokenizer buf = new StringTokenizer(head, " \0");
                    int id = 0;
                    int flags = 0;
                    int status = 0;
                    try{
                        try{
                            id = Integer.decode(buf.nextToken()).intValue();
                        }catch(final Exception exc){
                            id = 0;
                        }
                        try{
                            flags = Integer.decode(buf.nextToken()).intValue();
                        }catch(final Exception exc){
                            flags = 0;
                        }
                        try{
                            status = Integer.decode(buf.nextToken()).intValue();
                        }catch(final Exception exc){
                            status = 0;
                        }
                        int msg_len = 0;
                        try{
                            msg_len = Integer.decode(buf.nextToken()).intValue();
                        }catch(final Exception exc){
                            msg_len = 0;
                        }
                        if(msg_len > 10000){
                            System.err.println("WRONG MESSAGE LENGTH msg_len: " + msg_len);
                            System.err.println("for message: " + head);
                            msg_len = 0;
                        }
                        if(msg_len > 0){
                            final byte msg[] = new byte[msg_len];
                            dis.readFully(msg);
                            se = new MdsMonitorEvent(this, id, flags, status, new String(msg));
                            // MdsMonitorEvent me = (MdsMonitorEvent)se;
                        }else{
                            se = new MdsServerEvent(this, id, flags, status);
                        }
                        MdsServer.this.dispatchMdsServerEvent(se);
                    }catch(final Exception e){
                        System.out.println("Bad Message " + head);
                        e.printStackTrace();
                        se = new MdsServerEvent(this, id, flags, status);
                        MdsServer.this.dispatchMdsServerEvent(se);
                    }
                }
            }catch(final IOException e){}
        }
    } // Class watchdogHandler handles watchdog management
    class WatchdogHandler extends Thread{
        DataInputStream  dis;
        DataOutputStream dos;
        Socket           watchdogSock;

        WatchdogHandler(final String host, final int port){
            try{
                this.watchdogSock = new Socket(host, port);
                this.dis = new DataInputStream(this.watchdogSock.getInputStream());
                this.dos = new DataOutputStream(this.watchdogSock.getOutputStream());
            }catch(final Exception exc){
                System.err.println("Error starting Watchdog: " + exc);
            }
        }

        @Override
        public void run() {
            Timer timer;
            try{
                while(true){
                    timer = new Timer();
                    timer.schedule(new TimerTask(){
                        @Override
                        public void run() {
                            System.out.println("Detected server TIMEOUT");
                            try{
                                MdsServer.this.sock.close();
                            }catch(final Exception exc){}
                            try{
                                WatchdogHandler.this.watchdogSock.close();
                            }catch(final Exception exc){}
                            try{
                                MdsServer.this.read_sock.close();
                            }catch(final Exception exc){}
                            try{
                                MdsServer.this.rcv_sock.close();
                            }catch(final Exception exc){}
                            final ConnectionEvent ce = new ConnectionEvent(this, ConnectionEvent.LOST_CONNECTION, "Lost connection from : " + MdsServer.this.provider);
                            MdsServer.this.dispatchConnectionEvent(ce);
                        }
                    }, MdsServer.WATCHDOG_TIMEOUT);
                    this.dos.writeInt(1);
                    this.dos.flush();
                    this.dis.readInt();
                    timer.cancel();
                    Thread.currentThread();
                    Thread.sleep(MdsServer.WATCHDOG_PERIOD);
                }
            }catch(final Exception exc){}
        }
    }// End class WatchdogHandler static final int ServerABORT = 0xfe18032; /**** Used to start server ****/
    static final int   SrvAbort            = 1;
    /**** Abort current action or mdsdcl command ***/
    static final int   SrvAction           = 2;
    /**** Execute an action nid in a tree ***/
    static final int   SrvClose            = 3;
    /**** Turn logging on/off ***/
    static final int   SrvCommand          = 6;
    /**** Close open trees ***/
    static final int   SrvCreatePulse      = 4;
    static final int   SrvJobAFTER_NOTIFY  = 2;
    /**** Watchdog port ***/
    static final int   SrvJobBEFORE_NOTIFY = 1;
    /**** Execute MDSDCL command ***/
    static final int   SrvMonitor          = 7;
    static final int   SrvNoop             = 0;
    /**** Stop server ***/
    static final int   SrvRemoveLast       = 10;
    /**** Create pulse files for single tree (nosubtrees) ***/
    static final int   SrvSetLogging       = 5;
    /**** Broadcast messages to action monitors ***/
    static final int   SrvShow             = 8;
    /**** Request current status of server ***/
    static final int   SrvStop             = 9;
    /**** Remove last item in queue if action pending ***/
    static final int   SrvWatchdogPort     = 11;
    static final short START_PORT          = 8800;
    static final int   WATCHDOG_PERIOD     = 5000;
    static final int   WATCHDOG_TIMEOUT    = 20000;

    static void main(final String arg[]) {
        MdsServer ms = null;
        try{
            ms = new MdsServer("150.178.3.47:8001", true, -1);
            final Descriptor reply = ms.monitorCheckin();
            final Frame f = new Frame();
            f.setVisible(true);
            /*
            do
            {
                reply = null;
                reply = ms.getAnswer();
                if(reply.error != null)
                {
                    System.out.println("Error "+new String(reply.error));
                    ms.shutdown();
                    reply = null;
                }
            } while(reply != null);
            */
        }catch(final IOException e){
            System.out.println("" + e);
        }
    }
    transient Vector<Socket>            curr_listen_sock      = new Vector<Socket>();
    protected boolean                   rcv_connected         = false;
    public short                        rcv_port;
    /**
     * Server socket on which server messages
     * are received
     */
    private ServerSocket                rcv_sock;
    /**
     * Server socket port
     */
    protected Socket                    read_sock             = null;
    byte[]                              self_address;
    transient Vector<MdsServerListener> server_event_listener = new Vector<MdsServerListener>();
    boolean                             useJavaServer         = true;
    int                                 watchdogPort          = -1;

    public MdsServer(final String server, final boolean useJavaServer, final int watchdogPort) throws IOException{
        super(server);
        this.useJavaServer = useJavaServer;
        this.watchdogPort = watchdogPort;
        this.self_address = InetAddress.getLocalHost().getAddress();
        if(!this.ConnectToMds(false)) throw(new IOException(this.error));
        this.startReceiver();
    }

    public Descriptor abort(final boolean do_flush) throws IOException {
        final int flush[] = new int[1];
        flush[0] = (do_flush) ? 1 : 0;
        final Vector<Descriptor> args = new Vector<Descriptor>();
        args.add(new Descriptor(null, flush));
        final Descriptor reply = this.sendMessage(0, MdsServer.SrvAbort, args, true);
        return reply;
    }

    public synchronized void addMdsServerListener(final MdsServerListener l) {
        if(l == null){ return; }
        this.server_event_listener.addElement(l);
    }

    public Descriptor closeTrees() throws IOException {
        final Descriptor reply = this.sendMessage(0, MdsServer.SrvClose, null, true);
        return reply;
    }

    // Connection to mdsip server: if it is a Java action server the watchdog port is sent just after the connection
    @Override
    public synchronized boolean ConnectToMds(final boolean use_compression) {
        final boolean status = super.ConnectToMds(use_compression);
        if(this.watchdogPort > 0 && status) // Connection succeeded and watch dog defined
        {
            try{
                this.setWatchdogPort(this.watchdogPort);
            }catch(final Exception exc){
                System.err.println("Error setting watchdog port: " + exc);
            }
            this.startWatchdog(this.host, this.watchdogPort);
        }
        return status;
    }

    private void createPort(final short start_port) throws IOException {
        boolean found = false;
        short tries = 0;
        while(!found){
            for(tries = 0; this.rcv_sock == null && tries < 500; tries++){
                try{
                    this.rcv_sock = new ServerSocket((this.rcv_port = (short)(start_port + tries)));
                }catch(final IOException e){
                    this.rcv_sock = null;
                }
            }
            if(tries == 500) throw(new IOException("Can't create receive port"));
            found = true;
        }
    }

    public Descriptor createPulse(final String tree, final int shot) throws IOException {
        final Vector<Descriptor> args = new Vector<Descriptor>();
        args.add(new Descriptor(null, tree));
        // args.add(new Descriptor(null, new int[]{rcv_port}));
        args.add(new Descriptor(null, new int[]{shot}));
        final Descriptor reply = this.sendMessage(0, MdsServer.SrvCreatePulse, args, true);
        return reply;
    }

    public Descriptor dispatchAction(final String tree, final int shot, final String name, final int id) throws IOException {
        final Vector<Descriptor> args = new Vector<Descriptor>();
        args.add(new Descriptor(null, tree));
        args.add(new Descriptor(null, new int[]{shot}));
        if(this.useJavaServer) args.add(new Descriptor(null, name));
        else args.add(new Descriptor(null, new int[]{id}));
        final Descriptor reply = this.sendMessage(id, MdsServer.SrvAction, true, args, true);
        return reply;
    }

    public Descriptor dispatchCommand(final String cli, final String command) throws IOException {
        final Vector<Descriptor> args = new Vector<Descriptor>();
        args.add(new Descriptor(null, cli));
        args.add(new Descriptor(null, command));
        final Descriptor reply = this.sendMessage(0, MdsServer.SrvCommand, args, true);
        return reply;
    }

    public Descriptor dispatchDirectCommand(final String command) throws IOException {
        final Descriptor reply = this.mdsValue(command);
        return reply;
    }

    protected void dispatchMdsServerEvent(final MdsServerEvent e) {
        // synchronized(server_event_listener)
        {
            for(int i = 0; i < this.server_event_listener.size(); i++){
                final MdsServerListener curr_server = this.server_event_listener.elementAt(i);
                curr_server.processMdsServerEvent(e);
            }
        }
    }

    @Override
    protected void finalize() {
        this.shutdown();
    }

    public String getFullPath(final String tree, final int shot, final int nid) {
        final Vector<Descriptor> args = new Vector<Descriptor>();
        args.add(new Descriptor(null, tree));
        args.add(new Descriptor(null, new int[]{shot}));
        args.add(new Descriptor(null, new int[]{nid}));
        final Descriptor out = MdsServer.this.mdsValue("JavaGetFullPath", args);
        if(out.error != null) return "<Path evaluation error>";
        else return out.strdata;
    }

    public Descriptor monitorCheckin() throws IOException {
        final String cmd = "";
        final Vector<Descriptor> args = new Vector<Descriptor>();
        args.add(new Descriptor(null, new int[]{0}));
        args.add(new Descriptor(null, new int[]{0}));
        args.add(new Descriptor(null, new int[]{0}));
        args.add(new Descriptor(null, new int[]{0}));
        args.add(new Descriptor(null, new int[]{MdsMonitorEvent.MonitorCheckin}));
        args.add(new Descriptor(null, cmd));
        args.add(new Descriptor(null, new int[]{0}));
        final Descriptor reply = this.sendMessage(0, MdsServer.SrvMonitor, args, true);
        return reply;
    }

    public Descriptor removeLast() throws IOException {
        final Descriptor reply = this.sendMessage(0, MdsServer.SrvRemoveLast, null, true);
        return reply;
    }

    public synchronized void removeMdsServerListener(final MdsServerListener l) {
        if(l == null){ return; }
        this.server_event_listener.removeElement(l);
    }

    public Descriptor sendMessage(final int id, final int op, final boolean before_notify, Vector<Descriptor> args, final boolean wait) throws IOException {
        final String cmd = new String("ServerQAction");
        // int flags = before_notify?SrvJobBEFORE_NOTIFY:0;
        final int flags = before_notify ? MdsServer.SrvJobBEFORE_NOTIFY : MdsServer.SrvJobAFTER_NOTIFY;
        // System.out.println("PARTE SEND MESSAGE");
        if(args == null) args = new Vector<Descriptor>();
        /*
        args.add(0, new Descriptor(Descriptor.DTYPE_LONG,    null, Descriptor.dataToByteArray(new Integer(id))));
        args.add(0, new Descriptor(Descriptor.DTYPE_LONG,    null, Descriptor.dataToByteArray(new Integer(flags))));
        args.add(0, new Descriptor(Descriptor.DTYPE_LONG,    null, Descriptor.dataToByteArray(new Integer(op))));
        args.add(0, new Descriptor(Descriptor.DTYPE_SHORT,   null, Descriptor.dataToByteArray(new Short(rcv_port))));
        args.add(0, new Descriptor(Descriptor.DTYPE_LONG,    null, self_address));
        */
        args.add(0, new Descriptor(null, new int[]{id}));
        args.add(0, new Descriptor(null, new int[]{flags}));
        args.add(0, new Descriptor(null, new int[]{op}));
        args.add(0, new Descriptor(null, new short[]{this.rcv_port}));
        args.add(0, new Descriptor(null, this.self_address));
        Descriptor out;
        if(wait) out = this.mdsValue(cmd, args);
        else out = this.mdsValueStraight(cmd, args);
        // System.out.println("FINISCE SEND MESSAGE");
        if(out.error != null) throw(new IOException(out.error));
        return out;
    }

    public Descriptor sendMessage(final int id, final int op, final Vector<Descriptor> args, final boolean wait) throws IOException {
        return this.sendMessage(id, op, false, args, wait);
    }

    public Descriptor setLogging(final byte logging_mode) throws IOException {
        final byte data[] = new byte[1];
        data[0] = logging_mode;
        final Vector<Descriptor> args = new Vector<Descriptor>();
        /*
        args.add(new Descriptor(Descriptor.DTYPE_CHAR,  null, data));
        */
        args.add(new Descriptor(null, data));
        final Descriptor reply = this.sendMessage(0, MdsServer.SrvSetLogging, args, true);
        return reply;
    }

    public Descriptor setWatchdogPort(final int port) throws IOException {
        final Vector<Descriptor> args = new Vector<Descriptor>();
        args.add(new Descriptor(null, new int[]{port}));
        final Descriptor reply = this.sendMessage(0, MdsServer.SrvWatchdogPort, true, args, true);
        return reply;
    }

    public void shutdown() {
        if(this.server_event_listener != null && this.server_event_listener.size() != 0){
            this.server_event_listener.removeAllElements();
        }
        try{
            if(this.rcv_sock != null) this.rcv_sock.close();
            if(this.read_sock != null) this.read_sock.close();
            if(this.curr_listen_sock.size() != 0){
                for(int i = 0; i < this.curr_listen_sock.size(); i++)
                    this.curr_listen_sock.elementAt(i).close();
                this.curr_listen_sock.removeAllElements();
            }
        }catch(final Exception exc){}
        QuitFromMds();
    }

    private void startReceiver() throws IOException {
        if(this.rcv_port == 0) this.createPort(MdsServer.START_PORT);
        final ListenServerConnection listen_server_con = new ListenServerConnection();
        listen_server_con.start();
    }

    void startWatchdog(final String host, final int port) {
        (new WatchdogHandler(host, port)).start();
    }

    public Descriptor stop() throws IOException {
        final Descriptor reply = this.sendMessage(0, MdsServer.SrvStop, null, true);
        return reply;
    }
}
