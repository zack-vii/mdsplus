// package jTraverser;
public class DependencyData extends CompoundData{
    public static final int   DEPENDENCY_AND   = 10;
    public static final int   DEPENDENCY_OR    = 11;
    private static final long serialVersionUID = -1926888273446979979L;

    public static Data getData() {
        return new DependencyData();
    }
    int opcode;

    public DependencyData(){
        this.dtype = Data.DTYPE_DEPENDENCY;
    }

    public DependencyData(final int opcode, final Data arg1, final Data arg2){
        this.dtype = Data.DTYPE_DEPENDENCY;
        this.opcode = opcode;
        this.descs = new Data[2];
        this.descs[0] = arg1;
        this.descs[1] = arg2;
    }

    public final Data[] getArguments() {
        return this.descs;
    }

    public final int getOpcode() {
        return this.opcode;
    }
}