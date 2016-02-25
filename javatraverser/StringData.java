// package jTraverser;
public class StringData extends AtomicData{
    private static final long serialVersionUID = -3077927844127022199L;

    public static Data getData(final String datum) {
        return new StringData(datum);
    }
    String datum;

    public StringData(){}

    public StringData(final String datum){
        this.dtype = Data.DTYPE_T;
        this.datum = datum;
    }

    @Override
    public String getString() {
        return this.datum;
    }
}