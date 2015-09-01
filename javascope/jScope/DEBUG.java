package jScope;
public final class DEBUG
{
    //set to false to allow compiler to identify and eliminate
    //unreachable code
    public static final int LV = 0;
    public static final boolean ON = LV>0;
    public static void printByteArray(byte[] buf, int byteblock, int width, int height, int pages)
    {
        int i = 0;
        String s;
        System.out.println("---page break---");
        for(int f = 0; f < pages; f++)
        {
            for(int h = 0; h < height; h++)
            {
                s = ", ";
                for(int w = 0; w < width; w++)
                {   
                    for(int b = 0; b < byteblock && i<0x10000; b++)
                    {
                       StringBuilder sb = new StringBuilder();
                       sb.append(Integer.toHexString( 0xFF & buf[i]));
                       if (sb.length() < 2)
                           sb.insert(0, '0');
                       s += sb.toString()+" ";
                       i++;
                    }
                    s += ", ";
                }
                System.out.println(s);
            }
            if (i==0x10000)
                System.out.println(" ... "+i+"/"+buf.length);
            System.out.println("---page break---");
        }
    }
    public static void printIntArray(int[] buf, int byteblock, int width, int height, int pages)
    {
        int i = 0;
        String s;
        System.out.println("---page break---");
        for(int f = 0; f < pages; f++)
        {
            for(int h = 0; h < height; h++)
            {
                s = ", ";
                for(int w = 0; w < width; w++)
                {   
                    for(int b = 0; b < byteblock && i<0x10000; b++)
                    {
                       StringBuilder sb = new StringBuilder();
                       sb.append(Integer.toHexString( 0xFFFFFFFF & buf[i]));
                       if (sb.length() < 8)
                           sb.insert(0, '0');
                       s += sb.toString()+" ";
                       i++;
                    }
                    s += ", ";
                }
                System.out.println(s);
            }
            if (i==0x10000)
                System.out.println(" ... "+i+"/"+buf.length);
            System.out.println("---page break---");
        }
    }
}