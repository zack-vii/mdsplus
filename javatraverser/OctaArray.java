// package jTraverser;
public class OctaArray extends ArrayData{
    private static final long serialVersionUID = -5422495107574469505L;

    public static Data getData(final long datum[], final boolean unsigned) {
        return new OctaArray(datum, unsigned);
    }
    long datum[];

    public OctaArray(final long datum[]){
        this(datum, false);
    }

    public OctaArray(final long datum[], final boolean unsigned){
        this.length = datum.length / 2;
        if(unsigned) this.dtype = Data.DTYPE_OU;
        else this.dtype = Data.DTYPE_O;
        this.datum = new long[datum.length];
        for(int i = 0; i < datum.length; i++)
            this.datum[i] = datum[i];
    }

    @Override
    public float[] getFloatArray() {
        final float ris[] = new float[this.datum.length];
        for(int i = 0; i < this.datum.length; i++)
            ris[i] = this.datum[2 * i];
        return ris;
    }

    @Override
    public int[] getIntArray() {
        final int ris[] = new int[this.datum.length];
        for(int i = 0; i < this.datum.length; i++)
            ris[i] = (int)this.datum[2 * i];
        return ris;
    }
}