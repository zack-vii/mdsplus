// package jTraverser;
public class ByteData extends AtomicData{
    private static final long serialVersionUID = 3995773544994029942L;

    public static Data getData(final byte datum, final boolean unsigned) {
        return new ByteData(datum, unsigned);
    }
    byte datum;

    public ByteData(final byte datum){
        this(datum, false);
    }

    public ByteData(final byte datum, final boolean unsigned){
        if(unsigned) this.dtype = Data.DTYPE_BU;
        else this.dtype = Data.DTYPE_B;
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