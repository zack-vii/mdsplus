package jScope;

/* $Id$ */
import java.io.IOException;

public class JiDim{
    public int    mStart, mCount, mStride;
    public String mName;

    public JiDim(String name, int start, int count){
        mName = name;
        mStart = start;
        mCount = count;
        mStride = 1;
    }

    public JiDim(String name, int start, int count, int stride){
        mName = name;
        mStart = start;
        mCount = count;
        mStride = stride;
    }

    @Override
    protected Object clone() {
        return new JiDim(mName, mStart, mCount, mStride);
    }

    public JiDim copy() {
        return (JiDim)clone();
    }

    public static JiVar getCoordVar() throws IOException {
        throw new IOException("JiDim::getCoordVar() : not supported");
    }

    public String getName() {
        return mName;
    }

    @Override
    public String toString() {
        return "(" + mName + "," + mStart + "," + mCount + "," + mStride + ")";
    }
}
