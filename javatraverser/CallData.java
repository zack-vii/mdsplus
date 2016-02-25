// package jTraverser;
public class CallData extends CompoundData{
    private static final long serialVersionUID = 5328836366819599964L;

    public static Data getData() {
        return new CallData();
    }
    int type;

    public CallData(){
        this.dtype = Data.DTYPE_CALL;
    }

    public CallData(final int type, final Data image, final Data routine, final Data[] arguments){
        int ndescs;
        this.dtype = Data.DTYPE_CALL;
        this.type = type;
        if(arguments == null) ndescs = 2;
        else ndescs = 2 + arguments.length;
        this.descs = new Data[ndescs];
        this.descs[0] = image;
        this.descs[1] = routine;
        if(arguments == null) return;
        for(int i = 2; i < ndescs; i++)
            this.descs[i] = arguments[i - 2];
    }

    public final Data[] getArguments() {
        final Data ris[] = new Data[this.descs.length - 2];
        for(int i = 0; i < this.descs.length - 2; i++)
            ris[i] = this.descs[i + 2];
        return ris;
    }

    public final Data getImage() {
        return this.descs[0];
    }

    public final Data getRoutine() {
        return this.descs[1];
    }

    public final int getType() {
        return this.type;
    }
}