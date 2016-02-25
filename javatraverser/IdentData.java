// package jTraverser;
public class IdentData extends StringData{
    private static final long serialVersionUID = 2603439338345489646L;

    public static Data getData(final String datum) {
        return new IdentData(datum);
    }

    public IdentData(){}

    public IdentData(final String datum){
        super(datum);
        this.dtype = Data.DTYPE_IDENT;
    }

    @Override
    public String getString() {
        return this.datum;
    }
}