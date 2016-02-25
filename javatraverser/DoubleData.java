// package jTraverser;
public class DoubleData extends AtomicData{
    private static final long serialVersionUID = -2253299312256543857L;

    public static Data getData(final double datum, final int flags) {
        return new DoubleData(datum, flags);
    }
    double datum;
    int    flags = Data.DTYPE_DOUBLE;

    public DoubleData(final double datum){
        this(datum, Data.DTYPE_DOUBLE);
    }

    public DoubleData(final double datum, final int flags){
        this.dtype = Data.DTYPE_DOUBLE;
        this.datum = datum;
        this.flags = flags;
    }

    @Override
    public double getDouble() {
        return this.datum;
    }

    public int getFlags() {
        return this.flags;
    }

    @Override
    public float getFloat() {
        return (float)this.datum;
    }

    @Override
    public int getInt() {
        return (int)this.datum;
    }
}
