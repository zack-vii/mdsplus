// package jTraverser;
public class FloatData extends AtomicData{
    private static final long serialVersionUID = -7493576298930196817L;

    public static Data getData(final float datum, final int flags) {
        return new FloatData(datum, flags);
    }
    float datum;
    int   flags = Data.DTYPE_FLOAT;

    public FloatData(final float datum){
        this(datum, Data.DTYPE_FLOAT);
    }

    public FloatData(final float datum, final int flags){
        this.dtype = Data.DTYPE_FLOAT;
        this.datum = datum;
        this.flags = flags;
    }

    public int getFlags() {
        return this.flags;
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