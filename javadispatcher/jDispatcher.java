import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

class jDispatcher implements ServerListener{
    class jDispatcherInfo extends Thread{
        ServerSocket serverSock;

        jDispatcherInfo(final int port){
            try{
                this.serverSock = new ServerSocket(port);
            }catch(final Exception exc){
                System.err.println("Error starting jDispatcher info server: " + exc);
                this.serverSock = null;
            }
        }

        @Override
        public void run() {
            try{
                while(true){
                    final Socket sock = this.serverSock.accept();
                    (new jDispatcherInfoHandler(sock)).start();
                }
            }catch(final Exception exc){}
        }
    }
    // Inner classes jDispatcherInfo and jDispatcherInfoHandler are used to handle
    // request for information
    class jDispatcherInfoHandler extends Thread{
        DataInputStream  dis;
        DataOutputStream dos;

        jDispatcherInfoHandler(final Socket sock){
            try{
                this.dis = new DataInputStream(sock.getInputStream());
                this.dos = new DataOutputStream(sock.getOutputStream());
            }catch(final Exception exc){
                this.dis = null;
                this.dos = null;
            }
        }

        @Override
        public void run() {
            try{
                while(true){
                    final String command = this.dis.readUTF();
                    if(command.toLowerCase().equals("servers")){
                        this.dos.writeInt(jDispatcher.this.servers.size());
                        for(int i = 0; i < jDispatcher.this.servers.size(); i++){
                            final Server currServer = jDispatcher.this.servers.elementAt(i);
                            final String serverClass = currServer.getServerClass();
                            String address = "";
                            if(currServer instanceof ActionServer) address = ((ActionServer)currServer).getAddress();
                            final boolean isActive = currServer.isActive();
                            this.dos.writeUTF(serverClass);
                            this.dos.writeUTF(address);
                            this.dos.writeBoolean(isActive);
                            // dos.writeInt(currServer.getDoingAction());
                            this.dos.flush();
                        }
                    }
                }
            }catch(final Exception exc){}
        }
    }
    /**
     * Indexed by nid. For each nid, a vector of potentially dependent actions is defined.
     * Note that the hierarchy of ActionData does not override equals and hashCode, and therefore
     * two actions are equal only if they refer to the same instance.
     * This kind of dependency is NOT restricted to the current phase.
     */
    static class PhaseDescriptor
    /**
     * PhaseDescriptor carries all the data structures required by each phase
     */
    {
        /**
         * List of conditional actions with no dependency. These actions will be started immediately
         * at the beginning of the phase
         */
        Hashtable<Integer, Action>                            all_actions       = new Hashtable<Integer, Action>();
        // Indexed by server class, every element of totSeqActions is in turn an Hashtable indexed by sequence
        // number, associating a vector of actions (i.e. the actions for the given sequence number and
        // for the given server class in this phase
        // OLD Hashtable seq_actions = new Hashtable();
        /**
         * Indexed by sequence number. For each sequence number, a vector of actions is defined.
         */
        Hashtable<ActionData, Vector<Action>>                 dependencies      = new Hashtable<ActionData, Vector<Action>>();
        /**
         * Indexed by action data (not actions!). For each action, a vector of potentially dependent actions is defined.
         * Note that the hierarchy of ActionData does not override equals and hashCode, and therefore
         * two actions are equal only if ther refer to the same instance.
         */
        Vector<Action>                                        immediate_actions = new Vector<Action>();
        String                                                phase_name;
        // Indexed by server class, every element of totSeqNumbers contains the sequence number for this
        // phase and the given server class
        // OLD Vector seq_numbers = new Vector();
        /**
         * Active sequence numbers, in ascending order
         */
        Hashtable<String, Hashtable<Integer, Vector<Action>>> totSeqActions     = new Hashtable<String, Hashtable<Integer, Vector<Action>>>();
        Hashtable<String, Vector<Integer>>                    totSeqNumbers     = new Hashtable<String, Vector<Integer>>();

        /**
         * A complete list of actions for that phase, indexed by their nid
         */
        PhaseDescriptor(final String phase_name){
            this.phase_name = phase_name;
        }
    }
    static final int MONITOR_BEGIN_SEQUENCE = 1;
    static final int MONITOR_BUILD          = 3;
    static final int MONITOR_BUILD_BEGIN    = 2;
    static final int MONITOR_BUILD_END      = 4;
    static final int MONITOR_CHECKIN        = 5;
    static final int MONITOR_DISPATCHED     = 6;
    static final int MONITOR_DOING          = 7;
    static final int MONITOR_DONE           = 8;
    static final int MONITOR_END_PHASE      = 10;
    static final int MONITOR_END_SEQUENCE   = 11;
    static final int MONITOR_START_PHASE    = 9;
    static final int TdiUNKNOWN_VAR         = 0xfd380f2;
    static boolean   verbose                = true;

    public static void main(final String args[]) {
        final Server server;
        final Balancer balancer = new Balancer();
        final jDispatcher dispatcher = new jDispatcher(balancer);
        dispatcher.addServer(new InfoServer("disp_test"));
        dispatcher.addServer(server = new ActionServer("disp_test", "10.44.4.11:8000", "mecklenburg-monitor"));
        balancer.setDefaultServer(server);
        dispatcher.collectDispatchInformation();
        dispatcher.setTree("W7X", 0);
        dispatcher.startInfoServer(8010);
        final Enumeration<ActionData> acts = dispatcher.actions.keys();
        while(acts.hasMoreElements())
            System.out.println(acts.nextElement());
        // dispatcher.beginSequence(2);
        // dispatcher.startPhase("INITIALIZATION");
        // dispatcher.waitPhase();
        // dispatcher.endSequence(2);
        System.exit(0);
    }
    /**
     * Indexed by ActionData, used to retrieve actions from ActionData in method isEnabled
     */
    Hashtable<Integer, Action>              action_nids       = new Hashtable<Integer, Action>();
    /**
     * Indexed by phase name. Associates dispatch data structures with each phase.
     */
    Hashtable<ActionData, Action>           actions           = new Hashtable<ActionData, Action>();
    // Indexed by server class, every element of totSeqNumber contains the enumeration
    // of the sequence numbers for this phase and this server class
    Hashtable<String, Integer>              actSeqNumbers     = new Hashtable<String, Integer>();
    /**
     * server list, used for collecting dispatch information. Servers will receive work by the Balancer
     */
    protected Balancer                      balancer;
    protected PhaseDescriptor               curr_phase;                                                       // selects the current phase data structures
    /**
     * synchSeqNumbers contains the synchronization sequenc numbers defined in jDispatcher.properties.
     */
    String                                  defaultServerName = "";
    // OLD Vector seq_dispatched = new Vector();
    /**
     * Vector of currently dispatched sequential actions
     */
    Vector<Action>                          dep_dispatched    = new Vector<Action>();
    boolean                                 doing_phase       = false;
    /**
     * All actions will be dispatched to balancer
     */
    protected Vector<MonitorListener>       monitors          = new Vector<MonitorListener>();
    // OLD Enumeration curr_seq_numbers = null;
    /**
     * Contains the sequence numbers of the current phase
     */
    Hashtable<Integer, Vector<Action>>      nidDependencies   = new Hashtable<Integer, Vector<Action>>();
    Hashtable<String, PhaseDescriptor>      phases            = new Hashtable<String, PhaseDescriptor>();
    // Indexed by server class. Keeps track of the current sequence number;
    Hashtable<String, Boolean>              phaseTerminated   = new Hashtable<String, Boolean>();
    // Indexed by server class. Keeps track of whether the curent phase has terminated for this server class
    /**
     * Indexed by nid, used to retrieve actions to be manually dispatched
     */
    protected Vector<Server>                servers           = new Vector<Server>();
    /**
     * currently open tree
     */
    int                                     shot;
    Hashtable<String, Vector<Integer>>      synchSeqNumberH   = new Hashtable<String, Vector<Integer>>();
    // Synch number sequence for every phase name
    Vector<Integer>                         synchSeqNumbers   = new Vector<Integer>();
    /**
     * current shot;
     */
    int                                     timestamp         = 1;
    /**
     * timestamp used for messages. Incremented each new seqeuence. Messages with older timestamp
     * are discarded.
     */
    Hashtable<String, Vector<Action>>       totSeqDispatched  = new Hashtable<String, Vector<Action>>();
    // Indexed by server class, every element of totSeqDispatched is the vector of currently dispatched
    // action for a given server class
    /**
     * Vector of currently dispatched dependent actions
     */
    Hashtable<String, Enumeration<Integer>> totSeqNumbers     = new Hashtable<String, Enumeration<Integer>>();
    String                                  tree;

    public jDispatcher(){}

    /**
     * Registered Monitor listeners.
     */
    public jDispatcher(final Balancer balancer){
        this.balancer = balancer;
    }

    public /* OCT 2008 synchronized */ void abortAction(final int nid) {
        if(this.action_nids == null) return;
        final Action action = this.action_nids.get(new Integer(nid));
        if(action == null) return;
        this.balancer.abortAction(action);
    }

    public synchronized void abortPhase() {
        Action action;
        this.balancer.abort();
        final Enumeration serverClasses = this.totSeqDispatched.keys();
        while(serverClasses.hasMoreElements()){
            final String serverClass = (String)serverClasses.nextElement();
            final Vector<Action> seqDispatched = this.totSeqDispatched.get(serverClass);
            while(!seqDispatched.isEmpty()){
                action = seqDispatched.elementAt(0);
                action.setStatus(Action.ABORTED, 0, jDispatcher.verbose);
                seqDispatched.removeElementAt(0);
            }
        }
        while(!this.dep_dispatched.isEmpty()){
            action = this.dep_dispatched.elementAt(0);
            action.setStatus(Action.ABORTED, 0, jDispatcher.verbose);
            // seq_dispatched.removeElementAt(0);
        }
    }

    @Override
    public /*synchronized*/ void actionAborted(final ServerEvent event)
    /**
     * called by a server to notify that the action is starting being executed.
     * Simply reports the fact
     */
    {
        // if(event.getTimestamp() != timestamp) //outdated message
        // return;
        event.getAction().setStatus(Action.ABORTED, 0, jDispatcher.verbose);
        this.fireMonitorEvent(event.getAction(), jDispatcher.MONITOR_DONE);
        this.reportDone(event.getAction());
    }

    @Override
    public synchronized void actionFinished(final ServerEvent event)
    /**
     * called by a server to notify that the action has finished
     */
    {
        // if(event.getTimestamp() != timestamp) //outdated message
        // return;
        // update status in report
        final Action action = event.getAction();
        try{
            final String mdsevent = ((StringData)(((DispatchData)(action.getAction().getDispatch())).getCompletion())).getString();
            if(mdsevent != null && !mdsevent.equals("\"\"")){
                MdsHelper.generateEvent(mdsevent, 0);
            }
        }catch(final Exception exc){}
        action.setStatus(Action.DONE, event.getStatus(), jDispatcher.verbose);
        this.fireMonitorEvent(action, jDispatcher.MONITOR_DONE);
        this.reportDone(action);
    }

    @Override
    public synchronized void actionStarting(final ServerEvent event)
    /**
     * called by a server to notify that the action is starting being executed.
     * Simply reports the fact
     */
    {
        // if(event.getTimestamp() != timestamp) //outdated message
        // return;
        event.getAction().setStatus(Action.DOING, 0, jDispatcher.verbose);
        this.fireMonitorEvent(event.getAction(), jDispatcher.MONITOR_DOING);
    }

    public synchronized void addMonitorListener(final MonitorListener monitor) {
        this.monitors.addElement(monitor);
    }

    public synchronized void addServer(final Server server) {
        this.servers.addElement(server);
        server.addServerListener(this);
        this.balancer.addServer(server);
    }

    public synchronized void addSynchNumbers(final String phase, final Vector<Integer> synchNumbers) {
        this.synchSeqNumberH.put(phase, synchNumbers);
    }

    boolean allSeqDispatchedAreEmpty() {
        final Enumeration serverClasses = this.totSeqDispatched.keys();
        while(serverClasses.hasMoreElements()){
            final String serverClass = (String)serverClasses.nextElement();
            if(!this.totSeqDispatched.get(serverClass).isEmpty()) ;
            return false;
        }
        return true;
    }

    boolean allTerminatedInPhase() {
        final Enumeration serverClasses = this.phaseTerminated.keys();
        while(serverClasses.hasMoreElements()){
            final String serverClass = (String)serverClasses.nextElement();
            if(!(this.phaseTerminated.get(serverClass)).booleanValue()) return false;
        }
        return true;
    }

    public synchronized void beginSequence(final int shot) {
        this.shot = shot;
        final Enumeration server_list = this.servers.elements();
        while(server_list.hasMoreElements())
            ((Server)server_list.nextElement()).beginSequence(shot);
        this.fireMonitorEvent((Action)null, jDispatcher.MONITOR_BEGIN_SEQUENCE);
    }

    protected void buildDependencies() {
        DispatchData dispatch;
        final Enumeration phaseNames = this.phases.keys();
        while(phaseNames.hasMoreElements()){
            final PhaseDescriptor currPhase = this.phases.get(phaseNames.nextElement());
            if(currPhase == null) continue;
            final Enumeration action_list = currPhase.dependencies.keys();
            while(action_list.hasMoreElements()){
                final ActionData action_data = (ActionData)action_list.nextElement();
                try{
                    dispatch = (DispatchData)action_data.getDispatch();
                }catch(final Exception e){
                    continue;
                }
                if(dispatch.getType() == DispatchData.SCHED_COND) this.traverseDispatch(action_data, dispatch.getWhen(), currPhase);
            }
        }
    }

    // Check which servers can proceede to the next sequence number
    // Either None, or this server or a list of server classes
    Vector<String> canProceede(final String serverClass) {
        // Find the most advanced sequence number
        final Enumeration<Integer> seqNumbers = this.actSeqNumbers.elements();
        int maxSeq = -1;
        while(seqNumbers.hasMoreElements()){
            final int currSeq = seqNumbers.nextElement();
            if(currSeq > maxSeq) maxSeq = currSeq;
        }
        // Find smallest synch number which is greater or equan to maxSeq
        int actSynch = -1;
        int nextSynch = -1;
        for(int idx = 0; idx < this.synchSeqNumbers.size(); idx++){
            final int currSynch = this.synchSeqNumbers.elementAt(idx);
            if(currSynch >= maxSeq){
                actSynch = currSynch;
                if(idx == this.synchSeqNumbers.size() - 1) nextSynch = actSynch;
                else nextSynch = this.synchSeqNumbers.elementAt(idx + 1);
                break;
            }
        }
        if(actSynch == -1) // No more synch numbers, proceede freely
        {
            final Vector<String> retVect = new Vector<String>();
            retVect.addElement(serverClass);
            return retVect;
        }
        // If the next sequence number is less than or equal to actSynch. it can proceede.
        final Integer thisSeq = this.actSeqNumbers.get(serverClass);
        final Vector<Integer> currSeqNumbers = this.curr_phase.totSeqNumbers.get(serverClass);
        final int thisIdx = currSeqNumbers.indexOf(thisSeq);
        // if it is the last element can trivially proceede (it does nothing)
        final int thisSeqN = thisSeq.intValue();
        if(thisIdx == currSeqNumbers.size() - 1 && thisSeqN != actSynch){
            final Vector<String> retVect = new Vector<String>();
            retVect.addElement(serverClass);
            return retVect;
        }
        if(thisIdx < currSeqNumbers.size() - 1){
            if(currSeqNumbers.elementAt(thisIdx + 1) <= actSynch){
                final Vector<String> retVect = new Vector<String>();
                retVect.addElement(serverClass);
                return retVect;
            }
        }
        // Otherwise we must check that all the servers have reached a condition where they have
        // either finished or the next sequence number is larger that actSynch
        // In any case wait until all dispatched actions for any server
        if(!this.allSeqDispatchedAreEmpty()) return new Vector<String>();
        final Enumeration<String> serverClasses = this.curr_phase.totSeqNumbers.keys();
        final Vector<String> serverClassesV = new Vector<String>();
        while(serverClasses.hasMoreElements()){
            final String currServerClass = serverClasses.nextElement();
            final Integer currSeqNum = this.actSeqNumbers.get(currServerClass);
            final Vector<Integer> currSeqVect = this.curr_phase.totSeqNumbers.get(currServerClass);
            final int currSeqN = currSeqNum.intValue();
            if(currSeqN == -1) // This server has not yet started
            {
                final int firstSeqN = currSeqVect.elementAt(0);
                if((nextSynch == actSynch && firstSeqN > actSynch) || // If the lase synch number
                (nextSynch > actSynch && firstSeqN <= nextSynch)) // or before the next
                serverClassesV.addElement(currServerClass);
                // Will start only if the first sequence number is greater than the curent synch step
            }else{
                final int currIdx = currSeqVect.indexOf(currSeqNum);
                // if it is the last element can trivially proceede (it does nothing)
                serverClassesV.addElement(currServerClass);
                if(currIdx < currSeqVect.size() - 1) // It is the last element of the sequence, skip it
                {
                    if(currSeqVect.elementAt(currIdx + 1) >= actSynch) return new Vector<String>(); // Empty array
                    // There is at least one server class which has not yet
                    // reached the synchronization number
                }
            }
        }
        // Return the array of all server names
        return serverClassesV; // If I arrive here, all the servers are ready to pass to the next synch step
    }

    public boolean checkEssential() {
        final Enumeration<Action> actionsEn = this.curr_phase.all_actions.elements();
        while(actionsEn.hasMoreElements()){
            final Action action = actionsEn.nextElement();
            if(action.isEssential() && (action.getDispatchStatus() != Action.DONE || ((action.getStatus() & 1) == 0))) return false;
        }
        return true;
    }

    public synchronized void clearTables() {
        this.actions.clear();
        this.action_nids.clear();
        this.phases.clear();
        this.dep_dispatched.removeAllElements();
        this.totSeqDispatched = new Hashtable<String, Vector<Action>>();
        this.actSeqNumbers = new Hashtable<String, Integer>();
        // seq_dispatched.removeAllElements();
        this.nidDependencies.clear();
        this.timestamp++;
    }

    public synchronized void collectDispatchInformation()
    /**
     * request actions to each server and insert them into hashtables
     */
    {
        this.clearTables();
        // fireMonitorEvent(null, MONITOR_BUILD_BEGIN);
        final Enumeration server_list = this.servers.elements();
        while(server_list.hasMoreElements()){
            final Server curr_server = (Server)server_list.nextElement();
            final Action[] curr_actions = curr_server.collectActions();
            if(curr_actions != null){
                for(int i = 0; i < curr_actions.length; i++)
                    this.insertAction(curr_actions[i], i == 0, i == curr_actions.length - 1);
            }
        }
        this.buildDependencies();
        // fireMonitorEvent(null, MONITOR_BUILD_END);
    }

    public synchronized void collectDispatchInformation(final String rootPath)
    /**
     * request actions to each server and insert them into hashtables
     */
    {
        this.clearTables();
        // fireMonitorEvent(null, MONITOR_BUILD_BEGIN);
        final Enumeration server_list = this.servers.elements();
        while(server_list.hasMoreElements()){
            final Server curr_server = (Server)server_list.nextElement();
            final Action[] curr_actions = curr_server.collectActions(rootPath);
            if(curr_actions != null){
                for(int i = 0; i < curr_actions.length; i++)
                    this.insertAction(curr_actions[i], i == 0, i == curr_actions.length - 1);
            }
        }
        this.buildDependencies();
        // fireMonitorEvent(null, MONITOR_BUILD_END);
    }

    @Override
    public void connected(final ServerEvent event) {
        System.out.println(" ----- RECONNECTED -------  " + event.getMessage());
        event.getAction();
        this.fireMonitorEvent(event.getMessage(), MonitorEvent.CONNECT_EVENT);
    }

    void discardAction(final Action action) {
        action.setStatus(Action.ABORTED, 0, jDispatcher.verbose);
        this.reportDone(action);
    }

    @Override
    public void disconnected(final ServerEvent event) {
        System.out.println(" ----- CRASH -------  ");
        this.fireMonitorEvent(event.getMessage(), MonitorEvent.DISCONNECT_EVENT);
    }

    public synchronized boolean dispatchAction(final int nid) {
        if(this.action_nids == null) return false;
        final Action action = this.action_nids.get(new Integer(nid));
        if(action == null) return false;
        action.setStatus(Action.DISPATCHED, 0, jDispatcher.verbose);
        action.setManual(true);
        this.fireMonitorEvent(action, jDispatcher.MONITOR_DISPATCHED);
        if(!this.balancer.enqueueAction(action)) this.discardAction(action);
        return true;
    }

    public void dispatchAction(final String actionPath) {
        final Database currTree = InfoServer.getDatabase();
        NidData nid;
        try{
            nid = currTree.resolve(new PathData(actionPath), 0);
        }catch(final Exception exc){
            System.err.println("Cannot resolve " + actionPath);
            return;
        }
        this.dispatchAction(nid.getInt());
    }

    public synchronized void endSequence(final int shot) {
        final Enumeration server_list = this.servers.elements();
        while(server_list.hasMoreElements())
            ((Server)server_list.nextElement()).endSequence(shot);
        this.clearTables();
        this.fireMonitorEvent((Action)null, jDispatcher.MONITOR_END_SEQUENCE);
    }

    protected void fireMonitorEvent(final Action action, final int mode) {
        final String server;
        System.out.println("----------- fireMonitorEvent SHOT " + this.shot);
        final MonitorEvent event = new MonitorEvent(this, this.tree, this.shot, (this.curr_phase == null) ? "NONE" : this.curr_phase.phase_name, action);
        final Enumeration monitor_list = this.monitors.elements();
        while(monitor_list.hasMoreElements()){
            final MonitorListener curr_listener = (MonitorListener)monitor_list.nextElement();
            switch(mode){
                case MONITOR_BEGIN_SEQUENCE:
                    curr_listener.beginSequence(event);
                    break;
                case MONITOR_BUILD_BEGIN:
                    curr_listener.buildBegin(event);
                    break;
                case MONITOR_BUILD:
                    curr_listener.build(event);
                    break;
                case MONITOR_BUILD_END:
                    curr_listener.buildEnd(event);
                    break;
                case MONITOR_DISPATCHED:
                    curr_listener.dispatched(event);
                    break;
                case MONITOR_DOING:
                    curr_listener.doing(event);
                    break;
                case MONITOR_DONE:
                    curr_listener.done(event);
                    break;
                case MONITOR_END_SEQUENCE:
                    curr_listener.endSequence(event);
                    break;
                case MONITOR_START_PHASE:
                    event.eventId = MonitorEvent.START_PHASE_EVENT;
                    curr_listener.startPhase(event);
                    break;
                case MONITOR_END_PHASE:
                    event.eventId = MonitorEvent.END_PHASE_EVENT;
                    curr_listener.endPhase(event);
                    break;
            }
        }
    }

    protected void fireMonitorEvent(final String message, final int mode) {
        final MonitorEvent event = new MonitorEvent(this, mode, message);
        final Enumeration monitor_list = this.monitors.elements();
        while(monitor_list.hasMoreElements()){
            final MonitorListener curr_listener = (MonitorListener)monitor_list.nextElement();
            switch(mode){
                case MonitorEvent.CONNECT_EVENT:
                    curr_listener.connect(event);
                    break;
                case MonitorEvent.DISCONNECT_EVENT:
                    curr_listener.disconnect(event);
                    break;
                case MonitorEvent.START_PHASE_EVENT:
                    curr_listener.startPhase(event);
                    break;
                case MonitorEvent.END_PHASE_EVENT:
                    curr_listener.endPhase(event);
                    break;
            }
        }
    }

    int getFirstValidSynch() {
        // return the first synch number greater than or equal to any sequence number
        for(int idx = 0; idx < this.synchSeqNumbers.size(); idx++){
            final int currSynch = this.synchSeqNumbers.elementAt(idx);
            final Enumeration serverClasses = this.curr_phase.totSeqNumbers.keys();
            while(serverClasses.hasMoreElements()){
                final String currServerClass = (String)serverClasses.nextElement();
                final Vector<Integer> currSeqVect = this.curr_phase.totSeqNumbers.get(currServerClass);
                if(currSeqVect.size() > 0){
                    if(currSeqVect.elementAt(0) <= currSynch) return currSynch;
                }
            }
        }
        return -1;
    }

    protected String getServerClass(final Action action) {
        try{
            final DispatchData dispatch = (DispatchData)action.getAction().getDispatch();
            final String serverClass = dispatch.getIdent().getString().toUpperCase();
            if(serverClass == null || serverClass.equals("")) return this.defaultServerName;
            return this.balancer.getActServer(serverClass);
        }catch(final Exception exc){
            return this.defaultServerName;
        }
    }

    Vector<Integer> getValidSynchSeq(final String phaseName, final Hashtable<String, Vector<Integer>> currTotSeqNumbers) {
        final Vector<Integer> currSynchSeqNumbers = this.synchSeqNumberH.get(phaseName);
        if(currSynchSeqNumbers == null) return new Vector<Integer>();
        final Vector<Integer> actSynchSeqNumbers = new Vector<Integer>();
        // Get minimum ans maximum sequence number for all servers
        final Enumeration<String> serverNames = currTotSeqNumbers.keys();
        int minSeq = 0x7fffffff;
        int maxSeq = -1;
        while(serverNames.hasMoreElements()){
            final String currServerName = serverNames.nextElement();
            final Vector<Integer> currSeqNumbers = currTotSeqNumbers.get(currServerName);
            if(currSeqNumbers.size() > 0){
                final int currMin = currSeqNumbers.elementAt(0);
                final int currMax = currSeqNumbers.elementAt(currSeqNumbers.size() - 1);
                if(minSeq > currMin) minSeq = currMin;
                if(maxSeq < currMax) maxSeq = currMax;
            }
        }
        if(maxSeq == -1) return new Vector<Integer>();// No sequential actions in this phase
        for(int i = 0; i < currSynchSeqNumbers.size(); i++){
            final int currSynch = currSynchSeqNumbers.elementAt(i);
            if(currSynch >= minSeq && currSynch < maxSeq) actSynchSeqNumbers.addElement(new Integer(currSynch));
        }
        return actSynchSeqNumbers;
    }

    protected void insertAction(final Action action, final boolean is_first, final boolean is_last) {
        final String serverClass = this.getServerClass(action);
        // record current timestamp
        action.setTimestamp(this.timestamp);
        // Insert action in actions hashtable
        this.actions.put(action.getAction(), action);
        // Insert action in action_nids hashtable
        this.action_nids.put(new Integer(action.getNid()), action);
        // Check if the Action is sequential
        final DispatchData dispatch = (DispatchData)action.getAction().getDispatch();
        if(dispatch == null){
            System.out.println("Warning: Action " + action + " without dispatch info");
            return;
        }else{
            String phase_name, ident;
            try{
                phase_name = dispatch.getPhase().getString().toUpperCase();
            }catch(final Exception exc){
                System.out.println("Warning: Action " + action + " does not contain a phase string");
                return;
            }
            try{
                ident = dispatch.getIdent().getString();
            }catch(final Exception exc){
                System.out.println("Warning: Action " + action + " does not containg a server class string");
                return;
            }
            this.curr_phase = this.phases.get(phase_name);
            if(this.curr_phase == null){
                this.curr_phase = new PhaseDescriptor(phase_name);
                this.phases.put(phase_name, this.curr_phase);
            }
            this.curr_phase.all_actions.put(new Integer(action.getNid()), action);
            boolean isSequenceNumber = true;
            if(dispatch.getType() == DispatchData.SCHED_SEQ){
                int seq_number = 0;
                if(dispatch.getWhen() instanceof NidData) isSequenceNumber = false;
                else{
                    try{
                        seq_number = dispatch.getWhen().getInt();
                    }catch(final Exception exc){
                        isSequenceNumber = false;
                        // System.out.println("Warning: expression used for sequence number");
                    }
                }
                if(!isSequenceNumber) dispatch.descs[2] = this.traverseSeqExpression(action.getAction(), dispatch.getWhen());
                Hashtable<Integer, Vector<Action>> seqActions = this.curr_phase.totSeqActions.get(serverClass);
                if(seqActions == null) this.curr_phase.totSeqActions.put(serverClass, seqActions = new Hashtable<Integer, Vector<Action>>());
                Vector<Integer> seqNumbers = this.curr_phase.totSeqNumbers.get(serverClass);
                if(seqNumbers == null) this.curr_phase.totSeqNumbers.put(serverClass, seqNumbers = new Vector<Integer>());
                if(isSequenceNumber){
                    final Integer seq_obj = new Integer(seq_number);
                    if(seqActions.containsKey(seq_obj)) seqActions.get(seq_obj).addElement(action);
                    else{
                        // it is the first time such a sequence number is referenced
                        final Vector<Action> curr_vector = new Vector<Action>();
                        curr_vector.add(action);
                        seqActions.put(seq_obj, curr_vector);
                        /*//////////////////////////////////////////////////DA AGGIUNGERE ORDINATO!!!!!


                        int size = seqNumbers.size();
                        if (seq_number >= size) {
                            for (int i = size; i < seq_number; i++)
                                seqNumbers.addElement(new Integer(-1));
                            seqNumbers.addElement(seq_obj);
                        }
                        else {
                            if (seqNumbers.elementAt(seq_number) == -1) {
                                seqNumbers.removeElementAt(seq_number);
                                seqNumbers.insertElementAt(seq_obj, seq_number);
                            }
                        }
                        
                        ///////////////////////////////////////////////////////////////////////////////////*/
                        if(seqNumbers.size() == 0) seqNumbers.addElement(seq_obj);
                        else{
                            int idx, currNum = -1;
                            for(idx = 0; idx < seqNumbers.size(); idx++){
                                currNum = seqNumbers.elementAt(idx);
                                if(currNum >= seq_number) break;
                            }
                            if(currNum != seq_number) seqNumbers.insertElementAt(seq_obj, idx);
                        }
                    }
                }
            }
            ////////////////////////// GAB CHRISTMAS 2004
            // Handle Conditional actions with no dependencies
            // these will be dispatched asynchronously at the beginning of the phase
            if(dispatch.getType() == DispatchData.SCHED_COND && dispatch.getWhen() == null){
                this.curr_phase.immediate_actions.addElement(action);
            }
            ////////////////////////// GAB CHRISTMAS 2440
            this.curr_phase.dependencies.put(action.getAction(), new Vector<Action>()); // Insert every new action in dependencies hashtable
            if(is_first) this.fireMonitorEvent(action, jDispatcher.MONITOR_BUILD_BEGIN);
            else if(is_last) this.fireMonitorEvent(action, jDispatcher.MONITOR_BUILD_END);
            else this.fireMonitorEvent(action, jDispatcher.MONITOR_BUILD);
        }
    }

    boolean isConditional(final Action action) {
        final DispatchData dispatch = (DispatchData)action.getAction().getDispatch();
        if(dispatch.getType() != DispatchData.SCHED_SEQ) return true;
        if((dispatch.getWhen() instanceof NidData) || (dispatch.getWhen() instanceof PathData) || (dispatch.getWhen() instanceof IdentData)) return true;
        return false;
    }

    protected boolean isEnabled(final Data when)
    /**
     * Check whether this action is enabled to execute, based on the current status hold
     * in curr_status.reports hashtable.
     * In this class the check is done in Java. In a derived class it may be performed by evaluating
     * a TDI expression.
     */
    {
        if(when instanceof ConditionData){
            final int modifier = ((ConditionData)when).getModifier();
            final Action action = this.actions.get(((ConditionData)when).getArgument());
            if(action == null) // Action not present, maybe not enabled
            return false;
            final int dispatch_status = action.getDispatchStatus();
            final int status = action.getStatus();
            if(dispatch_status != Action.DONE) return false;
            switch(modifier){
                case 0:
                    if((status & 1) != 0) return true;
                    return false;
                case ConditionData.IGNORE_UNDEFINED: // ???
                case ConditionData.IGNORE_STATUS:
                    return true;
                case ConditionData.NEGATE_CONDITION:
                    if((status & 1) == 0) return true;
                    return false;
            }
        }
        if(when instanceof ActionData){
            final Action action = this.actions.get(when);
            if(action.getDispatchStatus() != Action.DONE) return false;
        }
        if(when instanceof DependencyData){
            final Data args[] = ((DependencyData)when).getArguments();
            if(args.length != 2){
                System.out.println("Error: dependency needs 2 arguments. Ignored");
                return false;
            }
            final int opcode = ((DependencyData)when).getOpcode();
            switch(opcode){
                case DependencyData.DEPENDENCY_AND:
                    return this.isEnabled(args[0]) && this.isEnabled(args[1]);
                case DependencyData.DEPENDENCY_OR:
                    return this.isEnabled(args[0]) || this.isEnabled(args[1]);
            }
        }
        return true;
    }

    public void redispatchAction(final int nid) {
        if(this.doing_phase) // Redispatch not allowed during sequence
        return;
        final Action action = this.curr_phase.all_actions.get(new Integer(nid));
        if(action == null){
            System.err.println("Redispatched a non existent action");
            return;
        }
        this.dep_dispatched.addElement(action);
        action.setStatus(Action.DISPATCHED, 0, jDispatcher.verbose);
        action.setManual(true);
        this.fireMonitorEvent(action, jDispatcher.MONITOR_DISPATCHED);
        if(!this.balancer.enqueueAction(action)) this.discardAction(action);
    }

    public void redispatchAction(final int nid, final String phaseName) {
        final PhaseDescriptor phase = this.phases.get(phaseName);
        if(this.doing_phase) // Redispatch not allowed during sequence
        return;
        final Action action = phase.all_actions.get(new Integer(nid));
        if(action == null){
            System.err.println("Redispatched a non existent action");
            return;
        }
        this.dep_dispatched.addElement(action);
        action.setStatus(Action.DISPATCHED, 0, jDispatcher.verbose);
        action.setManual(true);
        this.fireMonitorEvent(action, jDispatcher.MONITOR_DISPATCHED);
        if(!this.balancer.enqueueAction(action)) this.discardAction(action);
    }

    protected void reportDone(final Action action) {
        // remove action from dispatched
        final String serverClass = this.getServerClass(action);
        Vector<Action> currSeqDispatched = this.totSeqDispatched.get(serverClass);
        if((currSeqDispatched == null) || !currSeqDispatched.removeElement(action)) this.dep_dispatched.removeElement(action); // The action belongs only to one of the two
        if(!action.isManual()){
            // check dependent actions
            final Vector<Action> currDepV = this.curr_phase.dependencies.get(action.getAction());
            if(currDepV != null && currDepV.size() > 0){
                final Enumeration<Action> depend_actions = currDepV.elements();
                while(depend_actions.hasMoreElements()){
                    final Action curr_action = depend_actions.nextElement();
                    if(curr_action.isOn() && this.isEnabled(((DispatchData)curr_action.getAction().getDispatch()).getWhen())){ // the dependent action is now enabled
                        this.dep_dispatched.addElement(curr_action);
                        curr_action.setStatus(Action.DISPATCHED, 0, jDispatcher.verbose);
                        this.fireMonitorEvent(curr_action, jDispatcher.MONITOR_DISPATCHED);
                        if(!this.balancer.enqueueAction(curr_action)) this.discardAction(action);
                    }
                }
            }
            // Handle now possible dependencies based on sequence expression
            final Vector<Action> depVect = this.nidDependencies.get(new Integer(action.getNid()));
            if(depVect != null && depVect.size() > 0){
                final Database tree = InfoServer.getDatabase();
                final String doneExpr = "PUBLIC _ACTION_" + Integer.toHexString(action.getNid()) + " = " + action.getStatus();
                try{
                    tree.evaluateData(Data.fromExpr(doneExpr), 0);
                }catch(final Exception exc){
                    System.err.println("Error setting completion TDI variable: " + exc);
                }
                for(int i = 0; i < depVect.size(); i++){
                    final Action currAction = depVect.elementAt(i);
                    try{
                        final Data retData = tree.evaluateData(((DispatchData)currAction.getAction().getDispatch()).getWhen(), 0);
                        final int retStatus = retData.getInt();
                        if((retStatus & 0x00000001) != 0){ // Condition satisfied
                            this.dep_dispatched.addElement(currAction);
                            currAction.setStatus(Action.DISPATCHED, 0, jDispatcher.verbose);
                            this.fireMonitorEvent(currAction, jDispatcher.MONITOR_DISPATCHED);
                            if(!this.balancer.enqueueAction(currAction)) this.discardAction(action);
                        }else{ // The action is removed from dep_dispatched since it has not to be executed
                            action.setStatus(Action.ABORTED, Action.ServerNOT_DISPATCHED, jDispatcher.verbose);
                            this.fireMonitorEvent(action, jDispatcher.MONITOR_DONE);
                        }
                    }catch(final Exception exc){
                        /*     if (exc instanceof DatabaseException && ((DatabaseException)exc).getStatus() == TdiUNKNOWN_VAR) {
                                 currAction.setStatus(Action.ABORTED,
                         Action.ServerINVALID_DEPENDENCY,
                                                  verbose);
                                 fireMonitorEvent(currAction, MONITOR_DONE);
                          }*/
                    }
                }
            }
        }
        if(!this.isConditional(action)){
            if(currSeqDispatched.isEmpty()){ // No more sequential actions for this sequence number for all server classes
                                             // Get the list of servers which can advance their sequence number
                final Vector<String> serverClassesV = this.canProceede(serverClass);
                for(int i = 0; i < serverClassesV.size(); i++){
                    final String currServerClass = serverClassesV.elementAt(i);
                    final Enumeration<Integer> currSeqNumbers = this.totSeqNumbers.get(currServerClass);
                    currSeqDispatched = this.totSeqDispatched.get(currServerClass);
                    if(currSeqNumbers.hasMoreElements()){ // Still further sequence numbers
                        final Integer curr_int = currSeqNumbers.nextElement();
                        this.actSeqNumbers.put(currServerClass, curr_int);
                        final Enumeration<Action> actions = this.curr_phase.totSeqActions.get(currServerClass).get(curr_int).elements();
                        while(actions.hasMoreElements()){
                            final Action curr_action = actions.nextElement();
                            currSeqDispatched.addElement(curr_action);
                            curr_action.setStatus(Action.DISPATCHED, 0, jDispatcher.verbose); // Spostata da cesare
                            this.fireMonitorEvent(curr_action, jDispatcher.MONITOR_DISPATCHED);
                            if(!this.balancer.enqueueAction(curr_action)) this.discardAction(action);
                            // curr_action.setStatus(Action.DISPATCHED, 0, verbose);
                        }
                    }else{
                        this.phaseTerminated.put(currServerClass, new Boolean(true));
                        if(this.allTerminatedInPhase()){
                            if(this.dep_dispatched.isEmpty()){ // No more actions at all for this phase
                                this.doing_phase = false;
                                // Report those (dependent) actions which have not been dispatched
                                final Enumeration<Action> allActionsEn = this.curr_phase.all_actions.elements();
                                while(allActionsEn.hasMoreElements()){
                                    final Action currAction = allActionsEn.nextElement();
                                    final int currDispatchStatus = currAction.getDispatchStatus();
                                    if(currDispatchStatus != Action.ABORTED && currDispatchStatus != Action.DONE){
                                        currAction.setStatus(Action.ABORTED, Action.ServerCANT_HAPPEN, jDispatcher.verbose);
                                        // ???? Cesare fireMonitorEvent(action, MONITOR_DONE);
                                        this.fireMonitorEvent(currAction, jDispatcher.MONITOR_DONE);
                                    }
                                }
                                System.out.println("------------- FINE DELLA FASE --------------------- ");
                                // fireMonitorEvent(this.curr_phase.phase_name , MonitorEvent.END_PHASE_EVENT);
                                this.fireMonitorEvent((Action)null, jDispatcher.MONITOR_END_PHASE);
                                synchronized(this){
                                    this.notify();
                                }
                                return;
                            }
                        }
                    }
                }
            }
        }else // End of conditional action
        {
            if(this.allTerminatedInPhase()){
                if(this.dep_dispatched.isEmpty()){ // No more actions at all for this phase
                    this.doing_phase = false;
                    // Report those (dependent) actions which have not been dispatched
                    final Enumeration<Action> allActionsEn = this.curr_phase.all_actions.elements();
                    while(allActionsEn.hasMoreElements()){
                        final Action currAction = allActionsEn.nextElement();
                        final int currDispatchStatus = currAction.getDispatchStatus();
                        if(currDispatchStatus != Action.ABORTED && currDispatchStatus != Action.DONE){
                            currAction.setStatus(Action.ABORTED, Action.ServerCANT_HAPPEN, jDispatcher.verbose);
                            // ???? Cesare fireMonitorEvent(action, MONITOR_DONE);
                            this.fireMonitorEvent(currAction, jDispatcher.MONITOR_DONE);
                        }
                    }
                    System.out.println("------------- END PHASE --------------------- ");
                    // fireMonitorEvent(this.curr_phase.phase_name , MonitorEvent.END_PHASE_EVENT);
                    this.fireMonitorEvent((Action)null, jDispatcher.MONITOR_END_PHASE);
                    synchronized(this){
                        this.notify();
                    }
                    return;
                }
            }
        }
    }

    public void setDefaultServer(final Server server) {
        this.balancer.setDefaultServer(server);
    }

    public void setDefaultServerName(final String serverName) {
        this.defaultServerName = serverName;
    }

    public void setTree(final String tree) {
        this.tree = tree;
        final Enumeration server_list = this.servers.elements();
        while(server_list.hasMoreElements())
            ((Server)server_list.nextElement()).setTree(tree);
    }

    public void setTree(final String tree, final int shot) {
        final Database mdsTree = new Database(tree, shot);
        try{
            mdsTree.open();
        }catch(final Exception exc){
            System.err.println("Cannot open tree " + tree + " " + shot);
        }
        this.tree = tree;
        this.shot = shot;
        final Enumeration server_list = this.servers.elements();
        while(server_list.hasMoreElements())
            ((Server)server_list.nextElement()).setTree(tree, shot);
    }

    public void startInfoServer(final int port) {
        System.out.println("Start info server on port " + port);
        (new jDispatcherInfo(port)).start();
    }

    public synchronized boolean startPhase(final String phase_name) {
        this.doing_phase = false;
        // increment timestamp. Incoming messages with older timestamp will be ignored
        this.curr_phase = this.phases.get(phase_name); // select data structures for the current phase
        if(this.curr_phase == null){
            this.curr_phase = new PhaseDescriptor(phase_name);
            // return false; //Phase name does not correspond to any known phase.
        }
        this.synchSeqNumbers = this.getValidSynchSeq(phase_name, this.curr_phase.totSeqNumbers);
        System.out.println("------------- BEGIN PHASE ---------------------  ");
        System.out.print("SYNCHRONOUS SEQUENCE NUMBERS: ");
        for(int i = 0; i < this.synchSeqNumbers.size(); i++)
            System.out.print(" " + this.synchSeqNumbers.elementAt(i));
        System.out.println("");
        this.fireMonitorEvent((Action)null, jDispatcher.MONITOR_START_PHASE);
        // GAB CHRISTMAS 2004
        // dispatch immediate actions, if any
        if(this.curr_phase.immediate_actions.size() > 0){
            for(int i = 0; i < this.curr_phase.immediate_actions.size(); i++){
                final Action action = this.curr_phase.immediate_actions.elementAt(i);
                if(action.isOn()){
                    this.doing_phase = true;
                    this.dep_dispatched.addElement(action);
                    action.setStatus(Action.DISPATCHED, 0, jDispatcher.verbose);
                    this.fireMonitorEvent(action, jDispatcher.MONITOR_DISPATCHED);
                    if(!this.balancer.enqueueAction(action)) this.discardAction(action);
                }
            }
        }
        //////////////////////
        final Enumeration serverClasses = this.curr_phase.totSeqNumbers.keys();
        // For every server class
        this.phaseTerminated = new Hashtable<String, Boolean>();
        boolean anyDispatched = false;
        final int firstSynch = this.getFirstValidSynch();
        while(serverClasses.hasMoreElements()){
            final String serverClass = (String)serverClasses.nextElement();
            final Vector<Integer> seqNumbers = this.curr_phase.totSeqNumbers.get(serverClass);
            int firstSeq;
            try{
                firstSeq = seqNumbers.elementAt(0);
            }catch(final Exception exc){
                firstSeq = -1;
            }
            final Enumeration<Integer> currSeqNumbers = seqNumbers.elements();
            this.totSeqNumbers.put(serverClass, currSeqNumbers);
            // currSeqNumbers contains the sequence number for the selected phase and for the selected server class
            if(currSeqNumbers.hasMoreElements()){
                this.phaseTerminated.put(serverClass, new Boolean(false));
                if(firstSynch >= 0 && firstSeq > firstSynch) // Can't start yet
                {
                    this.actSeqNumbers.put(serverClass, new Integer(-1));
                    this.totSeqDispatched.put(serverClass, new Vector<Action>());
                }else{
                    final Integer curr_int = currSeqNumbers.nextElement();
                    this.actSeqNumbers.put(serverClass, curr_int);
                    final Enumeration<Action> first_actions = this.curr_phase.totSeqActions.get(serverClass).get(curr_int).elements();
                    while(first_actions.hasMoreElements()){
                        final Action action = first_actions.nextElement();
                        if(action.isOn()){
                            this.doing_phase = true;
                            Vector<Action> currSeqDispatched = this.totSeqDispatched.get(serverClass);
                            if(currSeqDispatched == null) this.totSeqDispatched.put(serverClass, currSeqDispatched = new Vector<Action>());
                            currSeqDispatched.addElement(action);
                            action.setStatus(Action.DISPATCHED, 0, jDispatcher.verbose);
                            this.fireMonitorEvent(action, jDispatcher.MONITOR_DISPATCHED);
                            if(!this.balancer.enqueueAction(action)) this.discardAction(action);
                        }
                    }
                    anyDispatched = true;
                }
            }
        }
        if(anyDispatched) return true;
        System.out.println("XXX ------------- END PHASE --------------------- ");
        this.fireMonitorEvent((Action)null, jDispatcher.MONITOR_END_PHASE);
        return false; // no actions to be executed in this phase
    }

    protected void traverseDispatch(final ActionData action_data, final Data when, final PhaseDescriptor currPhase) {
        Action action;
        if(when == null) return;
        if(when instanceof ConditionData){
            final Vector<Action> act_vector = currPhase.dependencies.get(((ConditionData)when).getArgument());
            if(act_vector != null && (action = this.actions.get(action_data)) != null) act_vector.addElement(action);
            else System.out.println("Warning: condition does not refer to a known action");
        }else if(when instanceof ActionData){
            final Vector<Action> act_vector = currPhase.dependencies.get(when);
            if(act_vector != null && (action = this.actions.get(action_data)) != null) act_vector.addElement(action);
            else System.out.println("Warning: condition does not refer to a known action");
        }else if(when instanceof DependencyData){
            final Data[] args = ((DependencyData)when).getArguments();
            for(final Data arg : args)
                this.traverseDispatch(action_data, arg, currPhase);
        }
    }

    protected Data traverseSeqExpression(final ActionData action_data, final Data seq) {
        final Action action = this.actions.get(action_data);
        if(action == null){
            System.err.println("Internal error in traverseSeqExpression: no action for action_data");
            return null;
        }
        final Database tree = InfoServer.getDatabase();
        if(seq == null) return null;
        if(seq instanceof PathData || seq instanceof NidData){
            int nid;
            try{
                if(seq instanceof PathData) nid = (tree.resolve((PathData)seq, 0)).getInt();
                else nid = seq.getInt();
                Vector<Action> actVect = this.nidDependencies.get(new Integer(nid));
                if(actVect == null){
                    actVect = new Vector<Action>();
                    this.nidDependencies.put(new Integer(nid), actVect);
                }
                actVect.addElement(action);
                final String expr = "PUBLIC _ACTION_" + Integer.toHexString(nid) + " = COMPILE('$_UNDEFINED')";
                try{
                    tree.evaluateData(Data.fromExpr(expr), 0);
                }catch(final Exception exc){} // Will always generate an exception since the variable is undefined
                return new IdentData("_ACTION_" + Integer.toHexString(nid));
            }catch(final Exception exc){
                System.err.println("Error in resolving path names in sequential action: " + exc);
                return null;
            }
        }
        if(seq instanceof CompoundData){
            final Data[] descs = ((CompoundData)seq).getDescs();
            for(int i = 0; i < descs.length; i++)
                descs[i] = this.traverseSeqExpression(action_data, descs[i]);
        }
        return seq;
    }

    public synchronized void waitPhase() {
        try{
            while(this.doing_phase)
                this.wait();
        }catch(final InterruptedException exc){}
    }
}
