package jScope;

/* $Id$ */
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Insert the type's description here.
 * Creation date: (12/10/99 18:49:35)
 */
public class RandomAccessData{
    /**
     * Insert the method's description here.
     * Creation date: (15/10/99 17:17:05)
     *
     * @return java.lang.String
     */
    public static String readString() {
        System.out.println("readString not supported");
        return null;
    }
    byte buffer[];
    int  position = 0;

    /**
     * RandomAccessData constructor comment.
     */
    public RandomAccessData(){
        super();
    }

    public RandomAccessData(final byte[] buffer) throws IOException{
        super();
        this.buffer = buffer;
    }

    public RandomAccessData(final RandomAccessFile file) throws IOException{
        super();
        final int len = (int)file.length();
        this.buffer = new byte[len];
        file.read(this.buffer);
    }

    /**
     * Insert the method's description here.
     * Creation date: (15/10/99 17:15:27)
     *
     * @return long
     */
    public long getFilePointer() {
        return this.position;
    }

    /**
     * Insert the method's description here.
     * Creation date: (15/10/99 17:13:31)
     *
     * @param size
     *            int
     */
    public void readFully(final byte data[]) {
        for(int i = 0; i < data.length; i++)
            data[i] = this.buffer[this.position + i];
        this.position += data.length;
    }

    /**
     * Insert the method's description here.
     * Creation date: (12/10/99 19:05:42)
     */
    int readInt() throws java.io.IOException {
        int x;
        final int pos = this.position;
        x = (short)(this.buffer[pos] & 0xFF);
        x <<= 8;
        x |= (short)(this.buffer[pos + 1] & 0xFF);
        x <<= 8;
        x |= (short)(this.buffer[pos + 2] & 0xFF);
        x <<= 8;
        x |= (short)(this.buffer[pos + 3] & 0xFF);
        this.position += 4;
        return x;
    }

    /**
     * Insert the method's description here.
     * Creation date: (12/10/99 19:02:37)
     *
     * @exception java.io.IOException
     *                The exception description.
     */
    short readShort() throws java.io.IOException {
        short x;
        final int pos = this.position;
        x = (short)(this.buffer[pos] & 0xFF);
        x <<= 8;
        x |= (short)(this.buffer[pos + 1] & 0xFF);
        this.position += 2;
        return x;
    }

    /**
     * Insert the method's description here.
     * Creation date: (12/10/99 19:00:27)
     *
     * @exception java.io.IOException
     *                The exception description.
     */
    public void seek(final long pos) throws java.io.IOException {
        this.position = (int)pos;
    }

    /**
     * Insert the method's description here.
     * Creation date: (15/10/99 17:16:08)
     *
     * @param amount
     *            int
     */
    public void skipBytes(final int amount) {
        this.position += amount;
    }
}