import java.io.File;
import java.io.FileOutputStream;
import java.util.Properties;
import java.util.Vector;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

public class DecompileTree{
    static Document document;
    static Database mdsTree;

    public static void main(final String args[]) {
        if(args.length < 1){
            System.err.println("Usage: java DecompileTree <treeName> [<shot>]");
            System.exit(0);
        }
        final String treeName = args[0];
        int shot = -1;
        if(args.length > 1){
            try{
                shot = Integer.parseInt(args[1]);
            }catch(final Exception exc){
                System.err.println("Invalid shot number");
                System.exit(0);
            }
        }
        final Properties properties = System.getProperties();
        final String full = properties.getProperty("full");
        boolean isFull = false;
        if(full != null && full.equals("yes")) isFull = true;
        String outName = properties.getProperty("out");
        if(outName == null) outName = args[0] + ".xml";
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try{
            final DocumentBuilder builder = factory.newDocumentBuilder();
            DecompileTree.document = builder.newDocument(); // Create from whole cloth
        }catch(final Exception exc){
            System.err.println("Cannot instantiate a new Document: " + exc);
            DecompileTree.document = null;
            System.exit(0);
        }
        final Element tree = DecompileTree.document.createElement("tree");
        DecompileTree.mdsTree = new Database(treeName, shot);
        try{
            DecompileTree.mdsTree.open();
        }catch(final Exception exc){
            System.err.println("Cannot open tree: " + exc);
            System.exit(0);
        }
        final NidData topNid = new NidData(0);
        NidData[] sons;
        try{
            sons = DecompileTree.mdsTree.getSons(topNid, 0);
        }catch(final Exception exc){
            sons = new NidData[0];
        }
        for(final NidData son : sons){
            final Element docSon = DecompileTree.document.createElement("node");
            tree.appendChild(docSon);
            DecompileTree.recDecompile(son, docSon, false, isFull);
        }
        NidData[] members;
        try{
            members = DecompileTree.mdsTree.getMembers(topNid, 0);
        }catch(final Exception exc){
            members = new NidData[0];
        }
        for(final NidData member : members){
            Element docMember = null;
            try{
                final NodeInfo info = DecompileTree.mdsTree.getInfo(member, 0);
                if(info.getUsage() == NodeInfo.USAGE_DEVICE) docMember = DecompileTree.document.createElement("device");
                if(info.getUsage() == NodeInfo.USAGE_COMPOUND_DATA) docMember = DecompileTree.document.createElement("compound_data");
                else docMember = DecompileTree.document.createElement("member");
            }catch(final Exception exc){
                System.err.println(exc);
            }
            tree.appendChild(docMember);
            DecompileTree.recDecompile(member, docMember, false, isFull);
        }
        final TransformerFactory transFactory = TransformerFactory.newInstance();
        try{
            final Transformer transformer = transFactory.newTransformer();
            final DOMSource source = new DOMSource(tree);
            final File newXML = new File(outName);
            final FileOutputStream os = new FileOutputStream(newXML);
            final StreamResult result = new StreamResult(os);
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.transform(source, result);
        }catch(final Exception exc){
            System.err.println(exc);
        }
    }

    static void recDecompile(final NidData nid, final Element node, final boolean isDeviceField, final boolean isFull) {
        try{
            final NidData prevNid = DecompileTree.mdsTree.getDefault(0);
            DecompileTree.mdsTree.setDefault(nid, 0);
            NodeInfo info = null;
            try{
                info = DecompileTree.mdsTree.getInfo(nid, 0);
            }catch(final Exception exc){
                System.err.println("Error getting info: " + exc);
                return;
            }
            String[] tags;
            try{
                tags = DecompileTree.mdsTree.getTags(nid, 0);
            }catch(final Exception exc){
                tags = new String[0];
            }
            if(isDeviceField) // Handle device field display
            {
                Data data = null;
                // TACON
                if(info.getName().endsWith("_GAIN")) System.out.println("TACON: " + info.getName());
                if(info.isSetup() || isFull || info.getName().endsWith("_GAIN"))
                // if(info.isSetup() || isFull)
                {
                    try{
                        data = DecompileTree.mdsTree.getData(nid, 0);
                    }catch(final Exception exc){
                        data = null;
                    }
                }
                // Handle Possible non-device subtree. Never, never do that!!.....
                NidData sons[], members[];
                final Vector<NidData> subtreeNodes = new Vector<NidData>();
                try{
                    sons = DecompileTree.mdsTree.getSons(nid, 0);
                }catch(final Exception exc){
                    sons = new NidData[0];
                }
                for(final NidData son : sons){
                    try{
                        final NodeInfo currInfo = DecompileTree.mdsTree.getInfo(son, 0);
                        if(currInfo.getConglomerateElt() == 1) // Descendant NOT belonging to the device
                        subtreeNodes.addElement(son);
                    }catch(final Exception exc){}
                }
                final Vector<NidData> subtreeMembers = new Vector<NidData>();
                try{
                    members = DecompileTree.mdsTree.getMembers(nid, 0);
                }catch(final Exception exc){
                    members = new NidData[0];
                }
                for(final NidData member : members){
                    try{
                        final NodeInfo currInfo = DecompileTree.mdsTree.getInfo(member, 0);
                        if(currInfo.getConglomerateElt() == 1) // Descendant NOT belonging to the device
                        subtreeMembers.addElement(member);
                    }catch(final Exception exc){}
                }
                if((!info.isOn() && info.isParentOn()) || (info.isOn() && !info.isParentOn()) || (info.isSetup() && data != null) || tags.length > 0 || subtreeNodes.size() > 0 || subtreeMembers.size() > 0 || isFull
                // TACON
                || info.getName().endsWith("_GAIN")) // show it only at these conditions
                {
                    final Element fieldNode = DecompileTree.document.createElement("field");
                    node.appendChild(fieldNode);
                    final int conglomerateElt = info.getConglomerateElt();
                    final NidData deviceNid = new NidData(nid.getInt() - conglomerateElt + 1);
                    NodeInfo deviceInfo = null;
                    try{
                        deviceInfo = DecompileTree.mdsTree.getInfo(deviceNid, 0);
                    }catch(final Exception exc){
                        System.err.println("Error getting device info: " + exc);
                        return;
                    }
                    final String devicePath = deviceInfo.getFullPath();
                    final String fieldPath = info.getFullPath();
                    if(fieldPath.startsWith(devicePath)) // if the field has not been renamed
                    {
                        fieldNode.setAttribute("NAME", fieldPath.substring(devicePath.length(), fieldPath.length()));
                        if(!info.isOn() && info.isParentOn()) fieldNode.setAttribute("STATE", "OFF");
                        if(info.isOn() && !info.isParentOn()) fieldNode.setAttribute("STATE", "ON");
                        if(tags.length > 0){
                            String tagList = "";
                            for(int i = 0; i < tags.length; i++)
                                tagList += (i == tags.length - 1) ? tags[i] : (tags[i] + ",");
                            fieldNode.setAttribute("TAGS", tagList);
                        }
                        if(data != null){
                            final Element dataNode = DecompileTree.document.createElement("data");
                            final Text dataText = DecompileTree.document.createTextNode(data.toString());
                            dataNode.appendChild(dataText);
                            fieldNode.appendChild(dataNode);
                        }
                    }
                }
                // Display possible non device subtrees
                for(int i = 0; i < subtreeNodes.size(); i++){
                    final Element currNode = DecompileTree.document.createElement("node");
                    DecompileTree.recDecompile(subtreeNodes.elementAt(i), currNode, false, isFull);
                }
                for(int i = 0; i < subtreeMembers.size(); i++){
                    final Element currNode = DecompileTree.document.createElement("member");
                    DecompileTree.recDecompile(subtreeMembers.elementAt(i), currNode, false, isFull);
                }
            } // End management of device fields
            else{
                node.setAttribute("NAME", info.getName());
                if(info.getUsage() == NodeInfo.USAGE_DEVICE || info.getUsage() == NodeInfo.USAGE_COMPOUND_DATA){
                    ConglomData deviceData = null;
                    try{
                        deviceData = (ConglomData)DecompileTree.mdsTree.getData(nid, 0);
                        final String model = deviceData.getModel().toString();
                        node.setAttribute("MODEL", model.substring(1, model.length() - 1));
                    }catch(final Exception exc){
                        System.err.println("Error reading device data: " + exc);
                    }
                }
                final int conglomerateElt = info.getConglomerateElt();
                // Handle renamed device fields
                if(conglomerateElt > 1){
                    final NidData deviceNid = new NidData(nid.getInt() - conglomerateElt + 1);
                    NodeInfo deviceInfo = null;
                    try{
                        deviceInfo = DecompileTree.mdsTree.getInfo(deviceNid, 0);
                        node.setAttribute("DEVICE", deviceInfo.getFullPath());
                        node.setAttribute("OFFSET_NID", "" + conglomerateElt);
                    }catch(final Exception exc){
                        System.err.println("Error getting device info: " + exc);
                    }
                }
                // tags
                try{
                    tags = DecompileTree.mdsTree.getTags(nid, 0);
                }catch(final Exception exc){
                    System.err.println("Error getting tags: " + exc);
                    tags = new String[0];
                }
                if(tags.length > 0){
                    String tagList = "";
                    for(int i = 0; i < tags.length; i++)
                        tagList += (i == tags.length - 1) ? tags[i] : (tags[i] + ",");
                    node.setAttribute("TAGS", tagList);
                }
                // state
                if(!info.isOn() && info.isParentOn()) node.setAttribute("STATE", "OFF");
                if(info.isOn() && !info.isParentOn()) node.setAttribute("STATE", "ON");
                // flags
                String flags = "";
                if(info.isWriteOnce()) flags += (flags.length() > 0) ? ",WRITE_ONCE" : "WRITE_ONCE";
                if(info.isCompressible()) flags += (flags.length() > 0) ? ",COMPRESSIBLE" : "COMPRESSIBLE";
                if(info.isCompressOnPut()) flags += (flags.length() > 0) ? ",COMPRESS_ON_PUT" : "COMPRESS_ON_PUT";
                if(info.isNoWriteModel()) flags += (flags.length() > 0) ? ",NO_WRITE_MODEL" : "NO_WRITE_MODEL";
                if(info.isNoWriteShot()) flags += (flags.length() > 0) ? ",NO_WRITE_SHOT" : "NO_WRITE_SHOT";
                if(flags.length() > 0) node.setAttribute("FLAGS", flags);
                // usage
                final int usage = info.getUsage();
                if(usage != NodeInfo.USAGE_STRUCTURE && usage != NodeInfo.USAGE_DEVICE && usage != NodeInfo.USAGE_COMPOUND_DATA){
                    String usageStr = "";
                    switch(usage){
                        case NodeInfo.USAGE_NONE:
                            usageStr = "NONE";
                            break;
                        case NodeInfo.USAGE_ACTION:
                            usageStr = "ACTION";
                            break;
                        case NodeInfo.USAGE_DISPATCH:
                            usageStr = "DISPATCH";
                            break;
                        case NodeInfo.USAGE_NUMERIC:
                            usageStr = "NUMERIC";
                            break;
                        case NodeInfo.USAGE_SIGNAL:
                            usageStr = "SIGNAL";
                            break;
                        case NodeInfo.USAGE_TASK:
                            usageStr = "TASK";
                            break;
                        case NodeInfo.USAGE_TEXT:
                            usageStr = "TEXT";
                            break;
                        case NodeInfo.USAGE_WINDOW:
                            usageStr = "WINDOW";
                            break;
                        case NodeInfo.USAGE_AXIS:
                            usageStr = "AXIS";
                            break;
                        case NodeInfo.USAGE_SUBTREE:
                            usageStr = "SUBTREE";
                            break;
                    }
                    node.setAttribute("USAGE", usageStr);
                    // if(info.isSetup())
                    {
                        Data data;
                        try{
                            data = DecompileTree.mdsTree.getData(nid, 0);
                        }catch(final Exception exc){
                            data = null;
                        }
                        if(data != null){
                            final Element dataNode = DecompileTree.document.createElement("data");
                            final Text dataText = DecompileTree.document.createTextNode(data.toString());
                            dataNode.appendChild(dataText);
                            node.appendChild(dataNode);
                        }
                    }
                }
                // handle descendants, if not subtree
                if(usage != NodeInfo.USAGE_SUBTREE){
                    NidData[] sons;
                    try{
                        sons = DecompileTree.mdsTree.getSons(nid, 0);
                    }catch(final Exception exc){
                        sons = new NidData[0];
                    }
                    if(info.getUsage() == NodeInfo.USAGE_DEVICE || info.getUsage() == NodeInfo.USAGE_COMPOUND_DATA){
                        // int numFields = info.getConglomerateNids() - 1;
                        final int numFields = info.getConglomerateNids();
                        for(int i = 1; i < numFields; i++)
                            DecompileTree.recDecompile(new NidData(nid.getInt() + i), node, true, isFull);
                    }else{
                        for(final NidData son : sons){
                            final Element docSon = DecompileTree.document.createElement("node");
                            node.appendChild(docSon);
                            DecompileTree.recDecompile(son, docSon, false, isFull);
                        }
                        NidData[] members;
                        try{
                            members = DecompileTree.mdsTree.getMembers(nid, 0);
                        }catch(final Exception exc){
                            members = new NidData[0];
                        }
                        for(final NidData member : members){
                            Element docMember;
                            final NodeInfo currInfo = DecompileTree.mdsTree.getInfo(member, 0);
                            if(currInfo.getUsage() == NodeInfo.USAGE_DEVICE) docMember = DecompileTree.document.createElement((currInfo.getUsage() == NodeInfo.USAGE_DEVICE) ? "device" : "compound_data");
                            else if(currInfo.getUsage() == NodeInfo.USAGE_COMPOUND_DATA) docMember = DecompileTree.document.createElement("compound_data");
                            else docMember = DecompileTree.document.createElement("member");
                            node.appendChild(docMember);
                            DecompileTree.recDecompile(member, docMember, false, isFull);
                        }
                    }
                }
            }
            DecompileTree.mdsTree.setDefault(prevNid, 0);
        }catch(final Exception exc){
            System.err.println(exc);
        }
    }
    /*
          Element member = (Element) document.createElement("member");
          member.setAttribute("USAGE", "TEXT");
          tree.appendChild(member);
          Element data = (Element) document.createElement("data");
          Text dataText = (Text) document.createTextNode("2+3");
          data.appendChild(dataText);
          member.appendChild(data);
    */
}
