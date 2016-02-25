// package jTraverser;
public class QuadData extends AtomicData{
    private static final long serialVersionUID = -4487902258248152062L;

    public static Data getData(final long datum, final boolean unsigned) {
        return new QuadData(datum, unsigned);
    }
    long datum;

    public QuadData(final long datum){
        this(datum, false);
    }

    public QuadData(final long datum, final boolean unsigned){
        if(unsigned) this.dtype = Data.DTYPE_QU;
        else this.dtype = Data.DTYPE_Q;
        this.datum = datum;
    }

    @Override
    public float getFloat() {
        return this.datum;
    }

    @Override
    public int getInt() {
        return (int)this.datum;
    }
}