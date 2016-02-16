package jScope;

/* $Id$ */
import java.net.*;
import java.io.*;
import java.util.StringTokenizer;
import javax.swing.JOptionPane;
import javax.swing.JEditorPane;

public class W7XBrowseSignals extends jScopeBrowseSignals
{
    static final long serialVersionUID = 7777777L;
    String path;
    String shot="-1";
    final String tree="W7X";
    final String server_url = "http://archive-webapi.ipp-hgw.mpg.de/ArchiveDB";
    protected String getSignal(String url_name)
    {
        String sig_path;
        String[] parts;
        try
        {
            if(!url_name.startsWith(server_url))
                throw new Exception("Worng server, must be "+server_url);
            parts = url_name.substring(server_url.length()).split("/_signal\\.html\\?",2);
            System.out.println(parts.length+" "+parts[0]);
            sig_path = parts[0];
            if(parts.length==1) return sig_path;
            parts = parts[1].split("&");
            System.out.println(""+parts);
            String from =null,upto =null;
            for (int i=0 ; i<parts.length ; i++)
                if(parts[i].startsWith("from="))
                     from = parts[i].substring(5);
                else if(parts[i].startsWith("upto="))
                     upto = parts[i].substring(5);
            System.out.println(upto+"  " +from);
            if(from!=null && upto!=null)
                shot = "TIME("+from+"Q,"+upto+"Q,0Q);-1";
            System.out.println(sig_path);
        }
        catch (Exception exc){sig_path = null;}
        return sig_path;
    }

    protected String getTree(){return tree;}
    protected String getShot(){return shot;}
    protected String getServerAddr(){return server_url;}

    static private boolean reasonableShotNr( String shot )
    {
        try
        {
            Integer sn = new Integer(shot);
            return true;
        }
        catch(NumberFormatException e)
        {
            return false;
        }
    }
}
