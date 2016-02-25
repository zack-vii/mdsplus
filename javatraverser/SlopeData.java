// package jTraverser;
public class SlopeData extends CompoundData{
    private static final long serialVersionUID = 6953847514236487561L;

    public static Data getData() {
        return new SlopeData();
    }

    public SlopeData(){
        this.dtype = Data.DTYPE_SLOPE;
    }

    public SlopeData(final Data slope, final Data begin, final Data end){
        this.dtype = Data.DTYPE_SLOPE;
        this.descs = new Data[3];
        this.descs[0] = slope;
        this.descs[1] = begin;
        this.descs[2] = end;
    }

    public Data getBegin() {
        return this.descs[1];
    }

    public Data getEnd() {
        return this.descs[2];
    }

    public Data getSlope() {
        return this.descs[0];
    }
}
