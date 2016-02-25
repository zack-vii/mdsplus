// package jTraverser;
public class QuadArray extends ArrayData{
    private static final long serialVersionUID = -181698604757148501L;

    public static Data getData(final long datum[], final boolean unsigned) {
        return new QuadArray(datum, unsigned);
    }
    long datum[];

    public QuadArray(final long datum[]){
        this(datum, false);
    }

    public QuadArray(final long datum[], final boolean unsigned){
        this.length = datum.length;
        if(unsigned) this.dtype = Data.DTYPE_QU;
        else this.dtype = Data.DTYPE_Q;
        this.datum = new long[datum.length];
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
            ris[i] = (int)this.datum[i];
        return ris;
    }
}