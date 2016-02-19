package jScope;

/* $Id$ */
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;
import java.util.zip.InflaterInputStream;

final public class MdsMessage extends Object{
    public static final int            HEADER_SIZE                = 48;
    public static final int            SUPPORTS_COMPRESSION       = 0x8000;
    public static final byte           SENDCAPABILITIES           = (byte)0xF;
    public static final byte           COMPRESSED                 = (byte)0x20;
    public static final byte           BIG_ENDIAN_MASK            = (byte)0x80;
    public static final byte           SWAP_ENDIAN_ON_SERVER_MASK = (byte)0x40;
    public static final byte           JAVA_CLIENT                = (byte)((byte)3 | BIG_ENDIAN_MASK | SWAP_ENDIAN_ON_SERVER_MASK);
    public static final String         EVENTASTREQUEST            = "---EVENTAST---REQUEST---";
    public static final String         EVENTCANREQUEST            = "---EVENTCAN---REQUEST---";
    static byte                        msgid                      = 1;
    int                                msglen;
    public int                         status;
    public int                         length;
    public byte                        nargs;
    public byte                        descr_idx;
    public byte                        message_id;
    public byte                        dtype;
    public byte                        client_type;
    public byte                        ndims;
    public int                         dims[];
    public byte                        body[];
    protected boolean                  swap                       = false;
    protected boolean                  compressed                 = false;
    private Vector<ConnectionListener> connection_listener        = null;

    public MdsMessage(){
        BuildMdsMessage((byte)0, (byte)0, (byte)0, null, new byte[1]);
        status = 1;
    }

    public MdsMessage(String s){
        this(s, null);
    }

    public MdsMessage(byte c){
        this(c, null);
    }

    public MdsMessage(String s, Vector<ConnectionListener> v){
        connection_listener = v;
        BuildMdsMessage((byte)0, Descriptor.DTYPE_CSTRING, (byte)1, null, s.getBytes());
    }

    public MdsMessage(byte c, Vector<ConnectionListener> v){
        connection_listener = v;
        BuildMdsMessage((byte)0, Descriptor.DTYPE_CSTRING, (byte)1, null, new byte[]{c});
    }

    public MdsMessage(byte descr_idx, byte dtype, byte nargs, int dims[], byte body[]){
        BuildMdsMessage(descr_idx, dtype, nargs, dims, body);
    }

    public void BuildMdsMessage(byte descr_idx, byte dtype, byte nargs, int dims[], byte body[]) {
        int body_size = (body != null ? body.length : 0);
        msglen = HEADER_SIZE + body_size;
        status = 0;
        message_id = msgid;
        this.length = Descriptor.getDataSize(dtype, body);
        this.nargs = nargs;
        this.descr_idx = descr_idx;
        if(dims != null) ndims = (byte)((dims.length > Descriptor.MAX_DIM) ? Descriptor.MAX_DIM : dims.length);
        else ndims = 0;
        this.dims = new int[Descriptor.MAX_DIM];
        for(int i = 0; i < Descriptor.MAX_DIM; i++)
            this.dims[i] = (dims != null && i < dims.length) ? dims[i] : 0;
        this.dtype = dtype;
        client_type = JAVA_CLIENT;
        this.body = body;
    }

    public void useCompression(boolean use_cmp) {
        status = (use_cmp ? SUPPORTS_COMPRESSION | 5 : 0);
    }

    protected synchronized byte[] ReadCompressedBuf(InputStream dis) throws IOException
    // protected synchronized byte[] ReadCompressedBuf(DataInputStream dis) throws IOException
    {
        int bytes_to_read, read_bytes = 0, curr_offset = 0;
        byte out[], b4[] = new byte[4];
        ReadBuf(b4, dis);
        bytes_to_read = ToInt(b4) - HEADER_SIZE;
        out = new byte[bytes_to_read];
        InflaterInputStream zis = new InflaterInputStream(dis);
        while(bytes_to_read > 0){
            read_bytes = zis.read(out, curr_offset, bytes_to_read);
            curr_offset += read_bytes;
            bytes_to_read -= read_bytes;
        }
        // remove EOF
        byte pp[] = new byte[1];
        while(zis.available() == 1)
            zis.read(pp);
        return out;
    }

    // protected /*synchronized */void ReadBuf(byte buf[], DataInputStream dis) throws IOException
    protected synchronized void ReadBuf(byte buf[], InputStream dis) throws IOException {
        ConnectionEvent e;
        int bytes_to_read = buf.length, read_bytes = 0, curr_offset = 0;
        boolean send = false;
        if(bytes_to_read > 2000){
            send = true;
            e = new ConnectionEvent(this, buf.length, curr_offset);
            dispatchConnectionEvent(e);
        }
        while(bytes_to_read > 0){
            read_bytes = dis.read(buf, curr_offset, bytes_to_read);
            curr_offset += read_bytes;
            bytes_to_read -= read_bytes;
            if(send){
                e = new ConnectionEvent(this, buf.length, curr_offset);
                dispatchConnectionEvent(e);
            }
        }
    }

    public synchronized void Send(DataOutputStream dos) throws IOException {
        dos.writeInt(msglen);
        dos.writeInt(status);
        dos.writeShort(length);
        dos.writeByte(nargs);
        dos.writeByte(descr_idx);
        dos.writeByte(message_id);
        dos.writeByte(dtype);
        dos.writeByte(client_type);
        dos.writeByte(ndims);
        for(int i = 0; i < Descriptor.MAX_DIM; i++)
            dos.writeInt(dims[i]);
        dos.write(body, 0, body.length);
        dos.flush();
        if(descr_idx == (nargs - 1)) msgid++;
        if(msgid == 0) msgid = 1;
    }

    protected static int ByteToIntSwap(byte b[], int idx) {
        int out;
        out = (b[idx + 3] & 0xff) << 24;
        out += (b[idx + 2] & 0xff) << 16;
        out += (b[idx + 1] & 0xff) << 8;
        out += (b[idx + 0] & 0xff) << 0;
        return out;
    }

    protected static int ByteToInt(byte b[], int idx) {
        int out;
        out = (b[idx + 0] & 0xff) << 24;
        out += (b[idx + 1] & 0xff) << 16;
        out += (b[idx + 2] & 0xff) << 8;
        out += (b[idx + 3] & 0xff) << 0;
        return out;
    }

    protected static short ByteToShortSwap(byte b[], int idx) {
        short out;
        out = (short)((b[idx + 1] & 0xff) << 8);
        out += (short)((b[idx + 0] & 0xff) << 0);
        return out;
    }

    protected static short ByteToShort(byte b[], int idx) {
        short out;
        out = (short)((b[idx + 0] & 0xff) << 8);
        out += (short)((b[idx + 1] & 0xff) << 0);
        return out;
    }

    // public /*synchronized */ void Receive(DataInputStream dis)throws IOException
    public synchronized void Receive(InputStream dis) throws IOException {
        byte header_b[] = new byte[16 + Descriptor.MAX_DIM * 4];
        int c_type = 0;
        int idx = 0;
        // ReadBuf(header_b, dis);
        /*
                if(dis.read(header_b) == -1)
                    throw(new IOException("Broken connection with mdsip server"));
        */
        // dis.readFully(header_b);
        // dis.read(header_b);
        ReadBuf(header_b, dis);
        c_type = header_b[14];
        swap = ((c_type & BIG_ENDIAN_MASK) != BIG_ENDIAN_MASK);
        compressed = ((c_type & COMPRESSED) == COMPRESSED);
        if(swap){
            msglen = ByteToIntSwap(header_b, 0);
            idx = 4;
            status = ByteToIntSwap(header_b, idx);
            idx += 4;
            length = ByteToShortSwap(header_b, idx);;
            idx += 2;
        }else{
            msglen = ByteToInt(header_b, 0);
            idx = 4;
            status = ByteToInt(header_b, idx);
            idx += 4;
            length = ByteToShort(header_b, idx);;
            idx += 2;
        }
        nargs = header_b[idx++];
        descr_idx = header_b[idx++];
        message_id = header_b[idx++];
        dtype = header_b[idx++];
        c_type = header_b[idx++];
        ndims = header_b[idx++];
        if(swap){
            for(int i = 0, j = idx; i < Descriptor.MAX_DIM; i++, j += 4)
                dims[i] = ByteToIntSwap(header_b, j);
        }else{
            for(int i = 0, j = idx; i < Descriptor.MAX_DIM; i++, j += 4)
                dims[i] = ByteToInt(header_b, j);
        }
        if(msglen > HEADER_SIZE){
            if(compressed) body = ReadCompressedBuf(dis);
            else{
                body = new byte[msglen - HEADER_SIZE];
                ReadBuf(body, dis);
            }
        }else body = new byte[0];
    }

    protected static void Flip(byte bytes[], int size) {
        byte b;
        for(int i = 0; i < bytes.length; i += size)
            if(size == 2){
                b = bytes[i];
                bytes[i] = bytes[i + 1];
                bytes[i + 1] = b;
            }else if(size == 4){
                b = bytes[i];
                bytes[i] = bytes[i + 3];
                bytes[i + 3] = b;
                b = bytes[i + 1];
                bytes[i + 1] = bytes[i + 2];
                bytes[i + 2] = b;
            }
    }

    protected int ToInt(byte bytes[]) throws IOException {
        if(swap) Flip(bytes, 4);
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        DataInputStream dis = new DataInputStream(bis);
        return dis.readInt();
    }

    protected short ToShort(byte bytes[]) throws IOException {
        if(swap) Flip(bytes, 2);
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        DataInputStream dis = new DataInputStream(bis);
        return dis.readShort();
    }

    protected float ToFloat(byte bytes[]) throws IOException {
        if(swap) Flip(bytes, 4);
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        DataInputStream dis = new DataInputStream(bis);
        return dis.readFloat();
    }

    public long[] ToLongArray() throws IOException {
        long out[] = new long[body.length / 8];
        if(swap) for(int i = 0, j = 0; i < out.length; i++){
            out[i] = (body[j++] & 0xffL) << 0;
            out[i] += (body[j++] & 0xffL) << 8;
            out[i] += (body[j++] & 0xffL) << 16;
            out[i] += (body[j++] & 0xffL) << 24;
            out[i] += (body[j++] & 0xffL) << 32;
            out[i] += (body[j++] & 0xffL) << 40;
            out[i] += (body[j++] & 0xffL) << 48;
            out[i] += (body[j++] & 0xffL) << 56;
        }
        else for(int i = 0, j = 0; i < out.length; i++){
            out[i] = (body[j++] & 0xffL) << 56;
            out[i] += (body[j++] & 0xffL) << 48;
            out[i] += (body[j++] & 0xffL) << 40;
            out[i] += (body[j++] & 0xffL) << 32;
            out[i] += (body[j++] & 0xffL) << 24;
            out[i] += (body[j++] & 0xffL) << 16;
            out[i] += (body[j++] & 0xffL) << 8;
            out[i] += (body[j++] & 0xffL) << 0;
        }
        return out;
    }

    public long[] ToUIntArray() throws IOException {
        long out[] = new long[body.length / 4];
        if(swap) for(int i = 0, j = 0; i < out.length; i++){
            out[i] = (body[j++] & 0xff) << 0;
            out[i] += (body[j++] & 0xff) << 8;
            out[i] += (body[j++] & 0xff) << 16;
            out[i] += (body[j++] & 0xff) << 24;
        }
        else for(int i = 0, j = 0; i < out.length; i++){
            out[i] = (body[j++] & 0xff) << 24;
            out[i] += (body[j++] & 0xff) << 16;
            out[i] += (body[j++] & 0xff) << 8;
            out[i] += (body[j++] & 0xff) << 0;
        }
        return out;
    }

    public int[] ToIntArray() throws IOException {
        int out[] = new int[body.length / 4];
        if(swap) for(int i = 0, j = 0; i < out.length; i++){
            out[i] = (body[j++] & 0xff) << 0;
            out[i] += (body[j++] & 0xff) << 8;
            out[i] += (body[j++] & 0xff) << 16;
            out[i] += (body[j++] & 0xff) << 24;
        }
        else for(int i = 0, j = 0; i < out.length; i++){
            out[i] = (body[j++] & 0xff) << 24;
            out[i] += (body[j++] & 0xff) << 16;
            out[i] += (body[j++] & 0xff) << 8;
            out[i] += (body[j++] & 0xff) << 0;
        }
        return out;
    }

    public short[] ToShortArray() throws IOException {
        short out[] = new short[body.length / 2];
        if(swap) for(int i = 0, j = 0; i < out.length; i++){
            out[i] = (short)((body[j++] & 0xff) << 0);
            out[i] += (short)((body[j++] & 0xff) << 8);
        }
        else for(int i = 0, j = 0; i < out.length; i++){
            out[i] = (short)((body[j++] & 0xff) << 8);
            out[i] += (short)((body[j++] & 0xff) << 0);
        }
        return out;
    }

    public int[] ToUShortArray() throws IOException {
        int out[] = new int[body.length / 2];
        if(swap) for(int i = 0, j = 0; i < out.length; i++){
            out[i] = (body[j++] & 0xff) << 0;
            out[i] += (body[j++] & 0xff) << 8;
        }
        else for(int i = 0, j = 0; i < out.length; i++){
            out[i] = (body[j++] & 0xff) << 8;
            out[i] += (body[j++] & 0xff) << 0;
        }
        return out;
    }

    public float[] ToFloatArray() throws IOException {
        int ch;
        float out[] = new float[body.length / 4];
        if(swap) for(int i = 0, j = 0; i < out.length; i++){
            ch = (body[j++] & 0xff) << 0;
            ch += (body[j++] & 0xff) << 8;
            ch += (body[j++] & 0xff) << 16;
            ch += (body[j++] & 0xff) << 24;
            out[i] = Float.intBitsToFloat(ch);
        }
        else for(int i = 0, j = 0; i < out.length; i++){
            ch = (body[j++] & 0xff) << 24;
            ch += (body[j++] & 0xff) << 16;
            ch += (body[j++] & 0xff) << 8;
            ch += (body[j++] & 0xff) << 0;
            out[i] = Float.intBitsToFloat(ch);
        }
        return out;
    }

    public double[] ToDoubleArray() throws IOException {
        long ch;
        double out[] = new double[body.length / 8];
        if(swap) for(int i = 0, j = 0; i < out.length; i++){
            ch = (body[j++] & 0xffL) << 0;
            ch += (body[j++] & 0xffL) << 8;
            ch += (body[j++] & 0xffL) << 16;
            ch += (body[j++] & 0xffL) << 24;
            ch += (body[j++] & 0xffL) << 32;
            ch += (body[j++] & 0xffL) << 40;
            ch += (body[j++] & 0xffL) << 48;
            ch += (body[j++] & 0xffL) << 56;
            out[i] = Double.longBitsToDouble(ch);
        }
        else for(int i = 0, j = 0; i < out.length; i++){
            ch = (body[j++] & 0xffL) << 56;
            ch += (body[j++] & 0xffL) << 48;
            ch += (body[j++] & 0xffL) << 40;
            ch += (body[j++] & 0xffL) << 32;
            ch += (body[j++] & 0xffL) << 24;
            ch += (body[j++] & 0xffL) << 16;
            ch += (body[j++] & 0xffL) << 8;
            ch += (body[j++] & 0xffL) << 0;
            out[i] = Double.longBitsToDouble(ch);
        }
        return out;
    }

    public String ToString() {
        return new String(body);
    }

    protected static boolean IsRoprand(final byte arr[], final int idx) {
        return(arr[idx] == 0 && arr[idx + 1] == 0 && arr[idx + 2] == -128 && arr[idx + 3] == 0);
    }

    synchronized protected void dispatchConnectionEvent(ConnectionEvent e) {
        if(connection_listener != null) for(int i = 0; i < connection_listener.size(); i++)
            connection_listener.elementAt(i).processConnectionEvent(e);
    }
}
