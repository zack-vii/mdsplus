// package jTraverser;
public class WindowData extends CompoundData{
    private static final long serialVersionUID = 2634389159311572431L;

    public static Data getData() {
        return new WindowData();
    }

    public WindowData(){
        this.dtype = Data.DTYPE_WINDOW;
    }

    public WindowData(final Data start_idx, final Data end_idx, final Data value_at_0){
        this.dtype = Data.DTYPE_WINDOW;
        this.descs = new Data[3];
        this.descs[0] = start_idx;
        this.descs[1] = end_idx;
        this.descs[2] = value_at_0;
    }

    public final Data getEndIdx() {
        return this.descs[1];
    }

    public final Data getStartIdx() {
        return this.descs[0];
    }

    public final Data getValueAt0() {
        return this.descs[2];
    }
}