// package jTraverser;
public class OctaData extends AtomicData{
    private static final long serialVersionUID = 8585918019070580881L;

    public static Data getData(final long datum[], final boolean unsigned) {
        return new OctaData(datum, unsigned);
    }
    long datum[];

    public OctaData(final long datum[]){
        this(datum, false);
    }

    public OctaData(final long datum[], final boolean unsigned){
        if(unsigned) this.dtype = Data.DTYPE_OU;
        else this.dtype = Data.DTYPE_O;
        this.datum = new long[2];
        this.datum[0] = datum[0];
        this.datum[1] = datum[1];
    }

    @Override
    public float getFloat() {
        return this.datum[0];
    }

    @Override
    public int getInt() {
        return (int)this.datum[0];
    }
}