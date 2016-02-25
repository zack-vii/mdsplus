/**
 * Load Configuration from a pulse file. It reads from file LoadPulse.conf. Each line
 * of the file is interpteted as a node freference, and the whole subtree will be read and stored
 * (as decompiled). The read values are then written into the model.
 * +
 */
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

public class LoadPulse{
    static class NodeDescriptor{
        String  decompiled;
        boolean on, parentOn, noWriteModel;
        String  path;

        NodeDescriptor(final String path, final String decompiled, final boolean on, final boolean parentOn, final boolean noWriteModel){
            this.path = path;
            this.decompiled = decompiled;
            this.on = on;
            this.parentOn = parentOn;
            this.noWriteModel = noWriteModel;
        }

        String getDecompiled() {
            return this.decompiled;
        }

        String getPath() {
            return this.path;
        }

        boolean isNoWriteModel() {
            return this.noWriteModel;
        }

        boolean isOn() {
            return this.on;
        }

        boolean isParentOn() {
            return this.parentOn;
        }
    }
    static final String confFileName = "LoadPulse.conf";

    public static void main(final String args[]) {
        int outShot = -1;
        if(args.length < 2){
            System.out.println("Usage: java LoadPulse <experiment> <input shot> [<output shot>]");
            System.exit(0);
        }
        if(args.length >= 2){
            try{
                outShot = Integer.parseInt(args[2]);
            }catch(final Exception exc){
                outShot = -1;
            }
            final int shot = Integer.parseInt(args[1]);
            final LoadPulse lp = new LoadPulse();
            try{
                lp.load(args[0], shot, outShot);
            }catch(final Exception exc){
                System.out.println(exc);
            }
        }
    }
    int      numPMUnits   = 0;
    String   PCconnection = "";
    String   PVconnection = "";
    float    rTransfer    = 0;
    Database tree;

    int countPMUnits() {
        try{
            final NidData rootNid = this.tree.resolve(new PathData("\\PM_SETUP"), 0);
            final NidData unitsNid = new NidData(rootNid.getInt() + 5);
            final Data unitsData = this.tree.evaluateData(unitsNid, 0);
            final String units = unitsData.toString();
            final StringTokenizer st = new StringTokenizer(units, " ,\"");
            return st.countTokens();
        }catch(final Exception exc){
            System.out.println("Error getting num enabled PM: " + exc);
            return 0;
        }
    }

    void evaluatePCConnection() {
        try{
            final NidData rootNid = this.tree.resolve(new PathData("\\PC_SETUP"), 0);
            final NidData connectionNid = new NidData(rootNid.getInt() + 2);
            final Data connectionData = this.tree.evaluateData(connectionNid, 0);
            this.PCconnection = connectionData.getString();
        }catch(final Exception exc){
            System.out.println("Error getting PC connection: " + exc);
        }
    }

    void evaluatePVConnection() {
        try{
            final NidData rootNid = this.tree.resolve(new PathData("\\PV_SETUP"), 0);
            final NidData connectionNid = new NidData(rootNid.getInt() + 2);
            final Data connectionData = this.tree.evaluateData(connectionNid, 0);
            this.PVconnection = connectionData.getString();
        }catch(final Exception exc){
            System.out.println("Error getting PV connection: " + exc);
        }
    }

    void evaluateRTransfer() {
        try{
            final NidData rootNid = this.tree.resolve(new PathData("\\P_CONFIG"), 0);
            final NidData rTransferNid = new NidData(rootNid.getInt() + 20);
            final Data rTransferData = this.tree.evaluateData(rTransferNid, 0);
            this.rTransfer = rTransferData.getFloat();
        }catch(final Exception exc){
            System.out.println("Error getting R transfer: " + exc);
        }
    }

    Vector<NodeDescriptor> getNodes(final String experiment, final int shot) throws Exception {
        final Vector<NodeDescriptor> nodesV = new Vector<NodeDescriptor>();
        this.tree = new Database(experiment, shot);
        this.tree.open();
        final BufferedReader br = new BufferedReader(new FileReader(LoadPulse.confFileName));
        String basePathLine;
        String currPath = "";
        String outPath = null;
        final NidData defNid = this.tree.getDefault(0);
        while((basePathLine = br.readLine()) != null){
            NidData currNid;
            if(basePathLine.trim().equals("")) continue;
            System.out.println(basePathLine);
            String basePath = "";
            try{
                final StringTokenizer st = new StringTokenizer(basePathLine, " ");
                basePath = st.nextToken();
                currNid = this.tree.resolve(new PathData(basePath), 0);
                outPath = null;
                if(st.hasMoreTokens()){
                    final String next = st.nextToken();
                    if(next.toUpperCase().equals("STATE")) // If only state has to retrieved
                    {
                        final NodeInfo currInfo = this.tree.getInfo(currNid, 0);
                        currPath = currInfo.getFullPath();
                        try{
                            nodesV.addElement(new NodeDescriptor(currPath, null, currInfo.isOn(), currInfo.isParentOn(), currInfo.isNoWriteModel() || currInfo.isWriteOnce()));
                        }catch(final Exception exc){
                            System.out.println("Error reading state of " + currPath + ": " + exc);
                        }
                        continue;
                    }else // An alternate out pathname is provided in next
                    {
                        outPath = next.toUpperCase();
                    }
                }
                this.tree.setDefault(currNid, 0);
                NidData[] nidsNumeric = this.tree.getWild(NodeInfo.USAGE_NUMERIC, 0);
                if(nidsNumeric == null) nidsNumeric = new NidData[0];
                NidData[] nidsText = this.tree.getWild(NodeInfo.USAGE_TEXT, 0);
                if(nidsText == null) nidsText = new NidData[0];
                NidData[] nidsSignal = this.tree.getWild(NodeInfo.USAGE_SIGNAL, 0);
                if(nidsSignal == null) nidsSignal = new NidData[0];
                NidData[] nidsStruct = this.tree.getWild(NodeInfo.USAGE_STRUCTURE, 0);
                if(nidsStruct == null) nidsStruct = new NidData[0];
                //// Get also data from subtree root
                int addedLen;
                try{
                    this.tree.getData(currNid, 0);
                    addedLen = 1;
                }catch(final Exception exc){
                    addedLen = 0;
                }
                final NidData nids[] = new NidData[nidsNumeric.length + nidsText.length + nidsSignal.length + nidsStruct.length + addedLen];
                if(addedLen > 0) nids[nidsNumeric.length + nidsText.length + nidsSignal.length + nidsStruct.length] = currNid;
                ///////////////////////
                int j = 0;
                for(final NidData element : nidsNumeric)
                    nids[j++] = element;
                for(final NidData element : nidsText)
                    nids[j++] = element;
                for(final NidData element : nidsSignal)
                    nids[j++] = element;
                for(final NidData element : nidsStruct)
                    nids[j++] = element;
                this.tree.setDefault(defNid, 0);
                for(int i = 0; i < nids.length; i++){
                    final NodeInfo currInfo = this.tree.getInfo(nids[i], 0);
                    currPath = currInfo.getFullPath();
                    if(i == (nids.length - 1))// If IT IS the node described in LoadPulse.congf (and not any descendant)
                    {
                        if(outPath != null){
                            System.out.println(currPath + " --> " + outPath);
                            currPath = outPath;
                        }else System.out.println(currPath);
                    }else System.out.println(currPath);
                    try{
                        Data currData;
                        try{
                            currData = this.tree.getData(nids[i], 0);
                        }catch(final Exception exc){
                            currData = null;
                        }
                        if(currData != null){
                            final String currDecompiled = currData.toString();
                            nodesV.addElement(new NodeDescriptor(currPath, currDecompiled, currInfo.isOn(), currInfo.isParentOn(), currInfo.isNoWriteModel() || currInfo.isWriteOnce()));
                        }else nodesV.addElement(new NodeDescriptor(currPath, null, currInfo.isOn(), currInfo.isParentOn(), currInfo.isNoWriteModel() || currInfo.isWriteOnce()));
                    }catch(final Exception exc){
                        nodesV.addElement(new NodeDescriptor(currPath, null, currInfo.isOn(), currInfo.isParentOn(), currInfo.isNoWriteModel() || currInfo.isWriteOnce()));
                    }
                }
            }catch(final Exception exc){
                System.out.println("Error reading " + basePath + ": " + exc);
            }
        }
        this.numPMUnits = this.countPMUnits();
        this.evaluatePCConnection();
        this.evaluatePVConnection();
        this.evaluateRTransfer();
        this.tree.close(0);
        br.close();
        return nodesV;
    }

    String getPCConnection() {
        return this.PCconnection;
    }

    int getPMUnits() {
        return this.numPMUnits;
    }

    String getPVConnection() {
        return this.PVconnection;
    }

    float getRTransfer() {
        return this.rTransfer;
    }

    void getSetup(final String experiment, final int shot, final Hashtable<String, String> setupHash, final Hashtable<String, Boolean> setupOnHash) throws Exception {
        final Vector<NodeDescriptor> nodesV = this.getNodes(experiment, shot);
        int i;
        NodeDescriptor currNode;
        for(i = 0; i < nodesV.size(); i++){
            currNode = nodesV.elementAt(i);
            final String decompiled = currNode.getDecompiled();
            if(decompiled != null){
                try{
                    setupHash.put(currNode.getPath(), currNode.getDecompiled());
                }catch(final Exception exc){
                    System.out.println("Internal error in LoadPulse.getSetup(): " + exc);
                }
            }
            try{
                setupOnHash.put(currNode.getPath(), new Boolean(currNode.isOn()));
            }catch(final Exception exc){
                System.out.println("Internal error in LoadPulse.getSetup(): " + exc);
            }
        }
    }

    // Load setup and expands path names into abs path names with reference to pathRefShot
    public void getSetupWithAbsPath(final String experiment, final int shot, final int pathRefShot, final Hashtable<String, String> setupHash, final Hashtable<String, Boolean> setupOnHash) throws Exception {
        final Hashtable<String, String> currSetupHash = new Hashtable<String, String>();
        this.getSetup(experiment, shot, currSetupHash, setupOnHash);
        try{
            final Database tree = new Database(experiment, pathRefShot);
            tree.open();
            final Enumeration<String> pathNamesEn = currSetupHash.keys();
            while(pathNamesEn.hasMoreElements()){
                final String currPath = pathNamesEn.nextElement();
                try{
                    final NidData currNid = tree.resolve(new PathData(currPath), 0);
                    final NodeInfo currInfo = tree.getInfo(currNid, 0);
                    final String currAbsPath = currInfo.getFullPath();
                    setupHash.put(currAbsPath, currSetupHash.get(currPath));
                }catch(final Exception exc){
                    System.out.println("LoadSetup: Cannot expand path name " + currPath + " : " + exc);
                }
            }
            tree.close(0);
        }catch(final Exception exc){
            System.out.println("Cannot expand path names in LoadSetup: " + exc);
        }
    }

    void load(final String experiment, final int shot, final int outShot) throws Exception {
        System.out.println("LOAD PULSE");
        final Vector<NodeDescriptor> nodesV = this.getNodes(experiment, shot);
        try{
            this.tree = new Database(experiment, outShot);
            this.tree.open();
            for(int i = 0; i < nodesV.size(); i++){
                final NodeDescriptor currNode = nodesV.elementAt(i);
                try{
                    final NidData currNid = this.tree.resolve(new PathData(currNode.getPath()), 0);
                    // if(currNode.isNoWriteModel()) System.out.println("NO WRITE MODEL!!" + currNode.getPath());
                    if(currNode.getDecompiled() != null && !currNode.isNoWriteModel()){
                        final Data currData = Data.fromExpr(currNode.getDecompiled());
                        this.tree.putData(currNid, currData, 0);
                    }
                    if(currNode.isOn() && currNode.isParentOn()) this.tree.setOn(currNid, true, 0);
                    else if(currNode.isParentOn()){
                        this.tree.setOn(currNid, false, 0);
                    }
                }catch(final Exception exc){
                    System.out.println("Error writing " + currNode.getPath() + " in model: " + exc);
                }
            }
            this.tree.close(0);
        }catch(final Exception exc){
            System.out.println("FATAL ERROR: " + exc);
        }
    }
}
