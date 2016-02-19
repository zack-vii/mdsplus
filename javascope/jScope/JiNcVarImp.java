package jScope;

/* $Id$ */
import java.io.IOException;

abstract class JiNcVarImp{
    protected JiNcVar          mParent;
    protected RandomAccessData mRFile;
    protected long             mOffset;

    public JiNcVarImp(RandomAccessData in, JiNcVar parent, long offset){
        mParent = parent;
        mRFile = in;
        mOffset = offset;
    }

    public static void convertDoubles(byte[] bytes, double[] doubles) throws IOException {
        if(bytes.length % 8 != 0) throw new IOException();
        // Lack of unsigned types make this a real pain...
        int count = 0;
        for(int i = 0; i < bytes.length; i += 8){
            long result = bytes[i] & 0xff;
            result <<= 8;
            result |= bytes[i + 1] & 0xff;
            result <<= 8;
            result |= bytes[i + 2] & 0xff;
            result <<= 8;
            result |= bytes[i + 3] & 0xff;
            result <<= 8;
            result |= bytes[i + 4] & 0xff;
            result <<= 8;
            result |= bytes[i + 5] & 0xff;
            result <<= 8;
            result |= bytes[i + 6] & 0xff;
            result <<= 8;
            result |= bytes[i + 7] & 0xff;
            doubles[count++] = Double.longBitsToDouble(result);
        }
    }

    public static void convertFloats(byte[] bytes, float[] floats) throws IOException {
        if(bytes.length % 4 != 0) throw new IOException();
        // Lack of unsigned types make this a real pain...
        int count = 0;
        int ix = 0;
        while(ix < bytes.length){
            int val = ((bytes[ix++] & 0xff) << 24) + ((bytes[ix++] & 0xff) << 16) + ((bytes[ix++] & 0xff) << 8) + (bytes[ix++] & 0xff);
            floats[count++] = Float.intBitsToFloat(val);
        }
    }

    public static void convertInts(byte[] bytes, int[] ints) throws IOException {
        if(bytes.length % 4 != 0) throw new IOException();
        // Lack of unsigned types make this a real pain...
        int count = 0;
        for(int i = 0; i < bytes.length; i += 4){
            ints[count++] = ((bytes[i] & 0xff) << 24) + ((bytes[i + 1] & 0xff) << 16) + ((bytes[i + 2] & 0xff) << 8) + (bytes[i + 3] & 0xff);
        }
    }

    public static void convertShorts(byte[] bytes, short[] shorts) throws IOException {
        if(bytes.length % 2 != 0) throw new IOException();
        // Lack of unsigned types make this a real pain...
        int count = 0;
        for(int i = 0; i < bytes.length; i += 4){
            shorts[count++] = (short)(((bytes[i] & 0xff) << 8) + (bytes[i + 1] & 0xff));
        }
    }

    public abstract Object read(JiDim[] dims) throws IOException;

    @SuppressWarnings("static-method")
    public byte[] readByte(JiDim[] dims) throws IOException {
        return null;
    }

    @SuppressWarnings("static-method")
    public char[] readChar(JiDim[] dims) throws IOException {
        return null;
    }

    @SuppressWarnings("static-method")
    public double[] readDouble(JiDim[] dims) throws IOException {
        return null;
    }

    @SuppressWarnings("static-method")
    public float[] readFloat(JiDim[] dims) throws IOException {
        return null;
    }

    @SuppressWarnings("static-method")
    public int[] readInt(JiDim[] dims) throws IOException {
        return null;
    }

    @SuppressWarnings("static-method")
    public short[] readShort(JiDim[] dims) throws IOException {
        return null;
    }

    public abstract int sizeof();
}
