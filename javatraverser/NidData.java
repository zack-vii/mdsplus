// package jTraverser;
public class NidData extends IntData implements NodeId{
    private static final long serialVersionUID = -9003783966234178438L;

    public static Data getData(final int datum) {
        return new NidData(datum);
    }

    public NidData(){
        this.dtype = Data.DTYPE_NID;
    }

    public NidData(final int nid){
        this.dtype = Data.DTYPE_NID;
        this.datum = nid;
    }

    @Override
    public boolean equals(final Object obj) {
        if(!(obj instanceof NidData)) return false;
        return this.datum == ((NidData)obj).datum;
    }

    @Override
    public int getInt() {
        return this.datum;
    }

    public void incrementNid() {
        this.datum++;
    }

    @Override
    public boolean isResolved() {
        return true;
    }
}