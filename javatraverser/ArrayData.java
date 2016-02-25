// package jTraverser;
public class ArrayData extends Data{
    private static final long serialVersionUID = -5427351977598520816L;
    int                       length;

    public ArrayData(){
        this.dclass = Data.CLASS_A;
    }

    @Override
    public boolean isAtomic() {
        return false;
    }
}