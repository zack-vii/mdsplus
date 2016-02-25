// package jTraverser;
public class ByteArray extends ArrayData{
    private static final long serialVersionUID = -3367858364462011080L;

    public static Data getData(final byte datum[], final boolean unsigned) {
        return new ByteArray(datum, unsigned);
    }
    byte datum[];

    public ByteArray(final byte datum[]){
        this(datum, false);
    }

    public ByteArray(final byte datum[], final boolean unsigned){
        this.length = datum.length;
        if(unsigned) this.dtype = Data.DTYPE_BU;
        else this.dtype = Data.DTYPE_B;
        this.datum = new byte[datum.length];
        for(int i = 0; i < datum.length; i++)
            this.datum[i] = datum[i];
    }

    public byte[] getByteArray() {
        return this.datum;
    }

    @Override
    public float[] getFloatArray() {
        final float ris[] = new float[this.datum.length];
        for(int i = 0; i < this.datum.length; i++)
            ris[i] = this.datum[i];
        return ris;
    }

    @Override
    public int[] getIntArray() {
        final int ris[] = new int[this.datum.length];
        for(int i = 0; i < this.datum.length; i++)
            ris[i] = this.datum[i];
        return ris;
    }
}