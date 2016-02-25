// package jTraverser;
public class FloatArray extends ArrayData{
    private static final long serialVersionUID = -663878241016764137L;

    public static Data getData(final float datum[], final int flags) {
        return new FloatArray(datum, flags);
    }
    float datum[];
    int   flags = Data.DTYPE_FLOAT;

    public FloatArray(final float datum[]){
        this(datum, Data.DTYPE_FLOAT);
    }

    public FloatArray(final float datum[], final int flags){
        this.length = datum.length;
        this.dtype = Data.DTYPE_FLOAT;
        this.datum = new float[datum.length];
        for(int i = 0; i < datum.length; i++)
            this.datum[i] = datum[i];
        this.flags = flags;
    }

    public int getFlags() {
        return this.flags;
    }

    @Override
    public float[] getFloatArray() {
        return this.datum;
    }

    @Override
    public int[] getIntArray() {
        final int ris[] = new int[this.datum.length];
        for(int i = 0; i < this.datum.length; i++)
            ris[i] = (int)this.datum[i];
        return ris;
    }
}