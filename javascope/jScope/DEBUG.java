package jScope;
public final class DEBUG
{
    //set to false to allow compiler to identify and eliminate
    //unreachable code
    public static final boolean ON = true;
    public static void printByteArray(byte[] buf, int byteblock, int width, int height, int pages)
    {
        int i = 0;
        String s;
        System.out.println(" ------------------------------------------------------- ");
        for(int f = 0; f < pages; f++)
        {
            for(int h = 0; h < height; h++)
            {
                s = ", ";
                for(int w = 0; w < width; w++)
                {   
                    for(int b = 0; b < byteblock; b++)
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
            System.out.println(" ------------------------------------------------------- ");
        }
    }
}