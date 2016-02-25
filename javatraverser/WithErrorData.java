// package jTraverser;
public class WithErrorData extends CompoundData{
    private static final long serialVersionUID = 586505059063293330L;

    public static Data getData() {
        return new WithErrorData();
    }

    public WithErrorData(){
        this.dtype = Data.DTYPE_WITH_ERROR;
    }

    public WithErrorData(final Data data, final Data error){
        this.dtype = Data.DTYPE_WITH_ERROR;
        this.descs = new Data[2];
        this.descs[0] = data;
        this.descs[1] = error;
    }

    public final Data getDatum() {
        return this.descs[0];
    }

    public final Data getErrror() {
        return this.descs[1];
    }
}