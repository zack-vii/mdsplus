// package jTraverser;
public class AtomicData extends Data{
    private static final long serialVersionUID = 94372955317250190L;

    public AtomicData(){
        this.dclass = Data.CLASS_S;
    }

    @Override
    public boolean isAtomic() {
        return true;
    }
}