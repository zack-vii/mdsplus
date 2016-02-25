import java.util.StringTokenizer;
import java.util.Vector;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class CompileTree extends Thread{
    public static void main(final String args[]) {
        String experiment;
        int shot = -1;
        if(args.length < 1){
            System.out.println("Usage: java CompileTree <experiment> [<shot>]");
            System.exit(0);
        }
        experiment = args[0];
        if(args.length > 1){
            try{
                shot = Integer.parseInt(args[1]);
            }catch(final Exception exc){
                System.out.println("Error Parsing shot number");
                System.exit(0);
            }
        }
        (new CompileTree(experiment, shot)).start();
    }
    String          experiment;
    Vector<String>  newNames         = new Vector<String>();
    // originalNames and renamedNames keep info about nodes to be renamed
    Vector<String>  renamedDevices   = new Vector<String>();
    Vector<String>  renamedFieldNids = new Vector<String>();
    int             shot;
    Vector<NidData> subtreeNids      = new Vector<NidData>();
    Database        tree;
    Vector<String>  unresolvedExprV  = new Vector<String>();
    Vector<NidData> unresolvedNidV   = new Vector<NidData>();

    CompileTree(final String experiment, final int shot){
        this.experiment = experiment;
        this.shot = shot;
    }

    void recCompile(final Element node) {
        final String type = node.getNodeName();
        String name = node.getAttribute("NAME");
        // final String state = node.getAttribute("STATE");
        final String usageStr = node.getAttribute("USAGE");
        NidData nid = null;
        boolean success;
        try{
            final NidData parentNid = this.tree.getDefault(0);
            success = false;
            if(type.equals("data")){
                // final Element parentNode = (Element)node.getParentNode();
                final boolean isDeviceField = node.getNodeName().equals("field");
                final Text dataNode = (Text)node.getFirstChild();
                if(dataNode != null){
                    final String dataStr = dataNode.getData();
                    final Data data = null;
                    {
                        this.unresolvedExprV.addElement(dataStr);
                        this.unresolvedNidV.addElement(this.tree.getDefault(0));
                    }
                    try{
                        nid = this.tree.getDefault(0);
                        if(isDeviceField){
                            Data oldData;
                            try{
                                oldData = this.tree.getData(nid, 0);
                            }catch(final Exception exc){
                                oldData = null;
                            }
                            if(oldData == null || !dataStr.equals(oldData.toString())) this.tree.putData(nid, data, 0);
                        }else this.tree.putData(nid, data, 0);
                    }catch(final Exception exc){
                        System.out.println("Error writing data: " + exc);
                    }
                }
                return;
            }
            // First handle renamed nodes: they do not need to be created, but to be renamed
            final String originalDevice = node.getAttribute("DEVICE");
            final String deviceOffsetStr = node.getAttribute("OFFSET_NID");
            if(originalDevice != null && deviceOffsetStr != null && !originalDevice.equals("") && !deviceOffsetStr.equals("")){
                String newName;
                try{
                    newName = (this.tree.getInfo(parentNid, 0)).getFullPath();
                }catch(final Exception exc){
                    System.err.println("Error getting renamed path: " + exc);
                    return;
                }
                if(type.equals("node")) newName += "." + name;
                else newName += ":" + name;
                this.newNames.addElement(newName);
                this.renamedDevices.addElement(originalDevice);
                this.renamedFieldNids.addElement(deviceOffsetStr);
                return; // No descendant for a renamed node
            }
            if(type.equals("node")){
                try{
                    if(name.length() > 12) name = name.substring(0, 12);
                    nid = this.tree.addNode("." + name, NodeInfo.USAGE_STRUCTURE, 0);
                    if(usageStr != null && usageStr.equals("SUBTREE")) this.subtreeNids.addElement(nid);
                    this.tree.setDefault(nid, 0);
                    success = true;
                }catch(final Exception e){
                    System.err.println("Error adding member " + name + " : " + e);
                }
            }else if(type.equals("member")){
                int usage = NodeInfo.USAGE_NONE;
                if(usageStr.equals("NONE")) usage = NodeInfo.USAGE_NONE;
                if(usageStr.equals("ACTION")) usage = NodeInfo.USAGE_ACTION;
                if(usageStr.equals("NUMERIC")) usage = NodeInfo.USAGE_NUMERIC;
                if(usageStr.equals("SIGNAL")) usage = NodeInfo.USAGE_SIGNAL;
                if(usageStr.equals("TASK")) usage = NodeInfo.USAGE_TASK;
                if(usageStr.equals("TEXT")) usage = NodeInfo.USAGE_TEXT;
                if(usageStr.equals("WINDOW")) usage = NodeInfo.USAGE_WINDOW;
                if(usageStr.equals("AXIS")) usage = NodeInfo.USAGE_AXIS;
                if(usageStr.equals("DISPATCH")) usage = NodeInfo.USAGE_DISPATCH;
                try{
                    if(name.length() > 12) name = name.substring(0, 12);
                    nid = this.tree.addNode(":" + name, usage, 0);
                    this.tree.setDefault(nid, 0);
                    success = true;
                }catch(final Exception e){
                    System.err.println("Error adding member " + name + " : " + e);
                }
            }else if(type.equals("device")){
                final String model = node.getAttribute("MODEL");
                // final NodeInfo info = this.tree.getInfo(parentNid, 0);
                try{
                    Thread.currentThread();
                    Thread.sleep(100);
                    nid = this.tree.addDevice(name.trim(), model, 0);
                    if(nid != null){
                        this.tree.setDefault(nid, 0);
                        success = true;
                    }
                }catch(final Exception exc){}
            }else if(type.equals("field")){
                try{
                    nid = this.tree.resolve(new PathData(name), 0);
                    this.tree.setDefault(nid, 0);
                    success = true;
                }catch(final Exception e){
                    System.err.println("WARNING: device field  " + name + " not found in model : " + e);
                }
            }
            if(success){
                // tags
                final String tagsStr = node.getAttribute("TAGS");
                if(tagsStr != null && tagsStr.length() > 0){
                    int i = 0;
                    final StringTokenizer st = new StringTokenizer(tagsStr, ", ");
                    final String[] tags = new String[st.countTokens()];
                    while(st.hasMoreTokens())
                        tags[i++] = st.nextToken();
                    try{
                        this.tree.setTags(nid, tags, 0);
                    }catch(final Exception exc){
                        System.err.println("Error adding tags " + tagsStr + " : " + exc);
                    }
                }
                // flags
                final String flagsStr = node.getAttribute("FLAGS");
                if(flagsStr != null && flagsStr.length() > 0){
                    int flags = 0;
                    final StringTokenizer st = new StringTokenizer(flagsStr, ", ");
                    while(st.hasMoreTokens()){
                        final String flag = st.nextToken();
                        if(flag.equals("WRITE_ONCE")) flags |= NodeInfo.WRITE_ONCE;
                        if(flag.equals("COMPRESSIBLE")) flags |= NodeInfo.COMPRESSIBLE;
                        if(flag.equals("COMPRESS_ON_PUT")) flags |= NodeInfo.COMPRESS_ON_PUT;
                        if(flag.equals("NO_WRITE_MODEL")) flags |= NodeInfo.NO_WRITE_MODEL;
                        if(flag.equals("NO_WRITE_SHOT")) flags |= NodeInfo.NO_WRITE_SHOT;
                    }
                    try{
                        this.tree.setFlags(nid, flags);
                    }catch(final Exception e){
                        System.err.println("Error setting flags to node " + name + " : " + e);
                    }
                }
                // state
                final String stateStr = node.getAttribute("STATE");
                if(stateStr != null && stateStr.length() > 0){
                    try{
                        if(stateStr.equals("ON")) this.tree.setOn(nid, true, 0);
                        if(stateStr.equals("OFF")) this.tree.setOn(nid, false, 0);
                    }catch(final Exception e){
                        System.err.println("Error setting state of node " + name + " : " + e);
                    }
                }
                // Descend
                final NodeList nodes = node.getChildNodes();
                for(int i = 0; i < nodes.getLength(); i++){
                    final Node currNode = nodes.item(i);
                    if(currNode.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) // Only element nodes at this
                    this.recCompile((Element)currNode);
                }
            }
            this.tree.setDefault(parentNid, 0);
        }catch(final Exception e){
            System.err.println("Internal error in recCompile: " + e);
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try{
            final DocumentBuilder builder = factory.newDocumentBuilder();
            final Document document = builder.parse(this.experiment + ".xml");
            final Element rootNode = document.getDocumentElement();
            final NodeList nodes = rootNode.getChildNodes();
            for(int i = 0; i < nodes.getLength(); i++){
                final Node currNode = nodes.item(i);
                if(currNode.getNodeType() == Node.ELEMENT_NODE) // Only element nodes at this
                this.recCompile((Element)currNode);
            }
        }catch(final SAXParseException e){ // Error generated by the parser
            System.err.println("\n** Parsing error" + ", line " + e.getLineNumber() + ", uri " + e.getSystemId());
            System.err.println("   " + e.getMessage());
            final Exception x = e.getException();
            if(x != null) x.printStackTrace();
            else e.printStackTrace();
        }catch(final SAXException se){ // Error generated during parsing
            final Exception e = se.getException();
            if(e != null) e.printStackTrace();
            else se.printStackTrace();
        }catch(final ParserConfigurationException e){ // Parser with specified options can't be built
            e.printStackTrace();
        }catch(final Exception e){ // I/O error
            e.printStackTrace();
        }
        this.tree = new Database(this.experiment, this.shot);
        this.tree.setEditable(true);
        try{
            this.tree.openNew();
        }catch(final Exception e){
            System.err.println("Error opening tree " + this.experiment + " : " + e);
            System.exit(0);
        }
        // handle renamed nodes
        for(int i = 0; i < this.newNames.size(); i++){
            final String newName = this.newNames.elementAt(i);
            final String deviceName = this.renamedDevices.elementAt(i);
            final String offsetStr = this.renamedFieldNids.elementAt(i);
            try{
                final int deviceOffset = Integer.parseInt(offsetStr);
                final NidData deviceNid = this.tree.resolve(new PathData(deviceName), 0);
                final NidData renamedNid = new NidData(deviceNid.getInt() + deviceOffset);
                this.tree.renameNode(renamedNid, newName, 0);
            }catch(final Exception e){
                System.err.println("Error renaming node of " + deviceName + " to " + newName + " : " + e);
            }
        }
        for(int i = 0; i < this.unresolvedNidV.size(); i++){
            Data data = null;
            try{
                this.tree.setDefault(this.unresolvedNidV.elementAt(i), 0);
                data = Data.fromExpr(this.unresolvedExprV.elementAt(i));
            }catch(final Exception e){
                System.err.println("Error parsing expression " + this.unresolvedExprV.elementAt(i) + " : " + e);
            }
            try{
                this.tree.putData(this.unresolvedNidV.elementAt(i), data, 0);
            }catch(final Exception e){
                System.err.println("Error writing data: " + e);
            }
        }
        // Set subtrees (apparently this must be done at the end....
        for(int i = 0; i < this.subtreeNids.size(); i++){
            try{
                this.tree.setSubtree(this.subtreeNids.elementAt(i), 0);
            }catch(final Exception e){
                System.err.println("Error setting subtree: " + e);
            }
        }
        try{
            this.tree.write(0);
            this.tree.close(0);
        }catch(final Exception e){
            System.err.println("Error closeing tree: " + e);
        }
    }
}
