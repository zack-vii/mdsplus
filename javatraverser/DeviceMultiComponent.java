import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;

public abstract class DeviceMultiComponent extends DeviceComponent{
    private static final long serialVersionUID = -7778546697746673348L;

    protected static String getNameSeparator() {
        return "/";
    }
    protected String baseName = ".PARAMETERS";
    NidData          compNids[];

    protected abstract void addParameter(JPanel jp, NidData nidData);

    @Override
    public void apply() throws Exception {
        for(final NidData compNid : this.compNids)
            this.applyComponent(compNid);
    }

    @Override
    public void apply(final int currBaseNid) throws Exception {
        this.apply();
    }

    protected abstract void applyComponent(NidData nidData);

    @Override
    public void configure(final int baseNid) {
        try{
            final NidData prevDefNid = this.subtree.getDefault(0);
            this.subtree.setDefault(new NidData(baseNid), 0);
            this.nidData = this.subtree.resolve(new PathData(this.baseName), 0);
            this.subtree.setDefault(prevDefNid, 0);
        }catch(final Exception exc){
            JOptionPane.showMessageDialog(null, "Cannot resolve base nid: " + this.baseName);
            return;
        }
        this.baseNid = baseNid;
        this.baseNidData = new NidData(baseNid);
        final int numComponents = this.getNumComponents(this.nidData);
        final String compNames[] = new String[numComponents];
        this.compNids = new NidData[numComponents];
        for(int i = 0; i < numComponents; i++){
            compNames[i] = this.getComponentNameAt(this.nidData, i);
            this.compNids[i] = this.getComponentNidAt(this.nidData, i);
        }
        final Hashtable<String, Vector<NidData>> compHash = new Hashtable<String, Vector<NidData>>();
        final String separator = DeviceMultiComponent.getNameSeparator();
        for(int i = 0; i < numComponents; i++){
            if(compNames[i] == null) continue;
            final StringTokenizer st = new StringTokenizer(compNames[i], separator);
            String firstPart = st.nextToken();
            if(!st.hasMoreTokens()) firstPart = "Default";
            Vector<NidData> nidsV = compHash.get(firstPart);
            if(nidsV == null){
                nidsV = new Vector<NidData>();
                compHash.put(firstPart, nidsV);
            }
            if(this.compNids != null) nidsV.addElement(this.compNids[i]);
        }
        this.setLayout(new BorderLayout());
        final JTabbedPane tabP = new JTabbedPane();
        this.add(tabP, "Center");
        final Enumeration groups = compHash.keys();
        while(groups.hasMoreElements()){
            final String currName = (String)groups.nextElement();
            final JPanel jp = new JPanel();
            tabP.add(currName, new JScrollPane(jp));
            final Vector currParams = compHash.get(currName);
            final int nParams = currParams.size();
            jp.setLayout(new GridLayout(nParams, 1));
            for(int i = 0; i < nParams; i++)
                this.addParameter(jp, (NidData)currParams.elementAt(i));
        }
    }

    @Override
    protected void displayData(final Data data, final boolean is_on) {}

    public String getBaseName() {
        return this.baseName;
    }

    protected abstract String getComponentNameAt(NidData nidData, int idx);

    protected abstract NidData getComponentNidAt(NidData nidData, int idx);

    @Override
    protected Data getData() {
        return null;
    }

    protected abstract int getNumComponents(NidData nidData);

    @Override
    protected boolean getState() {
        return false;
    }

    @Override
    protected void initializeData(final Data data, final boolean is_on) {}

    @Override
    public void reset() {
        for(final NidData compNid : this.compNids)
            this.resetComponent(compNid);
    }

    protected abstract void resetComponent(NidData nidData);

    // return null when no more components
    public void setBaseName(final String baseName) {
        this.baseName = baseName;
    }
}
