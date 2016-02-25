// package jTraverser;
public class WithUnitsData extends CompoundData{
    private static final long serialVersionUID = -7312745275969299595L;

    public static Data getData() {
        return new WithUnitsData();
    }

    public WithUnitsData(){
        this.dtype = Data.DTYPE_WITH_UNITS;
    }

    public WithUnitsData(final Data data, final Data units){
        this.dtype = Data.DTYPE_WITH_UNITS;
        this.descs = new Data[2];
        this.descs[0] = data;
        this.descs[1] = units;
    }

    public final Data getDatum() {
        return this.descs[0];
    }

    public final Data getUnits() {
        return this.descs[1];
    }
}