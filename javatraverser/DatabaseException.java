// package jTraverser;
import java.util.StringTokenizer;

class DatabaseException extends Exception{
    private static final long serialVersionUID = -1977560342812497985L;

    static String getMsg(final String message) {
        final StringTokenizer st = new StringTokenizer(message, ":");
        String statusStr = "";
        if(st.countTokens() > 0) statusStr = st.nextToken();
        return message.substring(statusStr.length() + 1);
    }
    int status = 0;

    public DatabaseException(final String message){
        super(DatabaseException.getMsg(message));
        this.setStatus(message);
    }

    public int getStatus() {
        return this.status;
    }

    private void setStatus(final String message) {
        try{
            final StringTokenizer st = new StringTokenizer(message, ":");
            this.status = Integer.parseInt(st.nextToken());
        }catch(final Exception exc){
            this.status = 0;
        }
    }
}