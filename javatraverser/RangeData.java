// package jTraverser;
public class RangeData extends CompoundData{
    private static final long serialVersionUID = -5861852613794463254L;

    public static Data getData() {
        return new RangeData();
    }

    public RangeData(){
        this.dtype = Data.DTYPE_RANGE;
    }

    public RangeData(final Data begin, final Data end, final Data delta){
        this.dtype = Data.DTYPE_RANGE;
        this.descs = new Data[3];
        this.descs[0] = begin;
        this.descs[1] = end;
        this.descs[2] = delta;
    }

    public Data getBegin() {
        return this.descs[0];
    }

    public Data getDelta() {
        return this.descs[2];
    }

    public Data getEnd() {
        return this.descs[1];
    }
}