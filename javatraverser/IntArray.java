// package jTraverser;
public class IntArray extends ArrayData{
    private static final long serialVersionUID = 4956194520212221705L;

    public static Data getData(final int datum[], final boolean unsigned) {
        return new IntArray(datum, unsigned);
    }
    int datum[];

    public IntArray(final int datum[]){
        this(datum, false);
    }

    public IntArray(final int datum[], final boolean unsigned){
        this.length = datum.length;
        if(unsigned) this.dtype = Data.DTYPE_LU;
        else this.dtype = Data.DTYPE_L;
        this.datum = new int[datum.length];
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
        return this.datum;
    }
}