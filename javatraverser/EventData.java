// package jTraverser;
public class EventData extends StringData{
    private static final long serialVersionUID = 5967241343179988753L;

    public static Data getData(final String datum) {
        return new EventData(datum);
    }

    public EventData(final String path){
        this.datum = path;
        this.dtype = Data.DTYPE_EVENT;
    }
}
