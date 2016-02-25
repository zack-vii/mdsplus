// package jTraverser;
public class SignalData extends CompoundData{
    private static final long serialVersionUID = -7231298030703354245L;

    public static Data getData() {
        return new SignalData();
    }

    public SignalData(){
        this.dtype = Data.DTYPE_SIGNAL;
    }

    public SignalData(final Data data, final Data raw, final Data[] dimensions){
        int ndescs;
        this.dtype = Data.DTYPE_SIGNAL;
        ndescs = 2 + dimensions.length;
        this.descs = new Data[ndescs];
        this.descs[0] = data;
        this.descs[1] = raw;
        if(dimensions != null){
            for(int i = 0; i < dimensions.length; i++)
                this.descs[2 + i] = dimensions[i];
        }
    }

    public final Data getDatum() {
        return this.descs[0];
    }

    public final Data getDimension(final int idx) {
        if(idx > this.descs.length - 2) return null;
        return this.descs[idx + 2];
    }

    public final Data getRaw() {
        return this.descs[1];
    }
}