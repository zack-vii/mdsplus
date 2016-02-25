// package jTraverser;
public class DoubleArray extends ArrayData{
    private static final long serialVersionUID = -7094278403689619008L;

    public static Data getData(final double datum[], final int flags) {
        return new DoubleArray(datum, flags);
    }
    double datum[];
    int    flags = Data.DTYPE_DOUBLE;

    public DoubleArray(final double datum[]){
        this(datum, Data.DTYPE_DOUBLE);
    }

    public DoubleArray(final double datum[], final int flags){
        this.length = datum.length;
        this.dtype = Data.DTYPE_DOUBLE;
        this.datum = new double[datum.length];
        for(int i = 0; i < datum.length; i++)
            this.datum[i] = datum[i];
        this.flags = flags;
    }

    public int getFlags() {
        return this.flags;
    }

    @Override
    public float[] getFloatArray() {
        final float ris[] = new float[this.datum.length];
        for(int i = 0; i < this.datum.length; i++)
            ris[i] = (float)this.datum[i];
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