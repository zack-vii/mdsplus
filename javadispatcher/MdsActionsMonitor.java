import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Enumeration;
import java.util.Vector;
import jScope.Descriptor;
import mds.mdsMessage;

class MdsActionsMonitor extends MdsIp implements MonitorListener, Runnable{
    Vector<MdsMonitorEvent>      msg_vect       = new Vector<MdsMonitorEvent>();
    Vector<BufferedOutputStream> outstream_vect = new Vector<BufferedOutputStream>();

    public MdsActionsMonitor(final int port){
        super(port);
        new Thread(new Runnable(){
            @Override
            public void run() {
                MdsActionsMonitor.this.sendMessages();
            }
        }).start();
    }

    @Override
    public synchronized void beginSequence(final MonitorEvent event) {}

    @Override
    public void build(final MonitorEvent event) {}

    @Override
    public void buildBegin(final MonitorEvent event) {}

    @Override
    public void buildEnd(final MonitorEvent event) {}

    protected void communicate(final MonitorEvent event, final int mode) {
        try{
            MdsMonitorEvent mds_event = null;
            if(event.getAction() == null){
                int currMode = 0;
                switch(event.eventId){
                    case MonitorEvent.CONNECT_EVENT:
                        currMode = MdsMonitorEvent.MonitorServerConnected;
                        mds_event = new MdsMonitorEvent(this, null, 0, 0, 0, null, 1, currMode, null, event.getMessage(), 1);
                        break;
                    case MonitorEvent.DISCONNECT_EVENT:
                        currMode = MdsMonitorEvent.MonitorServerDisconnected;
                        mds_event = new MdsMonitorEvent(this, null, 0, 0, 0, null, 1, currMode, null, event.getMessage(), 1);
                        break;
                    case MonitorEvent.START_PHASE_EVENT:
                        currMode = MdsMonitorEvent.MonitorStartPhase;
                        mds_event = new MdsMonitorEvent(this, event.getTree(), event.getShot(), MdsHelper.toPhaseId(event.getPhase()), 0, null, 1, currMode, null, null, 1);
                        break;
                    case MonitorEvent.END_PHASE_EVENT:
                        currMode = MdsMonitorEvent.MonitorEndPhase;
                        mds_event = new MdsMonitorEvent(this, event.getTree(), event.getShot(), MdsHelper.toPhaseId(event.getPhase()), 0, null, 1, currMode, null, null, 1);
                        break;
                    default:
                        mds_event = new MdsMonitorEvent(this, event.getTree(), event.getShot(), 0, 0, null, 1, mode, null, null, 1);
                }
            }else{
                final Action action = event.getAction();
                mds_event = new MdsMonitorEvent(this, event.getTree(), event.getShot(), MdsHelper.toPhaseId(event.getPhase()), action.getNid(), action.getName(), action.isOn() ? 1 : 0, mode, ((DispatchData)(action.getAction().getDispatch())).getIdent().getString(), action.getServerAddress(), action.getStatus());
            }
            this.msg_vect.addElement(mds_event);
            synchronized(this){
                this.notify();
            }
        }catch(final Exception exc){
            System.out.println(exc);
        }
    }

    @Override
    public void connect(final MonitorEvent event) {}

    @Override
    public void disconnect(final MonitorEvent event) {}

    @Override
    public void dispatched(final MonitorEvent event) {}

    @Override
    public void doing(final MonitorEvent event) {}

    @Override
    public synchronized void done(final MonitorEvent event) {
        this.communicate(event, jDispatcher.MONITOR_DONE);
    }

    @Override
    public void endPhase(final MonitorEvent event) {}

    @Override
    public synchronized void endSequence(final MonitorEvent event) {}

    public mdsMessage handleMessage(final mdsMessage[] messages) {
        try{
            final short port = messages[2].asShortArray()[0];
            final byte[] ip = messages[1].asByteArray();
            final String addr = (ip[0] & 0xff) + "." + (ip[1] & 0xff) + "." + (ip[2] & 0xff) + "." + (ip[3] & 0xff);
            final Socket sock = new Socket(addr, port);
            this.outstream_vect.addElement(new BufferedOutputStream(sock.getOutputStream()));
            sock.close();
        }catch(final Exception exc){
            System.err.println("Unexpected message has been received by MdsMonitor");
        }
        final mdsMessage msg = new mdsMessage((byte)0, Descriptor.DTYPE_LONG, (byte)0, null, Descriptor.dataToByteArray(1));
        msg.verify();
        return msg;
    }

    public void sendMessages() {
        while(true){
            while(this.msg_vect.size() != 0){
                final MdsMonitorEvent msg = this.msg_vect.elementAt(0);
                this.msg_vect.removeElementAt(0);
                final byte[] bin_msg = msg.toBytes();
                final Enumeration outstream_list = this.outstream_vect.elements();
                while(outstream_list.hasMoreElements()){
                    final OutputStream os = (OutputStream)outstream_list.nextElement();
                    try{
                        os.write(bin_msg);
                        os.flush();
                    }catch(final Exception exc){}
                }
            }
            try{
                synchronized(MdsActionsMonitor.this){
                    this.wait();
                }
            }catch(final InterruptedException exc){
                return;
            }
        }
    }

    @Override
    public void startPhase(final MonitorEvent event) {}
}
