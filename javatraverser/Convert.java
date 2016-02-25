import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.StringTokenizer;

class Convert{
    static int getKey(final float data[]) {
        int outHash = 0;
        for(int i = 0; i < 192 * 192; i++){
            final int intVal = Float.floatToIntBits(data[i]);
            outHash = 37 * outHash + intVal;
        }
        return outHash;
    }

    public static void main(final String args[]) {
        if(args.length == 2){
            final Convert conv = new Convert(args[0], args[1]);
            conv.convertMatrix();
        }else if(args.length == 1){
            final Convert conv = new Convert("", args[0] + ".dat");
            final int key = conv.getKey();
            try{
                final BufferedWriter bw = new BufferedWriter(new FileWriter(args[0] + ".key"));
                bw.write("" + key);
                bw.close();
            }catch(final Exception exc){
                System.err.println(exc);
            }
        }
    }
    float  data[];
    String path, fileName;
    int    shotNum = -1;

    public Convert(final String path, final String fileName){
        this.path = path;
        this.fileName = fileName;
        this.loadData();
    }

    public Convert(final String path, final String fileName, final int shotNum){
        this.path = path;
        this.fileName = fileName;
        this.shotNum = shotNum;
        this.loadData();
    }

    public void convertMatrix() {
        System.out.println(this.path);
        final Database rfx = new Database("rfx", this.shotNum);
        try{
            rfx.open();
            final NidData nid = rfx.resolve(new PathData(this.path), 0);
            final FloatArray array = new FloatArray(this.data);
            rfx.putData(nid, array, 0);
            rfx.close(0);
        }catch(final Exception exc){
            System.err.println(exc);
        }
    }

    public void convertMatrix(final Database db) {
        System.out.println(this.path);
        try{
            final NidData nid = db.resolve(new PathData(this.path), 0);
            final FloatArray array = new FloatArray(this.data);
            db.putData(nid, array, 0);
        }catch(final Exception exc){
            System.err.println(exc);
        }
    }

    int getKey() {
        return Convert.getKey(this.data);
    }

    void loadData() {
        this.data = new float[192 * 192];
        if(this.fileName.equalsIgnoreCase("diagonal")){
            for(int i = 0; i < 192; i++){
                for(int j = 0; j < 192; j++){
                    if(i == j) this.data[192 * i + j] = 1;
                    else this.data[192 * i + j] = 0;
                }
            }
        }else{
            try{
                final BufferedReader br = new BufferedReader(new FileReader(this.fileName));
                for(int i = 0; i < 192; i++){
                    final String line = br.readLine();
                    final StringTokenizer st = new StringTokenizer(line, "[], ");
                    for(int j = 0; j < 192; j++)
                        this.data[192 * i + j] = Float.parseFloat(st.nextToken());
                }
                br.close();
            }catch(final Exception exc){
                System.err.println(exc);
            }
        }
    }
}
