// package jTraverser;
public class ParameterData extends CompoundData{
    private static final long serialVersionUID = 2850334635393236919L;

    public static Data getData() {
        return new ParameterData();
    }

    public ParameterData(){
        this.dtype = Data.DTYPE_PARAM;
    }

    public ParameterData(final Data data, final Data help, final Data validation){
        this.dtype = Data.DTYPE_PARAM;
        this.descs = new Data[3];
        this.descs[0] = data;
        this.descs[1] = help;
        this.descs[2] = validation;
    }

    public final Data getDatum() {
        return this.descs[0];
    }

    public final Data getHelp() {
        return this.descs[1];
    }

    public final Data getValidation() {
        return this.descs[2];
    }
}