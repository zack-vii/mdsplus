// package jTraverser;
public class DimensionData extends CompoundData{
    private static final long serialVersionUID = -8264452799549457840L;

    public static Data getData() {
        return new DimensionData();
    }

    public DimensionData(){
        this.dtype = Data.DTYPE_DIMENSION;
    }

    public DimensionData(final Data window, final Data axis){
        this.dtype = Data.DTYPE_DIMENSION;
        this.descs = new Data[2];
        this.descs[0] = window;
        this.descs[1] = axis;
    }

    public final Data getAxis() {
        return this.descs[1];
    }

    public final Data getWindow() {
        return this.descs[0];
    }
}