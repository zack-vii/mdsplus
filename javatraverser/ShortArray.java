// package jTraverser;
public class ShortArray extends ArrayData{
    private static final long serialVersionUID = -430377525509897700L;

    public static Data getData(final short datum[], final boolean unsigned) {
        return new ShortArray(datum, unsigned);
    }
    short datum[];

    public ShortArray(final short datum[]){
        this(datum, false);
    }

    public ShortArray(final short datum[], final boolean unsigned){
        this.length = datum.length;
        if(unsigned) this.dtype = Data.DTYPE_WU;
        else this.dtype = Data.DTYPE_W;
        this.datum = new short[datum.length];
        for(int i = 0; i < datum.length; i++)
            this.datum[i] = datum[i];
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