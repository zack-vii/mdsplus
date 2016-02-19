package jScope;

/* $Id$ */
public class W7XBrowseSignals extends jScopeBrowseSignals{
    static final long serialVersionUID = 7777777L;

    private static String getpath(String path) {
        final String DS = "_DATASTREAM/";
        String[] parts = path.split(DS, 2);
        path = parts[0] + DS;
        parts = parts[1].split("/");
        path += parts[0];
        if(parts[parts.length - 1].startsWith("scaled")) path += "/" + parts[parts.length - 1];
        return path;
    }
    String       path;
    final String server_url = "http://archive-webapi.ipp-hgw.mpg.de";
    String       shot       = "0";
    final String tree       = null;

    @Override
    protected String getServerAddr() {
        return this.server_url;
    }

    /*
        static private boolean reasonableShotNr(String shot) {
            try{
                Integer sn = new Integer(shot);
                return true;
            }catch(NumberFormatException e){
                return false;
            }
        }
     */
    private void getshot(final String param) {
        final String[] parts = param.split("&");
        String from = null, upto = null;
        for(final String part : parts)
            if(part.startsWith("from=")) from = part.substring(5);
            else if(part.startsWith("upto=")) upto = part.substring(5);
        if(from != null && upto != null) this.shot = "TIME(" + from + "Q," + upto + "Q,0Q);-1";
        else this.shot = "0";
    }

    @Override
    protected String getShot() {
        return this.shot;
    }

    @Override
    protected String getSignal(final String url_name) {
        String sig_path;
        String[] parts;
        try{
            this.shot = "0";
            if(!url_name.startsWith(this.server_url)) throw new Exception("Wrong server, must be " + this.server_url);
            parts = url_name.substring(this.server_url.length()).split("(_signal\\.html)?\\?", 2);
            sig_path = W7XBrowseSignals.getpath(parts[0]);
            if(parts.length > 1) this.getshot(parts[1]);
            System.out.println(sig_path);
        }catch(final Exception exc){
            System.err.println("Error " + exc);
            sig_path = null;
        }
        return sig_path;
    }

    @Override
    protected String getTree() {
        return this.tree;
    }
}
