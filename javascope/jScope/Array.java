package jScope;

import java.awt.Dimension;

public class Array{
    public static final class AllFrames extends ByteArray{
        public Dimension dim;
        public double    times[];

        public AllFrames(final ByteArray byteArray, final int width, final int height, final double times[]){
            super(byteArray.buf, byteArray.dtype);
            this.dim = new Dimension(width, height);
            this.times = times;
        }
    }
    public static class ByteArray{
        public byte buf[];
        byte        dtype;

        public ByteArray(final byte[] buf, final byte dtype){
            this.buf = buf;
            this.dtype = dtype;
        }

        public final int getDataSize() {
            return Descriptor.getDataSize(this.dtype, this.buf);
        }

        public final int getFrameType() {
            if(DEBUG.D) System.out.println(">> getFrameType = " + this.dtype);
            switch(this.dtype){
                case Descriptor.DTYPE_UBYTE:
                case Descriptor.DTYPE_BYTE:
                    return FrameData.BITMAP_IMAGE_8;
                case Descriptor.DTYPE_USHORT:
                case Descriptor.DTYPE_SHORT:
                    return FrameData.BITMAP_IMAGE_16;
                case Descriptor.DTYPE_ULONG:
                case Descriptor.DTYPE_LONG:
                    return FrameData.BITMAP_IMAGE_32;
                case Descriptor.DTYPE_ULONGLONG:
                case Descriptor.DTYPE_LONGLONG:
                case Descriptor.DTYPE_FLOAT:
                case Descriptor.DTYPE_DOUBLE:
                    return FrameData.BITMAP_IMAGE_FLOAT;
                default:
                    return FrameData.BITMAP_IMAGE_8;
            }
        }
    }
    public static final class RealArray{
        private final double[] doubleArray;
        private final float[]  floatArray;
        private final boolean  isDouble;
        public final boolean   isLong;
        private final long[]   longArray;

        public RealArray(final double[] doubleArray){
            if(DEBUG.M) System.out.println("mdsDataProvider.RealArray(" + doubleArray + ")");
            this.doubleArray = doubleArray;
            this.floatArray = null;
            this.longArray = null;
            this.isDouble = true;
            this.isLong = false;
        }

        public RealArray(final float[] floatArray){
            if(DEBUG.M) System.out.println("mdsDataProvider.RealArray(" + floatArray + ")");
            this.doubleArray = null;
            this.floatArray = floatArray;
            this.longArray = null;
            this.isDouble = false;
            this.isLong = false;
        }

        public RealArray(final long[] longArray){
            if(DEBUG.M) System.out.println("mdsDataProvider.RealArray(" + longArray + ")");
            this.doubleArray = null;
            this.floatArray = null;
            for(int i = 0; i < longArray.length; i++)
                longArray[i] = jScopeFacade.convertFromSpecificTime(longArray[i]);
            this.longArray = longArray;
            this.isDouble = false;
            this.isLong = true;
        }

        public final double[] getDoubleArray() {
            if(DEBUG.M) System.out.println("mdsDataProvider.RealArray.getDoubleArray()");
            if(DEBUG.D) System.out.println(">> " + this.isLong + this.isDouble + (this.floatArray != null) + (this.doubleArray != null));
            if(this.isLong) return null;
            if(this.isDouble) return this.doubleArray;
            final double[] doubleArray = new double[this.floatArray.length];
            for(int i = 0; i < this.floatArray.length; i++)
                doubleArray[i] = this.floatArray[i];
            return doubleArray;
        }

        public final float[] getFloatArray() {
            if(DEBUG.M) System.out.println("mdsDataProvider.RealArray.getFloatArray()");
            if(this.isLong) return null;
            if(!this.isDouble) return this.floatArray;
            final float[] floatArray = new float[this.doubleArray.length];
            for(int i = 0; i < this.doubleArray.length; i++)
                floatArray[i] = (float)this.doubleArray[i];
            return floatArray;
        }

        public final long[] getLongArray() {
            if(DEBUG.M) System.out.println("mdsDataProvider.RealArray.getLongArray()");
            return this.longArray;
        }
    }
}