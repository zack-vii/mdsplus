// package jTraverser;
public class RoutineData extends CompoundData{
    private static final long serialVersionUID = -1501809391297659266L;

    public static Data getData() {
        return new RoutineData();
    }

    public RoutineData(){
        this.dtype = Data.DTYPE_ROUTINE;
    }

    public RoutineData(final Data time_out, final Data image, final Data routine, final Data[] arguments){
        int ndescs;
        this.dtype = Data.DTYPE_ROUTINE;
        if(arguments != null) ndescs = 3 + arguments.length;
        else ndescs = 3;
        this.descs = new Data[ndescs];
        this.descs[0] = time_out;
        this.descs[1] = image;
        this.descs[2] = routine;
        if(arguments == null) return;
        for(int i = 3; i < ndescs; i++)
            this.descs[i] = arguments[i - 3];
    }

    public final Data[] getArguments() {
        final Data ris[] = new Data[this.descs.length - 3];
        for(int i = 0; i < this.descs.length - 3; i++)
            ris[i] = this.descs[3 + i];
        return ris;
    }

    public final Data getImage() {
        return this.descs[1];
    }

    public final Data getRoutine() {
        return this.descs[2];
    }

    public final Data getTimeout() {
        return this.descs[0];
    }
}