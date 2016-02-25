// package jTraverser;
public class ComplexData extends AtomicData{
    private static final long serialVersionUID = 7209564558300569651L;

    public static Data getData(final double re, final double im, final int flags) {
        return new ComplexData(re, im, flags);
    }
    int    flags = Data.DTYPE_FTC;
    double re, im;

    public ComplexData(final double re, final double im){
        this(re, im, Data.DTYPE_FTC);
    }

    public ComplexData(final double re, final double im, final int flags){
        this.dtype = Data.DTYPE_FTC;
        this.re = re;
        this.im = im;
        this.flags = flags;
    }
}
