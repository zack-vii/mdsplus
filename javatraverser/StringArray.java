// package jTraverser;
public class StringArray extends ArrayData{
    private static final long serialVersionUID = 1915305172807700792L;

    public static Data getData(final String datum[]) {
        return new StringArray(datum);
    }
    String datum[];

    public StringArray(final String datum[]){
        this.length = datum.length;
        this.dtype = Data.DTYPE_T;
        this.datum = new String[datum.length];
        for(int i = 0; i < datum.length; i++)
            this.datum[i] = new String(datum[i]);
    }

    @Override
    public String[] getStringArray() {
        final String ris[] = new String[this.datum.length];
        for(int i = 0; i < this.datum.length; i++)
            ris[i] = new String(this.datum[i]);
        return ris;
    }
}
