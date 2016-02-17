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
    String shot="0";
    final String tree=null;
    final String server_url = "http://archive-webapi.ipp-hgw.mpg.de";

    protected String getSignal(String url_name)
    {
        String sig_path;
        String[] parts;
        try{
            shot = "0";
            if(!url_name.startsWith(server_url))
                throw new Exception("Wrong server, must be "+server_url);
            parts = url_name.substring(server_url.length()).split("(_signal\\.html)?\\?",2);
            sig_path = getpath(parts[0]);
            if(parts.length>1)
                getshot(parts[1]);
            System.out.println(sig_path);
        }catch (Exception exc){System.err.println("Error "+exc);sig_path = null;}
        return sig_path;
    }

    private String getpath(String path)
    {
        final String DS = "_DATASTREAM/";
        String[] parts = path.split(DS,2);
        path = parts[0] + DS;
        parts = parts[1].split("/");
        path+= parts[0];
        if(parts[parts.length-1].startsWith("scaled"))
            path+="/"+parts[parts.length-1];
        return path;
    }

    private void getshot(String param)
    {
        String [] parts = param.split("&");
        String from =null,upto =null;
        for (int i=0 ; i<parts.length ; i++)
            if(parts[i].startsWith("from="))
                    from = parts[i].substring(5);
            else if(parts[i].startsWith("upto="))
                    upto = parts[i].substring(5);
        if(from!=null && upto!=null)
            shot = "TIME("+from+"Q,"+upto+"Q,0Q);-1";
        else
            shot = "0";
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
