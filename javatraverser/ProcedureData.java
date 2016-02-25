// package jTraverser;
public class ProcedureData extends CompoundData{
    private static final long serialVersionUID = 149721516346147126L;

    public static Data getData() {
        return new ProcedureData();
    }

    public ProcedureData(){
        this.dtype = Data.DTYPE_PROCEDURE;
    }

    public ProcedureData(final Data timeout, final Data language, final Data procedure, final Data[] arguments){
        int ndescs;
        this.dtype = Data.DTYPE_PROCEDURE;
        if(arguments != null) ndescs = 3 + arguments.length;
        else ndescs = 3;
        this.descs = new Data[ndescs];
        this.descs[0] = timeout;
        this.descs[1] = language;
        this.descs[2] = procedure;
        if(arguments == null) return;
        for(int i = 3; i < ndescs; i++)
            this.descs[i] = arguments[i - 3];
    }

    public final Data[] getArguments() {
        final Data[] ris = new Data[this.descs.length - 3];
        for(int i = 0; i < this.descs.length - 3; i++)
            ris[i] = this.descs[3 + i];
        return ris;
    }

    public final Data getLanguage() {
        return this.descs[1];
    }

    public final Data getProcedure() {
        return this.descs[2];
    }

    public final Data getTimeout() {
        return this.descs[0];
    }
}
