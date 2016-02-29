class MdsMonitor extends MdsActionsMonitor{
    public MdsMonitor(final int port){
        super(port);
    }

    @Override
    public synchronized void beginSequence(final MonitorEvent event) {
        this.communicate(event, jDispatcher.MONITOR_BEGIN_SEQUENCE);
    }

    @Override
    public void build(final MonitorEvent event) {
        this.communicate(event, jDispatcher.MONITOR_BUILD);
    }

    @Override
    public void buildBegin(final MonitorEvent event) {
        this.communicate(event, jDispatcher.MONITOR_BUILD_BEGIN);
    }

    @Override
    public void buildEnd(final MonitorEvent event) {
        this.communicate(event, jDispatcher.MONITOR_BUILD_END);
    }

    @Override
    public void connect(final MonitorEvent event) {
        this.communicate(event, MonitorEvent.CONNECT_EVENT);
    }

    @Override
    public void disconnect(final MonitorEvent event) {
        this.communicate(event, MonitorEvent.DISCONNECT_EVENT);
    }

    @Override
    public void dispatched(final MonitorEvent event) {
        this.communicate(event, jDispatcher.MONITOR_DISPATCHED);
    }

    @Override
    public void doing(final MonitorEvent event) {
        this.communicate(event, jDispatcher.MONITOR_DOING);
    }

    @Override
    public void endPhase(final MonitorEvent event) {
        this.communicate(event, MonitorEvent.END_PHASE_EVENT);
    }

    @Override
    public synchronized void endSequence(final MonitorEvent event) {
        this.communicate(event, jDispatcher.MONITOR_END_SEQUENCE);
    }

    @Override
    public void startPhase(final MonitorEvent event) {
        this.communicate(event, MonitorEvent.START_PHASE_EVENT);
    }
}
