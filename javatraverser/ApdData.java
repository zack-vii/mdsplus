// package jTraverser;
public class ApdData extends Data{
    private static final long serialVersionUID = 6271024783936646287L;

    public static Data getData(final Data descs[]) {
        return new ApdData(descs);
    }
    Data[] descs;

    public ApdData(){
        this(null);
    }

    public ApdData(final Data descs[]){
        this.dclass = Data.CLASS_APD;
        this.descs = descs;
    }

    @Override
    public boolean isAtomic() {
        return false;
    }
}