import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;
import mds.mdsMessage;

class jDispatcherIp extends MdsIp{
    static Vector<Server> servers = new Vector<Server>();

    public static void main(final String args[]) {
        String treeName;
        if(args.length >= 1) treeName = args[0];
        else treeName = "RFX";
        final Properties properties = MdsHelper.initialization(treeName);
        if(properties == null) System.exit(0);
        final Balancer balancer = new Balancer();
        final jDispatcher dispatcher = new jDispatcher(balancer);
        dispatcher.addServer(new InfoServer());
        int port = 0;
        try{
            port = Integer.parseInt(properties.getProperty("jDispatcher.port"));
        }catch(final Exception exc){
            System.out.println("Cannot read port");
            System.exit(0);
        }
        int info_port = 0;
        try{
            info_port = Integer.parseInt(properties.getProperty("jDispatcher.info_port"));
            dispatcher.startInfoServer(info_port);
        }catch(final Exception exc){
            System.out.println("Cannot read info_port");
            System.exit(0);
        }
        System.out.println("Start dispatcher on port " + port);
        final jDispatcherIp dispatcherIp = new jDispatcherIp(port, dispatcher, treeName);
        dispatcherIp.start();
        int i = 1;
        while(true){
            final String server_class = properties.getProperty("jDispatcher.server_" + i + ".class");
            if(server_class == null) break;
            final String server_ip = properties.getProperty("jDispatcher.server_" + i + ".address");
            if(server_ip == null) break;
            final String server_subtree = properties.getProperty("jDispatcher.server_" + i + ".subtree");
            boolean useJavaServer;
            try{
                useJavaServer = properties.getProperty("jDispatcher.server_" + i + ".use_jserver").equals("true");
            }catch(final Exception exc){
                useJavaServer = true;
            }
            int watchdogPort;
            try{
                watchdogPort = Integer.parseInt(properties.getProperty("jDispatcher.server_" + i + ".watchdog_port"));
            }catch(final Exception exc){
                watchdogPort = -1;
            }
            final Server server = new ActionServer("", server_ip.trim(), server_class.trim(), server_subtree, useJavaServer, watchdogPort);
            jDispatcherIp.servers.addElement(server);
            dispatcher.addServer(server);
            i++;
        }
        i = 1;
        while(true){
            final String monitor_port = properties.getProperty("jDispatcher.monitor_" + i + ".port");
            if(monitor_port == null) break;
            try{
                final int monitor_port_int = Integer.parseInt(monitor_port);
                System.out.println("Start monitor server on port " + monitor_port_int);
                final MdsMonitor monitor = new MdsMonitor(monitor_port_int);
                dispatcher.addMonitorListener(monitor);
                monitor.start();
                i++;
            }catch(final Exception exc){
                break;
            }
        }
        /*
        String actionsMonitorPort = properties.getProperty("jDispatcher.actions_monitor_port");
        if (actionsMonitorPort != null)
        {
        try {
            int actionsMonitorPortVal = Integer.parseInt(actionsMonitorPort);
            MdsActionsMonitor actionsMonitor = new MdsActionsMonitor(actionsMonitorPortVal);
            dispatcher.addMonitorListener(actionsMonitor);
            actionsMonitor.start();
            }
            catch (Exception exc) {}
            System.out.println("Start done actions monitor on port : " + actionsMonitorPort);
        }
        */
        final String default_server = properties.getProperty("jDispatcher.default_server_idx");
        try{
            final int default_server_idx = Integer.parseInt(default_server) - 1;
            final Server server = jDispatcherIp.servers.elementAt(default_server_idx);
            dispatcher.setDefaultServer(server);
        }catch(final Exception exc){}
        /*
             jDispatcherIp dispatcherIp = new jDispatcherIp(port, dispatcher, treeName);
                dispatcherIp.start();
         */
        i = 1;
        while(true){
            final String phaseName = properties.getProperty("jDispatcher.phase_" + i + ".name");
            if(phaseName == null) break;
            final Vector<Integer> currSynchNumbers = new Vector<Integer>();
            final String currSynchStr = properties.getProperty("jDispatcher.phase_" + i + ".synch_seq_numbers");
            if(currSynchStr != null){
                final StringTokenizer st = new StringTokenizer(currSynchStr, " ,");
                while(st.hasMoreTokens()){
                    try{
                        currSynchNumbers.addElement(new Integer(st.nextToken()));
                    }catch(final Exception exc){
                        System.out.println("WARNING: Invalid Syncronization number for phase " + phaseName);
                    }
                }
                dispatcher.addSynchNumbers(phaseName, currSynchNumbers);
            }
            i++;
        }
        try{
            dispatcherIp.getListenThread().join();
        }catch(final Exception exc){}
    }
    String      currTreeName;
    jDispatcher dispatcher;
    int         shot;
    Database    tree;
    String      treeName;

    public jDispatcherIp(final int port, final jDispatcher dispatcher, final String treeName){
        super(port);
        this.dispatcher = dispatcher;
        this.treeName = treeName;
        System.out.println("Tree name per jDispatcherIp : " + treeName);
        this.tree = new Database(treeName, -1);
    }

    protected int doCommand(final String command) {
        System.out.println("Command: " + command);
        final StringTokenizer st = new StringTokenizer(command, "/ ");
        try{
            final String first_part = st.nextToken();
            if(first_part.equals("DISPATCH")){
                final String second_part = st.nextToken();
                if(second_part.equals("BUILD")){
                    if(!st.hasMoreTokens()) this.dispatcher.collectDispatchInformation();
                    else{
                        final String buildRoot = st.nextToken();
                        this.dispatcher.collectDispatchInformation(buildRoot);
                    }
                }else if(second_part.equals("PHASE")){
                    final String third_part = st.nextToken();
                    this.dispatcher.startPhase(third_part);
                    this.dispatcher.waitPhase();
                }else{
                    int nid;
                    try{
                        nid = Integer.parseInt(second_part);
                    }catch(final Exception ex){
                        throw new Exception("Invalid command");
                    }
                    if(!this.dispatcher.dispatchAction(nid)) throw new Exception("Cannot dispatch action");
                }
            }else if(first_part.equals("CREATE")){
                final String second_part = st.nextToken();
                if(second_part.equals("PULSE")){
                    this.shot = Integer.parseInt(st.nextToken());
                    if(this.shot == 0) this.shot = this.getCurrentShot();
                    this.dispatcher.beginSequence(this.shot);
                }else throw new Exception("Invalid Command");
            }else if(first_part.equals("SET")){
                final String second_part = st.nextToken();
                if(second_part.equals("TREE")){
                    if(st.hasMoreTokens()){
                        this.currTreeName = st.nextToken();
                    }
                    try{
                        if(st.hasMoreTokens()){
                            this.shot = Integer.parseInt(st.nextToken());
                            // setCurrentShot(shot);
                            this.dispatcher.setTree(this.currTreeName, this.shot);
                        }else{
                            this.dispatcher.setTree(this.currTreeName);
                        }
                    }catch(final Exception exc){
                        System.err.println("Wrong shot number in SET TREE command\n" + exc);
                    }
                }else if(second_part.equals("CURRENT")){
                    final String third_part = st.nextToken();
                    this.setCurrentShot(Integer.parseInt(third_part));
                }else throw new Exception("Invalid Command");
            }else if(first_part.equals("CLOSE")){
                this.dispatcher.endSequence(this.shot);
            }else if(first_part.equals("ABORT")){
                final String second_part = st.nextToken();
                int nid;
                try{
                    if(second_part.toUpperCase().equals("PHASE")) this.dispatcher.abortPhase();
                    else{
                        nid = Integer.parseInt(second_part);
                        this.dispatcher.abortAction(nid);
                    }
                }catch(final Exception ex){
                    throw new Exception("Invalid command");
                }
            }else if(first_part.equals("REDISPATCH")){
                final String second_part = st.nextToken();
                String phaseName = null;
                try{
                    phaseName = st.nextToken();
                }catch(final Exception exc){
                    phaseName = null;
                }
                int nid;
                try{
                    nid = Integer.parseInt(second_part);
                }catch(final Exception ex){
                    throw new Exception("Invalid command");
                }
                if(phaseName != null) this.dispatcher.redispatchAction(nid, phaseName);
                else this.dispatcher.redispatchAction(nid);
            }else if(first_part.equals("GET")){
                final String second_part = st.nextToken();
                if(second_part.equals("CURRENT")){
                    System.out.println("Current shot :" + this.getCurrentShot());
                    return this.getCurrentShot();
                }else throw new Exception("Invalid Command");
            }else if(first_part.equals("INCREMENT")){
                final String second_part = st.nextToken();
                if(second_part.equals("CURRENT")){
                    this.incrementCurrentShot();
                }else throw new Exception("Invalid Command");
            }else if(first_part.equals("CHECK")){
                if(this.dispatcher.checkEssential()) return 1;
                else return 0;
            }else if(first_part.equals("DO")){
                try{
                    final String second_part = st.nextToken();
                    this.dispatcher.dispatchAction(second_part);
                }catch(final Exception exc){
                    throw new Exception("Invalid Command");
                }
            }else throw new Exception("Invalid Command");
            return -1;
        }catch(final Exception exc){
            System.out.println("Invalid command: " + command);
            return -1;
        }
    }

    int getCurrentShot() {
        try{
            return this.tree.getCurrentShot(this.treeName);
        }catch(final Exception exc){
            return -1;
        }
    }

    public mdsMessage handleMessage(final mdsMessage[] messages) {
        int ris = -1;
        String command = "", compositeCommand = "";
        compositeCommand = messages[0].asString();
        // System.err.println("Command at jDispatcherIp " + compositeCommand);
        // Handle direct commands: ABORT and DISPATCH
        if(compositeCommand.toUpperCase().startsWith("ABORT") || compositeCommand.toUpperCase().startsWith("DISPATCH") || compositeCommand.toUpperCase().startsWith("REDISPATCH")) command = compositeCommand;
        else{
            try{
                final StringTokenizer st = new StringTokenizer(compositeCommand, "\"");
                while(!(st.nextToken().equals("TCL")));
                st.nextToken();
                command = st.nextToken();
            }catch(final Exception exc){
                System.err.println("Unexpected message has been received by jDispatcherIp:" + compositeCommand + " " + exc);
            }
        }
        try{
            ris = this.doCommand(command.toUpperCase());
        }catch(final Exception exc){
            return new mdsMessage(exc.getMessage(), null);
        }
        if(ris < 0){ // i.e. if any command except get current
            final mdsMessage msg = new mdsMessage((byte)1);
            msg.verify();
            return msg;
        }else{
            final byte[] buf = new byte[4];
            buf[0] = (byte)((ris >> 24) & 0xFF);
            buf[1] = (byte)((ris >> 16) & 0xFF);
            buf[2] = (byte)((ris >> 8) & 0xFF);
            buf[3] = (byte)(ris & 0xFF);
            final mdsMessage msg = new mdsMessage((byte)0, Data.DTYPE_L, (byte)0, new int[]{1}, buf);
            msg.verify();
            return msg;
        }
    }

    void incrementCurrentShot() {
        try{
            final int shot = this.tree.getCurrentShot(this.treeName);
            this.tree.setCurrentShot(shot + 1);
        }catch(final Exception exc){
            System.err.println("Error incrementing current shot");
        }
    }

    void setCurrentShot(final int shot) {
        try{
            this.tree.setCurrentShot(this.currTreeName, shot);
        }catch(final Exception exc){}
    }

    public void setDispatcher(final jDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }
}
