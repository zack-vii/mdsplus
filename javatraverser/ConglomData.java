// package jTraverser;
public class ConglomData extends CompoundData{
    private static final long serialVersionUID = -5947433390171454862L;

    public static Data getData() {
        return new ConglomData();
    }

    public ConglomData(){
        this.dtype = Data.DTYPE_CONGLOM;
    }

    public ConglomData(final Data image, final Data model, final Data name, final Data qualifiers){
        this.dtype = Data.DTYPE_CONGLOM;
        this.descs = new Data[4];
        this.descs[0] = image;
        this.descs[1] = model;
        this.descs[2] = name;
        this.descs[3] = qualifiers;
    }

    public final Data getImage() {
        return this.descs[0];
    }

    public final Data getModel() {
        return this.descs[1];
    }

    public final Data getName() {
        return this.descs[2];
    }

    public final Data getQualifiers() {
        return this.descs[3];
    }
}