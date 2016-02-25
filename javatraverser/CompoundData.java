// package jTraverser;
public class CompoundData extends Data{
    private static final long serialVersionUID = 4683222067694494115L;
    Data[]                    descs;
    int                       opcode           = 0;

    public CompoundData(){
        this.dclass = Data.CLASS_R;
    }

    public CompoundData(final Data descs[]){
        this.descs = descs;
    }

    public Data[] getDescs() {
        return this.descs;
    }

    @Override
    public boolean isAtomic() {
        return false;
    }
}