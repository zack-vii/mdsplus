// package jTraverser;
public class ShortData extends AtomicData{
    private static final long serialVersionUID = 4653758297712883918L;

    public static Data getData(final short datum, final boolean unsigned) {
        return new ShortData(datum, unsigned);
    }
    short datum;

    public ShortData(final short datum){
        this(datum, false);
    }

    public ShortData(final short datum, final boolean unsigned){
        if(unsigned) this.dtype = Data.DTYPE_WU;
        else this.dtype = Data.DTYPE_W;
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