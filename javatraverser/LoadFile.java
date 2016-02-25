// Stores a file as a bonary array into a pulse file
// Arguments:
// 1) File name
// 2) Esperiment
// 3) shot
// 4) Node name
import java.io.RandomAccessFile;

public class LoadFile{
    public static void main(final String args[]) {
        if(args.length < 3 || args.length > 5){
            System.err.println("Usage: java StoreFile <filename> <nodename> <experiment> [< shot> ");
            System.exit(0);
        }
        final String fileName = args[0];
        final String nodeName = args[1];
        final String experiment = args[2];
        int shot = -1;
        if(args.length == 4){
            try{
                shot = Integer.parseInt(args[3]);
            }catch(final Exception exc){
                System.err.println("Invalid shot number");
                System.exit(0);
            }
        }
        final Database tree = new Database(experiment, shot);
        try{
            tree.open();
        }catch(final Exception exc){
            System.err.println("Cannot open experiment " + experiment + " shot " + shot + ": " + exc);
            System.exit(0);
        }
        NidData nid = null;
        try{
            nid = tree.resolve(new PathData(nodeName), 0);
        }catch(final Exception exc){
            System.err.println("Cannot find node " + nodeName);
            System.exit(0);
        }
        byte[] serialized = null;
        try{
            final ByteArray ba = (ByteArray)tree.getData(nid, 0);
            serialized = ba.getByteArray();
        }catch(final Exception exc){
            System.err.println("Error reading data in" + nodeName + ": " + exc);
            System.exit(0);
        }
        try{
            final RandomAccessFile raf = new RandomAccessFile(fileName, "rw");
            raf.write(serialized);
            raf.close();
        }catch(final Exception exc){
            System.err.println("Cannot read file " + fileName + ": " + exc);
            System.exit(0);
        }
    }
}
