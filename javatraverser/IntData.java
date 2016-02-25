// package jTraverser;
public class IntData extends AtomicData{
    private static final long serialVersionUID = 8712412969011107029L;

    public static Data getData(final int datum, final boolean unsigned) {
        return new IntData(datum, unsigned);
    }
    int datum;

    public IntData(){}

    public IntData(final int datum){
        this(datum, false);
    }

    public IntData(final int datum, final boolean unsigned){
        if(unsigned) this.dtype = Data.DTYPE_LU;
        else this.dtype = Data.DTYPE_L;
        this.datum = datum;
    }

    @Override
    public float getFloat() {
        return this.datum;
    }

    @Override
    public int getInt() {
        return this.datum;
    }
}