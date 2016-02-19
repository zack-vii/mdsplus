package jScope;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * MdsProtocolWrapper handles mdstcpip management for protocol plugin
 */
final public class MdsIpProtocolWrapper{
    class MdsIpInputStream extends InputStream{
        @Override
        public int read() throws IOException {
            if(MdsIpProtocolWrapper.this.connectionIdx == -1) throw new IOException("Not Connected");
            final byte[] readBuf = MdsIpProtocolWrapper.this.recv(MdsIpProtocolWrapper.this.connectionIdx, 1);
            if(readBuf == null) throw new IOException("Cannot Read Data");
            return readBuf[0];
        }

        @Override
        public int read(final byte buf[]) throws IOException {
            if(MdsIpProtocolWrapper.this.connectionIdx == -1) throw new IOException("Not Connected");
            final byte[] readBuf = MdsIpProtocolWrapper.this.recv(MdsIpProtocolWrapper.this.connectionIdx, buf.length);
            if(readBuf == null) throw new IOException("Cannot Read Data");
            System.arraycopy(readBuf, 0, buf, 0, buf.length);
            return buf.length;
        }

        @Override
        public int read(final byte buf[], final int offset, final int len) throws IOException {
            if(MdsIpProtocolWrapper.this.connectionIdx == -1) throw new IOException("Not Connected");
            final byte[] readBuf = MdsIpProtocolWrapper.this.recv(MdsIpProtocolWrapper.this.connectionIdx, len);
            if(readBuf == null || readBuf.length == 0) throw new IOException("Cannot Read Data");
            System.arraycopy(readBuf, 0, buf, offset, readBuf.length);
            return readBuf.length;
        }
    }
    class MdsIpOutputStream extends OutputStream{
        /*        public void flush() throws IOException
                {
        System.out.println("FLUSH..");
                    if(connectionIdx == -1)  throw new IOException("Not Connected");
                    MdsIpProtocolWrapper.this.flush(connectionIdx);
        System.out.println("FLUSH FATTO");
                }
         */@Override
         public void close() throws IOException {
             if(MdsIpProtocolWrapper.this.connectionIdx != -1){
                 MdsIpProtocolWrapper.this.disconnect(MdsIpProtocolWrapper.this.connectionIdx);
                 MdsIpProtocolWrapper.this.connectionIdx = -1;
             }
         }

         @Override
         public void write(final byte[] b) throws IOException {
             if(MdsIpProtocolWrapper.this.connectionIdx == -1) throw new IOException("Not Connected");
             final int numSent = MdsIpProtocolWrapper.this.send(MdsIpProtocolWrapper.this.connectionIdx, b, false);
             if(numSent == b.length) throw new IOException("Incomplete write");
         }

         @Override
         public void write(final int b) throws IOException {
             if(MdsIpProtocolWrapper.this.connectionIdx == -1) throw new IOException("Not Connected");
             final int numSent = MdsIpProtocolWrapper.this.send(MdsIpProtocolWrapper.this.connectionIdx, new byte[]{(byte)b}, false);
             if(numSent == -1) throw new IOException("Cannot Write Data");
         }
    }
    static{
        try{
            System.loadLibrary("JavaMds");
        }catch(final UnsatisfiedLinkError e){
            javax.swing.JOptionPane.showMessageDialog(null, "Can't load data provider class LocalDataProvider : " + e, "Alert LocalDataProvider", javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(final String args[]) {
        final MdsIpProtocolWrapper mpw = new MdsIpProtocolWrapper("tcp");
        final int idx = mpw.connectToMds("tcp://ra22.igi.cnr.it:8100");
        System.out.println("Connected: " + idx);
    }
    int connectionIdx = -1;

    public MdsIpProtocolWrapper(final String url){
        this.connectionIdx = this.connectToMds(url);
    }

    public native int connectToMds(String url);

    public native void disconnect(int connectionId);

    public native void flush(int connectionId);

    InputStream getInputStream() {
        return new MdsIpInputStream();
    }

    OutputStream getOutputStream() {
        return new MdsIpOutputStream();
    }

    public native byte[] recv(int connectionId, int len);

    public native int send(int connectionId, byte[] sendBuf, boolean nowait);
}
