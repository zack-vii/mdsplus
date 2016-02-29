import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Enumeration;
import java.util.Vector;
import jScope.ConnectionEvent;
import jScope.ConnectionListener;
import mds.mdsMessage;

class MdsIp implements Runnable{
    class ReceiverThread extends Thread{
        DataInputStream  dis;
        DataOutputStream dos;
        Socket           sock;

        public ReceiverThread(final Socket sock){
            this.sock = sock;
            try{
                this.dis = new DataInputStream(new BufferedInputStream(sock.getInputStream()));
                this.dos = new DataOutputStream(new BufferedOutputStream(sock.getOutputStream()));
            }catch(final Exception exc){
                MdsIp.this.fireConnectionEvent();
                this.dis = null;
                this.dos = null;
            }
        }

        @Override
        public void run() {
            if(this.dis == null || this.dos == null) return;
            final mdsMessage msg = new mdsMessage((byte)0, (byte)0, (byte)0, null, new byte[0]);
            try{
                msg.Receive(this.dis); // Connection message
                (new mdsMessage()).Send(this.dos);
            }catch(final Exception exc){
                MdsIp.this.fireConnectionEvent();
                return;
            }
            while(true){
                try{
                    final mdsMessage curr_msg = new mdsMessage((byte)0, (byte)0, (byte)0, null, new byte[0]);
                    curr_msg.Receive(this.dis);
                    final mdsMessage messages[] = new mdsMessage[curr_msg.getNargs()];
                    messages[0] = curr_msg;
                    final int nargs = curr_msg.getNargs();
                    for(int i = 0; i < nargs - 1; i++){
                        messages[i + 1] = new mdsMessage((byte)0, (byte)0, (byte)0, null, new byte[0]);
                        messages[i + 1].Receive(this.dis);
                    }
                    final mdsMessage answ = MdsIp.this.handleMessage(messages);
                    answ.Send(this.dos);
                }catch(final Exception exc){
                    MdsIp.this.fireConnectionEvent();
                    break;
                }
            }
        }
    }
    Thread                     listen_thread;
    Vector<ConnectionListener> listeners = new Vector<ConnectionListener>();
    boolean                    listening = true;
    int                        port;
    ServerSocket               server_sock;

    public MdsIp(final int port){
        this.port = port;
    }

    public synchronized void addConnectionListener(final ConnectionListener listener) {
        this.listeners.addElement(listener);
    }

    protected synchronized void fireConnectionEvent() {
        final Enumeration<ConnectionListener> listener_list = this.listeners.elements();
        while(listener_list.hasMoreElements()){
            final ConnectionListener listener = listener_list.nextElement();
            listener.processConnectionEvent(new ConnectionEvent(this, ConnectionEvent.LOST_CONNECTION, "Lost connection to mdsip client"));
        }
    }

    public Thread getListenThread() {
        return this.listen_thread;
    }

    @SuppressWarnings("static-method")
    public /*synchronized*/ mdsMessage handleMessage(final mdsMessage messages[]) {
        return new mdsMessage();
    }

    @Override
    public void run() {
        while(this.listening){
            try{
                new ReceiverThread(this.server_sock.accept()).start();
            }catch(final Exception exc){
                this.fireConnectionEvent();
                break;
            }
        }
        try{
            this.server_sock.close();
        }catch(final Exception exc){}
    }

    public boolean start() {
        try{
            this.server_sock = new ServerSocket(this.port);
        }catch(final Exception e){
            System.err.println("Could not listen on port: " + this.port);
            return false;
        }
        this.listen_thread = new Thread(this);
        this.listen_thread.start();
        return true;
    }
}
