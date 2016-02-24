package jScope;

import java.io.IOException;

public class test{
    private static final MdsDataProvider mds = new MdsDataProvider();

    public static void main(final String[] args) {
        try{
            test.mds.mds.setProvider("mds-data-1");
            test.mds.CheckOpen("W7X", 160223021);
            final String in_y = "QSR.HARDWARE:CYGNET4K_40B:FRAMES";
            final int numSegments = test.mds.mds.MdsValue("[GetNumSegments(" + in_y + ")]").getInt();
            System.out.println("" + numSegments);
            final int nid = test.mds.mds.MdsValue("getnci(" + in_y + ",'NID_NUMBER')").getInt();
            System.out.println("" + nid);
            double[] limits = test.mds.GetDoubleArray("GetLimits(" + in_y + ")");
            System.out.println("" + limits.length);
            limits = test.mds.GetDoubleArray("STATEMENT(_N=GETNCI(" + in_y + ",'NID_NUMBER'),_L=[],FOR(_I=0,_I<20,_I++,STATEMENT(_S=0,_E=0,_R=TreeShr->TreeGetSegmentLimits(VAL(_N),VAL(_I),XD(_S),XD(_E)),IF(_R&1,_L=[_L,_S,_E],_L=[_L,$ROPRAND,$ROPRAND]))),_L)");
            System.out.println("" + (limits == null ? null : limits.length));
        }catch(final IOException e){
            e.printStackTrace();
        }
    }
}
