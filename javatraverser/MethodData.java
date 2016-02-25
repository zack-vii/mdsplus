// package jTraverser;
public class MethodData extends CompoundData{
    private static final long serialVersionUID = 8248608896414175L;

    public static Data getData() {
        return new MethodData();
    }

    public MethodData(){
        this.dtype = Data.DTYPE_METHOD;
    }

    public MethodData(final Data timeout, final Data method, final Data object, final Data[] arguments){
        int ndescs;
        this.dtype = Data.DTYPE_METHOD;
        if(arguments != null) ndescs = 3 + arguments.length;
        else ndescs = 3;
        this.descs = new Data[ndescs];
        this.descs[0] = timeout;
        this.descs[1] = method;
        this.descs[2] = object;
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

    public final Data getDevice() {
        return this.descs[2];
    }

    public final Data getMethod() {
        return this.descs[1];
    }

    public final Data getTimeout() {
        return this.descs[0];
    }
}