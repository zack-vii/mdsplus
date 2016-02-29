import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import jScope.Descriptor;
import mds.mdsMessage;

public class jServer extends MdsIp{
    // static inner class ActionDescriptor is used to keep action-related information
    static class ActionDescriptor{
        // NidData nid;
        InetAddress address;
        int         id;
        String      name;
        int         port;
        int         shot;
        String      tree;

        ActionDescriptor(final String name, final InetAddress address, final int port, final int id, final String tree, final int shot){
            this.name = name;
            this.address = address;
            this.port = port;
            this.id = id;
            this.tree = tree;
            this.shot = shot;
        }

        final InetAddress getAddress() {
            return this.address;
        }

        final int getId() {
            return this.id;
        }

        // final NidData getNid(){return nid;}
        final String getName() {
            return this.name;
        }

        final int getPort() {
            return this.port;
        }

        final int getShot() {
            return this.shot;
        }

        final String getTree() {
            return this.tree;
        }
    } // Inner class ActionQueue keeps a queue of ActionDesctor objects and manages synchronization
    class ActionQueue{
        Vector<ActionDescriptor> actionV = new Vector<ActionDescriptor>();

        void dequeueAllActions() {
            this.actionV.removeAllElements();
        }

        synchronized void enqueueAction(final ActionDescriptor actionDescr) {
            this.actionV.addElement(actionDescr);
            this.notify();
        }

        synchronized ActionDescriptor nextAction() {
            while(this.actionV.size() == 0){
                try{
                    this.wait();
                }catch(final InterruptedException exc){}
            }
            final ActionDescriptor retAction = this.actionV.elementAt(0);
            this.actionV.removeElementAt(0);
            return retAction;
        }

        synchronized boolean removeLast() {
            if(this.actionV.size() > 0){
                this.actionV.removeElementAt(this.actionV.size() - 1);
                return true;
            }
            return false;
        }
    }
    // Class WatchdogHandler handles watchdog (an integer is received and sent back)
    class WatchdogHandler extends Thread{
        int port;

        WatchdogHandler(final int port){
            this.port = port;
        }

        @Override
        public void run() {
            try{
                @SuppressWarnings("resource")
                final ServerSocket serverSock = new ServerSocket(this.port);
                while(true){
                    final Socket sock = serverSock.accept();
                    final DataInputStream dis = new DataInputStream(sock.getInputStream());
                    final DataOutputStream dos = new DataOutputStream(sock.getOutputStream());
                    try{
                        while(true){
                            final int read = dis.readInt();
                            dos.writeInt(read);
                            dos.flush();
                            // System.out.println("WATCHDOG");
                        }
                    }catch(final Exception exc){}
                }
            }catch(final Exception exc){
                System.err.println("Error accepting watchdog: " + exc);
            }
        }
    } // End class WatchdogHandler //Inner class Worker performs assigned computation on a separate thread.
      // It gets the nid to be executed from an instance of nidQueue, and executes
      // he action on a separate thread until either the timeout is reached (is specified)
      // or an abort command is received
    class Worker extends Thread{
        // Inner class ActionMaker executes the action and records whether such action has been aborted
        class ActionMaker extends Thread{
            boolean          aborted = false;
            ActionDescriptor action;

            ActionMaker(final ActionDescriptor action){
                this.action = action;
            }

            @Override
            public void run() {
                final int status = jServer.this.doSimpleAction(this.action.getName(), this.action.getTree(), this.action.getShot());
                if(!this.aborted){
                    Worker.this.retStatus = status;
                    Worker.this.awakeWorker();
                }
            }

            public void setAborted() {
                this.aborted = true;
            }
        } // End ActionMaker class implementation
        ActionDescriptor currAction;
        boolean          currentActionAborted = false;
        int              retStatus            = 0;

        public synchronized void abortCurrentAction() {
            this.currentActionAborted = true;
            this.notify();
        }

        public synchronized void awakeWorker() {
            this.notify();
        }

        @Override
        public void run() {
            while(true){
                this.currAction = jServer.this.actionQueue.nextAction();
                // NidData nid = currAction.getNid();
                final String message = "" + this.currAction.getId() + " " + jServer.SrvJobSTARTING + " 1 0";
                jServer.this.writeAnswer(this.currAction.getAddress(), this.currAction.getPort(), message);
                ActionMaker currMaker;
                synchronized(this){
                    this.currentActionAborted = false;
                    currMaker = new ActionMaker(this.currAction);
                    currMaker.start();
                    try{
                        this.wait(); // until either the action terminates or timeout expires or an abort is received
                    }catch(final InterruptedException exc){}
                }
                if(!this.currentActionAborted){
                    // String answer = "" + currAction.getId() + " " + SrvJobFINISHED + " 1 0";
                    final String answer = "" + this.currAction.getId() + " " + jServer.SrvJobFINISHED + " " + this.retStatus + " 0";
                    jServer.this.writeAnswer(this.currAction.getAddress(), this.currAction.getPort(), answer);
                }else{
                    currMaker.setAborted();
                    final String answer = "" + this.currAction.getId() + " " + jServer.SrvJobABORTED + " 1 0";
                    jServer.this.writeAnswer(this.currAction.getAddress(), this.currAction.getPort(), answer);
                }
            }
        }
    } // End Worker class impementation public static int doingNid;
    static final int ServerABORT         = 0xfe18032;
    /**** Used to start server ****/
    static final int SrvAbort            = 1;
    /**** Abort current action or mdsdcl command ***/
    static final int SrvAction           = 2;
    /**** Execute an action nid in a tree ***/
    static final int SrvClose            = 3;
    /**** Turn logging on/off ***/
    static final int SrvCommand          = 6;
    /**** Close open trees ***/
    static final int SrvCreatePulse      = 4;
    static final int SrvJobABORTED       = 1;
    static final int SrvJobAFTER_NOTIFY  = 2;
    /**** Watchdog port ***/
    static final int SrvJobBEFORE_NOTIFY = 1;
    static final int SrvJobFINISHED      = 3;
    static final int SrvJobSTARTING      = 2;
    /**** Execute MDSDCL command ***/
    static final int SrvMonitor          = 7;
    static final int SrvNoop             = 0;
    /**** Stop server ***/
    static final int SrvRemoveLast       = 10;
    /**** Create pulse files for single tree (nosubtrees) ***/
    static final int SrvSetLogging       = 5;
    /**** Broadcast messages to action monitors ***/
    static final int SrvShow             = 8;
    /**** Request current status of server ***/
    static final int SrvStop             = 9;
    /**** Remove last item in queue if action pending ***/
    static final int SrvWatchdogPort     = 11;

    public static void main(final String args[]) {
        int port;
        try{
            System.out.println(args[0]);
            port = Integer.parseInt(args[0]);
        }catch(final Exception exc){
            port = 8002;
        }
        /*
        try
        {
            PrintStream logFile = new PrintStream(new FileOutputStream("out_"+port+".log"));
            System.setOut(new TeeStream(System.out, logFile));
            System.setErr(new TeeStream(System.err, logFile));
        }
        catch (Exception exc) {}
        */
        if(args.length > 1){
            final String tclBatch = args[1];
            final Database tree = new Database();
            try{
                tree.evaluateData(Data.fromExpr("tcl(\'@" + tclBatch + "\')"), 0);
            }catch(final Exception exc){
                System.err.println("Error executing initial TCL batch: " + exc);
            }
        }
        final jServer server = new jServer(port);
        server.start();
        final BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        try{
            while(true){
                final String command = br.readLine();
                if(command.equals("exit")){
                    server.closeAll();
                    System.exit(0);
                }
            }
        }catch(final Exception exc){}
    }
    ActionQueue    actionQueue     = new ActionQueue();
    int            lastShot;
    String         lastTree        = null;
    ///// jServer class implementation
    Database       mdsTree         = null;
    Vector<Socket> retSocketsV     = new Vector<Socket>();
    boolean        watchdogStarted = false;
    Worker         worker          = new Worker();

    jServer(final int port){
        super(port);
        this.worker.start();
    }

    public void closeAll() {
        for(int i = 0; i < this.retSocketsV.size(); i++){
            final Socket currSock = this.retSocketsV.elementAt(i);
            if(currSock != null){
                try{
                    currSock.shutdownInput();
                    currSock.shutdownOutput();
                    currSock.close();
                }catch(final Exception exc){}
            }
        }
    }

    // Execute the action. Return the action status.
    int doSimpleAction(String name, final String tree, final int shot) {
        int status = 1;
        try{
            if(this.mdsTree == null || !tree.equals(this.lastTree) || shot != this.lastShot){
                if(this.mdsTree != null) this.mdsTree.close(0);
                this.mdsTree = new Database(tree, shot);
                this.mdsTree.open();
                this.lastTree = tree;
                this.lastShot = shot;
            }
            NidData nid;
            try{
                nid = this.mdsTree.resolve(new PathData(name), 0);
            }catch(final Exception exc){
                System.err.println("Cannot Find Node " + name + ": " + exc);
                return 0;
            }
            try{
                Data.evaluate("DevSetDoingNid(" + nid.getInt() + ")");
            }catch(final Exception exc){}
            try{
                name = this.mdsTree.getInfo(nid, 0).getFullPath();
            }catch(final Exception exc){
                System.err.println("Cannot resolve action name " + nid.getInt());
                name = "";
            }
            System.out.println("" + new Date() + ", Doing " + name + " in " + tree + " shot " + shot);
            // Gabriele jan 2014: let doAction return the true status and get error message
            // via JNI method GetmdsMessage(status)
            status = this.mdsTree.doAction(nid, 0);
            if((status & 1) == 0){
                final Data d = this.mdsTree.dataFromExpr("getLastError()");
                final String errMsg = this.mdsTree.evaluateSimpleData(d, 0).getString();
                // System.err.println("Action Execution failed: " + mdsTree.getmdsMessage(status));
                if(status != 0){
                    System.out.println("" + new Date() + ", Failed " + name + " in " + tree + " shot " + shot + ": " + this.mdsTree.getMdsMessage(status) + " " + (errMsg == null ? "" : errMsg));
                }else{
                    System.out.println("" + new Date() + ", Failed " + name + " in " + tree + " shot " + shot + ": " + (errMsg == null ? "Uknown error reason" : errMsg));
                }
            }else{
                System.out.println("" + new Date() + ", Done " + name + " in " + tree + " shot " + shot);
            }
            /*
            try {
                mdsTree.doAction(nid, 0);
            }catch(Exception exc) {
                System.err.println("Exception generated in Action execution: " + exc);
                try {
                    String msg = exc.toString();
                    StringTokenizer st = new StringTokenizer(msg, ":");
                    status = Integer.parseInt(st.nextToken());
                }catch(Exception exc1)
                {
                    status = 0;
                }
            }
            */
            // status = 1;
        }catch(final Exception exc){
            System.out.println("" + new Date() + ", Failed " + name + " in " + tree + " shot " + shot + ": " + exc);
            if(exc instanceof DatabaseException) status = ((DatabaseException)exc).getStatus();
            else status = 0;
        }
        return status;
    }

    synchronized Socket getRetSocket(final InetAddress ip, final int port) {
        for(int i = 0; i < this.retSocketsV.size(); i++){
            final Socket currSock = this.retSocketsV.elementAt(i);
            if(currSock.getInetAddress().equals(ip) && currSock.getPort() == port && !currSock.isInputShutdown()){ return currSock; }
        }
        try{
            final Socket newSock = new Socket(ip, port);
            this.retSocketsV.addElement(newSock);
            return newSock;
        }catch(final Exception exc){
            return null;
        }
    }

    public mdsMessage handleMessage(final mdsMessage[] messages) {
        // final int ris = -1;
        // final int status = 1;
        String command = "";
        // final String compositeCommand = "";
        try{
            command = messages[0].asString();
            // System.out.println("Command: " + command);
            if(command.toLowerCase().startsWith("kill")){
                final Timer timer = new Timer();
                timer.schedule(new TimerTask(){
                    @Override
                    public void run() {
                        System.exit(0);
                    }
                }, 500);
            }
            if(command.startsWith("ServerQAction")){
                final InetAddress address = InetAddress.getByAddress(messages[1].asByteArray());
                DataInputStream dis = new DataInputStream(new ByteArrayInputStream(messages[2].asByteArray()));
                final int port = dis.readShort();
                dis = new DataInputStream(new ByteArrayInputStream(messages[3].asByteArray()));
                final int operation = dis.readInt();
                dis = new DataInputStream(new ByteArrayInputStream(messages[4].asByteArray()));
                // final int flags = dis.readInt();
                dis = new DataInputStream(new ByteArrayInputStream(messages[5].asByteArray()));
                final int id = dis.readInt();
                switch(operation){
                    case SrvAction:
                        String tree = messages[6].asString();
                        dis = new DataInputStream(new ByteArrayInputStream(messages[7].asByteArray()));
                        int shot = dis.readInt();
                        // dis = new DataInputStream(new ByteArrayInputStream(messages[8].
                        // body));
                        // int nid = dis.readInt();
                        final String name = messages[8].asString();
                        // System.out.println("SrvAction " + id + " " + tree + " " + shot + " " + nid);
                        this.actionQueue.enqueueAction(new ActionDescriptor(name, address, port, id, tree, shot));
                        break;
                    case SrvAbort:
                        dis = new DataInputStream(new ByteArrayInputStream(messages[6].asByteArray()));
                        final int flushInt = dis.readInt();
                        final boolean flush = (flushInt != 0);
                        // System.out.println("SrvAbort " + id + " " + flush);
                        if(flush){
                            this.actionQueue.dequeueAllActions();
                        }
                        this.worker.abortCurrentAction();
                        // answer = "" + id + " " + SrvJobABORTED + " 1 0";
                        // writeAnswer(address, port, answer);
                        break;
                    case SrvClose:
                        // System.out.println("SrvClose " + id);
                        String answer = "" + id + " " + jServer.SrvJobFINISHED + " 1 0";
                        if(this.mdsTree != null){
                            try{
                                this.mdsTree.close(0);
                                this.mdsTree = null;
                            }catch(final Exception exc){
                                this.mdsTree = null;
                            }
                        }
                        this.writeAnswer(address, port, answer);
                        break;
                    case SrvCreatePulse:
                        tree = messages[6].asString();
                        dis = new DataInputStream(new ByteArrayInputStream(messages[10].asByteArray()));
                        shot = dis.readInt();
                        // System.out.println("SrvCreatePulse " + id + " " + tree + " " + shot);
                        answer = "" + id + " " + jServer.SrvJobFINISHED + " 1 0";
                        this.writeAnswer(address, port, answer);
                        break;
                    case SrvCommand:
                        // final String cli = messages[6].asString();
                        command = messages[7].asString();
                        // System.out.println("SrvCommand " + id + " " + cli + " " + command);
                        break;
                    case SrvSetLogging:
                        // System.out.println("SrvSetLogging " + id);
                        break;
                    case SrvStop:
                        // System.out.println("SrvStop " + id);
                        break;
                    case SrvWatchdogPort:
                        dis = new DataInputStream(new ByteArrayInputStream(messages[6].asByteArray()));
                        try{
                            final int watchdogPort = dis.readInt();
                            this.startWatchdog(watchdogPort);
                        }catch(final Exception exc){
                            System.err.println("Error getting watchdog port: " + exc);
                        }
                        this.removeAllRetSocket();
                        break;
                    case SrvRemoveLast:
                        if(this.actionQueue.removeLast()){
                            final mdsMessage msg = new mdsMessage((byte)0, Descriptor.DTYPE_LONG, (byte)0, (int[])null, new byte[]{(byte)0, (byte)0, (byte)0, (byte)1});
                            msg.verify();
                            return msg;
                        }else{
                            final mdsMessage msg = new mdsMessage((byte)0, Descriptor.DTYPE_LONG, (byte)0, (int[])null, new byte[]{(byte)0, (byte)0, (byte)0, (byte)0});
                            msg.verify();
                            return msg;
                        }
                    default:
                        System.out.println("Unknown Operation: " + operation);
                }
            }
        }catch(final Exception exc){
            System.err.println(exc);
        }
        final mdsMessage msg = new mdsMessage((byte)0, Descriptor.DTYPE_LONG, (byte)0, (int[])null, new byte[]{(byte)0, (byte)0, (byte)0, (byte)0});
        /* public mdsMessage(byte descr_idx, byte dtype, byte nargs, int dims[], byte body[])*/
        msg.verify();
        return msg;
    }

    synchronized void removeAllRetSocket() {
        this.retSocketsV.removeAllElements();
    }

    synchronized void removeRetSocket(final Socket sock) {
        this.retSocketsV.remove(sock);
    }

    void startWatchdog(final int watchdogPort) {
        if(this.watchdogStarted) return;
        (new WatchdogHandler(watchdogPort)).start();
        this.watchdogStarted = true;
    }

    synchronized Socket updateRetSocket(final InetAddress ip, final int port) {
        for(int i = 0; i < this.retSocketsV.size(); i++){
            final Socket currSock = this.retSocketsV.elementAt(i);
            if(currSock.getInetAddress().equals(ip) && currSock.getPort() == port && !currSock.isInputShutdown()){
                this.retSocketsV.remove(currSock);
                break;
            }
        }
        try{
            final Socket newSock = new Socket(ip, port);
            this.retSocketsV.addElement(newSock);
            return newSock;
        }catch(final Exception exc){
            System.out.println("Error creating socket for answers");
            return null;
        }
    }

    synchronized void writeAnswer(final InetAddress ip, final int port, final String answer) {
        // System.out.println("Answer: " + answer);
        try{
            DataOutputStream dos;
            dos = new DataOutputStream((this.getRetSocket(ip, port)).getOutputStream());
            final byte[] answerMsg = answer.getBytes();
            final byte[] retMsg = new byte[60];
            for(int i = 0; i < answerMsg.length; i++)
                retMsg[i] = answerMsg[i];
            for(int i = answerMsg.length; i < 60; i++)
                retMsg[i] = (byte)0;
            try{
                dos.write(retMsg);
                dos.flush();
            }catch(final Exception exc){
                // Try once to re-establish answer socket
                System.out.println("Connection to jDispatcher went down:" + exc);
                exc.printStackTrace();
                this.updateRetSocket(ip, port);
                dos = new DataOutputStream((this.getRetSocket(ip, port)).getOutputStream());
                dos.write(retMsg);
                dos.flush();
            }
        }catch(final Exception exc){
            System.err.println("Error sending answer: " + exc);
        }
    }
}
