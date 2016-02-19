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
    public static final byte   BIG_ENDIAN_MASK            = (byte)0x80;
    public static final byte   COMPRESSED                 = (byte)0x20;
    public static final String EVENTASTREQUEST            = "---EVENTAST---REQUEST---";
    public static final String EVENTCANREQUEST            = "---EVENTCAN---REQUEST---";
    public static final int    HEADER_SIZE                = 48;
    public static final byte   JAVA_CLIENT                = (byte)((byte)3 | MdsMessage.BIG_ENDIAN_MASK | MdsMessage.SWAP_ENDIAN_ON_SERVER_MASK);
    static byte                msgid                      = 1;
    public static final byte   SENDCAPABILITIES           = (byte)0xF;
    public static final int    SUPPORTS_COMPRESSION       = 0x8000;
    public static final byte   SWAP_ENDIAN_ON_SERVER_MASK = (byte)0x40;

    protected static int ByteToInt(final byte b[], final int idx) {
        int out;
        out = (b[idx + 0] & 0xff) << 24;
        out += (b[idx + 1] & 0xff) << 16;
        out += (b[idx + 2] & 0xff) << 8;
        out += (b[idx + 3] & 0xff) << 0;
        return out;
    }

    protected static int ByteToIntSwap(final byte b[], final int idx) {
        int out;
        out = (b[idx + 3] & 0xff) << 24;
        out += (b[idx + 2] & 0xff) << 16;
        out += (b[idx + 1] & 0xff) << 8;
        out += (b[idx + 0] & 0xff) << 0;
        return out;
    }

    protected static short ByteToShort(final byte b[], final int idx) {
        short out;
        out = (short)((b[idx + 0] & 0xff) << 8);
        out += (short)((b[idx + 1] & 0xff) << 0);
        return out;
    }

    protected static short ByteToShortSwap(final byte b[], final int idx) {
        short out;
        out = (short)((b[idx + 1] & 0xff) << 8);
        out += (short)((b[idx + 0] & 0xff) << 0);
        return out;
    }

    protected static void Flip(final byte bytes[], final int size) {
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

    protected static boolean IsRoprand(final byte arr[], final int idx) {
        return(arr[idx] == 0 && arr[idx + 1] == 0 && arr[idx + 2] == -128 && arr[idx + 3] == 0);
    }
    public byte                        body[];
    public byte                        client_type;
    protected boolean                  compressed          = false;
    private Vector<ConnectionListener> connection_listener = null;
    public byte                        descr_idx;
    public int                         dims[];
    public byte                        dtype;
    public int                         length;
    public byte                        message_id;
    int                                msglen;
    public byte                        nargs;
    public byte                        ndims;
    public int                         status;
    protected boolean                  swap                = false;

    public MdsMessage(){
        this.BuildMdsMessage((byte)0, (byte)0, (byte)0, null, new byte[1]);
        this.status = 1;
    }

    public MdsMessage(final byte c){
        this(c, null);
    }

    public MdsMessage(final byte descr_idx, final byte dtype, final byte nargs, final int dims[], final byte body[]){
        this.BuildMdsMessage(descr_idx, dtype, nargs, dims, body);
    }

    public MdsMessage(final byte c, final Vector<ConnectionListener> v){
        this.connection_listener = v;
        this.BuildMdsMessage((byte)0, Descriptor.DTYPE_CSTRING, (byte)1, null, new byte[]{c});
    }

    public MdsMessage(final String s){
        this(s, null);
    }

    public MdsMessage(final String s, final Vector<ConnectionListener> v){
        this.connection_listener = v;
        this.BuildMdsMessage((byte)0, Descriptor.DTYPE_CSTRING, (byte)1, null, s.getBytes());
    }

    public void BuildMdsMessage(final byte descr_idx, final byte dtype, final byte nargs, final int dims[], final byte body[]) {
        final int body_size = (body != null ? body.length : 0);
        this.msglen = MdsMessage.HEADER_SIZE + body_size;
        this.status = 0;
        this.message_id = MdsMessage.msgid;
        this.length = Descriptor.getDataSize(dtype, body);
        this.nargs = nargs;
        this.descr_idx = descr_idx;
        if(dims != null) this.ndims = (byte)((dims.length > Descriptor.MAX_DIM) ? Descriptor.MAX_DIM : dims.length);
        else this.ndims = 0;
        this.dims = new int[Descriptor.MAX_DIM];
        for(int i = 0; i < Descriptor.MAX_DIM; i++)
            this.dims[i] = (dims != null && i < dims.length) ? dims[i] : 0;
            this.dtype = dtype;
            this.client_type = MdsMessage.JAVA_CLIENT;
            this.body = body;
    }

    synchronized protected void dispatchConnectionEvent(final ConnectionEvent e) {
        if(this.connection_listener != null) for(int i = 0; i < this.connection_listener.size(); i++)
            this.connection_listener.elementAt(i).processConnectionEvent(e);
    }

    // protected /*synchronized */void ReadBuf(byte buf[], DataInputStream dis) throws IOException
    protected synchronized void ReadBuf(final byte buf[], final InputStream dis) throws IOException {
        ConnectionEvent e;
        int bytes_to_read = buf.length, read_bytes = 0, curr_offset = 0;
        boolean send = false;
        if(bytes_to_read > 2000){
            send = true;
            e = new ConnectionEvent(this, buf.length, curr_offset);
            this.dispatchConnectionEvent(e);
        }
        while(bytes_to_read > 0){
            read_bytes = dis.read(buf, curr_offset, bytes_to_read);
            curr_offset += read_bytes;
            bytes_to_read -= read_bytes;
            if(send){
                e = new ConnectionEvent(this, buf.length, curr_offset);
                this.dispatchConnectionEvent(e);
            }
        }
    }

    protected synchronized byte[] ReadCompressedBuf(final InputStream dis) throws IOException
    // protected synchronized byte[] ReadCompressedBuf(DataInputStream dis) throws IOException
    {
        int bytes_to_read, read_bytes = 0, curr_offset = 0;
        byte out[];
        final byte b4[] = new byte[4];
        this.ReadBuf(b4, dis);
        bytes_to_read = this.ToInt(b4) - MdsMessage.HEADER_SIZE;
        out = new byte[bytes_to_read];
        final InflaterInputStream zis = new InflaterInputStream(dis);
        while(bytes_to_read > 0){
            read_bytes = zis.read(out, curr_offset, bytes_to_read);
            curr_offset += read_bytes;
            bytes_to_read -= read_bytes;
        }
        // remove EOF
        final byte pp[] = new byte[1];
        while(zis.available() == 1)
            zis.read(pp);
        return out;
    }

    // public /*synchronized */ void Receive(DataInputStream dis)throws IOException
    public synchronized void Receive(final InputStream dis) throws IOException {
        final byte header_b[] = new byte[16 + Descriptor.MAX_DIM * 4];
        int c_type = 0;
        int idx = 0;
        // ReadBuf(header_b, dis);
        /*
                if(dis.read(header_b) == -1)
                    throw(new IOException("Broken connection with mdsip server"));
         */
        // dis.readFully(header_b);
        // dis.read(header_b);
        this.ReadBuf(header_b, dis);
        c_type = header_b[14];
        this.swap = ((c_type & MdsMessage.BIG_ENDIAN_MASK) != MdsMessage.BIG_ENDIAN_MASK);
        this.compressed = ((c_type & MdsMessage.COMPRESSED) == MdsMessage.COMPRESSED);
        if(this.swap){
            this.msglen = MdsMessage.ByteToIntSwap(header_b, 0);
            idx = 4;
            this.status = MdsMessage.ByteToIntSwap(header_b, idx);
            idx += 4;
            this.length = MdsMessage.ByteToShortSwap(header_b, idx);;
            idx += 2;
        }else{
            this.msglen = MdsMessage.ByteToInt(header_b, 0);
            idx = 4;
            this.status = MdsMessage.ByteToInt(header_b, idx);
            idx += 4;
            this.length = MdsMessage.ByteToShort(header_b, idx);;
            idx += 2;
        }
        this.nargs = header_b[idx++];
        this.descr_idx = header_b[idx++];
        this.message_id = header_b[idx++];
        this.dtype = header_b[idx++];
        c_type = header_b[idx++];
        this.ndims = header_b[idx++];
        if(this.swap){
            for(int i = 0, j = idx; i < Descriptor.MAX_DIM; i++, j += 4)
                this.dims[i] = MdsMessage.ByteToIntSwap(header_b, j);
        }else{
            for(int i = 0, j = idx; i < Descriptor.MAX_DIM; i++, j += 4)
                this.dims[i] = MdsMessage.ByteToInt(header_b, j);
        }
        if(this.msglen > MdsMessage.HEADER_SIZE){
            if(this.compressed) this.body = this.ReadCompressedBuf(dis);
            else{
                this.body = new byte[this.msglen - MdsMessage.HEADER_SIZE];
                this.ReadBuf(this.body, dis);
            }
        }else this.body = new byte[0];
    }

    public synchronized void Send(final DataOutputStream dos) throws IOException {
        dos.writeInt(this.msglen);
        dos.writeInt(this.status);
        dos.writeShort(this.length);
        dos.writeByte(this.nargs);
        dos.writeByte(this.descr_idx);
        dos.writeByte(this.message_id);
        dos.writeByte(this.dtype);
        dos.writeByte(this.client_type);
        dos.writeByte(this.ndims);
        for(int i = 0; i < Descriptor.MAX_DIM; i++)
            dos.writeInt(this.dims[i]);
        dos.write(this.body, 0, this.body.length);
        dos.flush();
        if(this.descr_idx == (this.nargs - 1)) MdsMessage.msgid++;
        if(MdsMessage.msgid == 0) MdsMessage.msgid = 1;
    }

    public double[] ToDoubleArray() throws IOException {
        long ch;
        final double out[] = new double[this.body.length / 8];
        if(this.swap) for(int i = 0, j = 0; i < out.length; i++){
            ch = (this.body[j++] & 0xffL) << 0;
            ch += (this.body[j++] & 0xffL) << 8;
            ch += (this.body[j++] & 0xffL) << 16;
            ch += (this.body[j++] & 0xffL) << 24;
            ch += (this.body[j++] & 0xffL) << 32;
            ch += (this.body[j++] & 0xffL) << 40;
            ch += (this.body[j++] & 0xffL) << 48;
            ch += (this.body[j++] & 0xffL) << 56;
            out[i] = Double.longBitsToDouble(ch);
        }
        else for(int i = 0, j = 0; i < out.length; i++){
            ch = (this.body[j++] & 0xffL) << 56;
            ch += (this.body[j++] & 0xffL) << 48;
            ch += (this.body[j++] & 0xffL) << 40;
            ch += (this.body[j++] & 0xffL) << 32;
            ch += (this.body[j++] & 0xffL) << 24;
            ch += (this.body[j++] & 0xffL) << 16;
            ch += (this.body[j++] & 0xffL) << 8;
            ch += (this.body[j++] & 0xffL) << 0;
            out[i] = Double.longBitsToDouble(ch);
        }
        return out;
    }

    protected float ToFloat(final byte bytes[]) throws IOException {
        if(this.swap) MdsMessage.Flip(bytes, 4);
        final ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        final DataInputStream dis = new DataInputStream(bis);
        return dis.readFloat();
    }

    public float[] ToFloatArray() throws IOException {
        int ch;
        final float out[] = new float[this.body.length / 4];
        if(this.swap) for(int i = 0, j = 0; i < out.length; i++){
            ch = (this.body[j++] & 0xff) << 0;
            ch += (this.body[j++] & 0xff) << 8;
            ch += (this.body[j++] & 0xff) << 16;
            ch += (this.body[j++] & 0xff) << 24;
            out[i] = Float.intBitsToFloat(ch);
        }
        else for(int i = 0, j = 0; i < out.length; i++){
            ch = (this.body[j++] & 0xff) << 24;
            ch += (this.body[j++] & 0xff) << 16;
            ch += (this.body[j++] & 0xff) << 8;
            ch += (this.body[j++] & 0xff) << 0;
            out[i] = Float.intBitsToFloat(ch);
        }
        return out;
    }

    protected int ToInt(final byte bytes[]) throws IOException {
        if(this.swap) MdsMessage.Flip(bytes, 4);
        final ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        final DataInputStream dis = new DataInputStream(bis);
        return dis.readInt();
    }

    public int[] ToIntArray() throws IOException {
        final int out[] = new int[this.body.length / 4];
        if(this.swap) for(int i = 0, j = 0; i < out.length; i++){
            out[i] = (this.body[j++] & 0xff) << 0;
            out[i] += (this.body[j++] & 0xff) << 8;
            out[i] += (this.body[j++] & 0xff) << 16;
            out[i] += (this.body[j++] & 0xff) << 24;
        }
        else for(int i = 0, j = 0; i < out.length; i++){
            out[i] = (this.body[j++] & 0xff) << 24;
            out[i] += (this.body[j++] & 0xff) << 16;
            out[i] += (this.body[j++] & 0xff) << 8;
            out[i] += (this.body[j++] & 0xff) << 0;
        }
        return out;
    }

    public long[] ToLongArray() throws IOException {
        final long out[] = new long[this.body.length / 8];
        if(this.swap) for(int i = 0, j = 0; i < out.length; i++){
            out[i] = (this.body[j++] & 0xffL) << 0;
            out[i] += (this.body[j++] & 0xffL) << 8;
            out[i] += (this.body[j++] & 0xffL) << 16;
            out[i] += (this.body[j++] & 0xffL) << 24;
            out[i] += (this.body[j++] & 0xffL) << 32;
            out[i] += (this.body[j++] & 0xffL) << 40;
            out[i] += (this.body[j++] & 0xffL) << 48;
            out[i] += (this.body[j++] & 0xffL) << 56;
        }
        else for(int i = 0, j = 0; i < out.length; i++){
            out[i] = (this.body[j++] & 0xffL) << 56;
            out[i] += (this.body[j++] & 0xffL) << 48;
            out[i] += (this.body[j++] & 0xffL) << 40;
            out[i] += (this.body[j++] & 0xffL) << 32;
            out[i] += (this.body[j++] & 0xffL) << 24;
            out[i] += (this.body[j++] & 0xffL) << 16;
            out[i] += (this.body[j++] & 0xffL) << 8;
            out[i] += (this.body[j++] & 0xffL) << 0;
        }
        return out;
    }

    protected short ToShort(final byte bytes[]) throws IOException {
        if(this.swap) MdsMessage.Flip(bytes, 2);
        final ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        final DataInputStream dis = new DataInputStream(bis);
        return dis.readShort();
    }

    public short[] ToShortArray() throws IOException {
        final short out[] = new short[this.body.length / 2];
        if(this.swap) for(int i = 0, j = 0; i < out.length; i++){
            out[i] = (short)((this.body[j++] & 0xff) << 0);
            out[i] += (short)((this.body[j++] & 0xff) << 8);
        }
        else for(int i = 0, j = 0; i < out.length; i++){
            out[i] = (short)((this.body[j++] & 0xff) << 8);
            out[i] += (short)((this.body[j++] & 0xff) << 0);
        }
        return out;
    }

    public String ToString() {
        return new String(this.body);
    }

    public long[] ToUIntArray() throws IOException {
        final long out[] = new long[this.body.length / 4];
        if(this.swap) for(int i = 0, j = 0; i < out.length; i++){
            out[i] = (this.body[j++] & 0xff) << 0;
            out[i] += (this.body[j++] & 0xff) << 8;
            out[i] += (this.body[j++] & 0xff) << 16;
            out[i] += (this.body[j++] & 0xff) << 24;
        }
        else for(int i = 0, j = 0; i < out.length; i++){
            out[i] = (this.body[j++] & 0xff) << 24;
            out[i] += (this.body[j++] & 0xff) << 16;
            out[i] += (this.body[j++] & 0xff) << 8;
            out[i] += (this.body[j++] & 0xff) << 0;
        }
        return out;
    }

    public int[] ToUShortArray() throws IOException {
        final int out[] = new int[this.body.length / 2];
        if(this.swap) for(int i = 0, j = 0; i < out.length; i++){
            out[i] = (this.body[j++] & 0xff) << 0;
            out[i] += (this.body[j++] & 0xff) << 8;
        }
        else for(int i = 0, j = 0; i < out.length; i++){
            out[i] = (this.body[j++] & 0xff) << 8;
            out[i] += (this.body[j++] & 0xff) << 0;
        }
        return out;
    }

    public void useCompression(final boolean use_cmp) {
        this.status = (use_cmp ? MdsMessage.SUPPORTS_COMPRESSION | 5 : 0);
    }
}
