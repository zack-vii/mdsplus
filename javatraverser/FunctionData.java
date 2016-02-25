// package jTraverser;
public class FunctionData extends CompoundData{
    private static final long serialVersionUID = -1553027904365570332L;

    public static Data getData() {
        return new FunctionData();
    }

    public FunctionData(){
        this.dtype = Data.DTYPE_FUNCTION;
    }

    public FunctionData(final int opcode, final Data[] args){
        this.dtype = Data.DTYPE_FUNCTION;
        this.opcode = opcode;
        if(args == null){ return; }
        this.descs = new Data[args.length];
        for(int i = 0; i < args.length; i++)
            this.descs[i] = args[i];
    }

    public final Data[] getArgs() {
        return this.descs;
    }

    public final int getOpcode() {
        return this.opcode;
    }
}