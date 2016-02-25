// package jTraverser;
public class PathData extends StringData implements NodeId{
    private static final long serialVersionUID = -3352537836104568439L;

    public static Data getData(final String datum) {
        return new PathData(datum);
    }

    public PathData(final String path){
        this.datum = path;
        this.dtype = Data.DTYPE_PATH;
    }

    @Override
    public boolean isResolved() {
        return false;
    }
}