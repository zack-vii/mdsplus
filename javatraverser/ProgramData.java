// package jTraverser;
public class ProgramData extends CompoundData{
    private static final long serialVersionUID = -326661164737875457L;

    public static Data getData() {
        return new ProgramData();
    }

    public ProgramData(){
        this.dtype = Data.DTYPE_PROGRAM;
    }

    public ProgramData(final Data time_out, final Data program){
        this.dtype = Data.DTYPE_PROGRAM;
        this.descs = new Data[2];
        this.descs[0] = time_out;
        this.descs[1] = program;
    }

    public final Data getProgram() {
        return this.descs[1];
    }

    public final Data getTimeout() {
        return this.descs[0];
    }
}