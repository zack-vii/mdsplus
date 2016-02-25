import java.util.Vector;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

public class DeviceParameters extends DeviceMultiComponent{
    private static final long         serialVersionUID = -2359203244949049236L;
    protected Vector<DeviceComponent> parameters       = new Vector<DeviceComponent>();

    @Override
    protected void addParameter(final JPanel jp, final NidData nidData) {
        try{
            this.subtree.setDefault(nidData, 0);
            NidData currNid;
            currNid = this.subtree.resolve(new PathData(":DESCRIPTION"), 0);
            final String description = this.subtree.evaluateData(currNid, 0).getString();
            currNid = this.subtree.resolve(new PathData(":TYPE"), 0);
            final String typeStr = this.subtree.evaluateData(currNid, 0).getString();
            currNid = this.subtree.resolve(new PathData(":DIMS"), 0);
            final int[] dims = this.subtree.evaluateData(currNid, 0).getIntArray();
            currNid = this.subtree.resolve(new PathData(":DATA"), 0);
            if(dims[0] == 0) // Scalar
            {
                final DeviceField currField = new DeviceField();
                currField.setSubtree(this.subtree);
                currField.setBaseNid(currNid.getInt());
                currField.setOffsetNid(0);
                currField.setLabelString(description);
                final JPanel jp1 = new JPanel();
                jp1.add(currField);
                jp.add(jp1);
                currField.configure(currNid.getInt());
                this.parameters.addElement(currField);
            }else // Array or Matrix, use DeviceTable
            {
                final DeviceTable currField = new DeviceTable();
                currField.setSubtree(this.subtree);
                currField.setBaseNid(currNid.getInt());
                currField.setOffsetNid(0);
                if(typeStr.toUpperCase().trim().equals("BINARY")) currField.setBinary(true);
                else currField.setBinary(false);
                if(typeStr.toUpperCase().equals("REFLEX")) currField.setRefMode(DeviceTable.REFLEX);
                if(typeStr.toUpperCase().equals("REFLEX_INVERT")) currField.setRefMode(DeviceTable.REFLEX_INVERT);
                currField.setUseExpressions(true);
                currField.setDisplayRowNumber(true);
                currField.setLabelString(description);
                int numCols;
                if(dims.length == 1){
                    currField.setNumRows(1);
                    currField.setNumCols(numCols = dims[0]);
                }else{
                    currField.setNumRows(dims[0]);
                    currField.setNumCols(numCols = dims[1]);
                }
                final String colNames[] = new String[numCols];
                if(typeStr.toUpperCase().equals("REFLEX_INVERT") || typeStr.toUpperCase().equals("REFLEX")){
                    for(int i = 0; i <= numCols / 2; i++)
                        colNames[i] = "" + (-i);
                    for(int i = 1; i < numCols / 2; i++)
                        colNames[numCols / 2 + i] = "" + (numCols / 2 - i);
                }else{
                    for(int i = 0; i < numCols; i++)
                        colNames[i] = "" + i;
                }
                currField.setColumnNames(colNames);
                jp.add(currField);
                currField.configure(currNid.getInt());
                this.parameters.addElement(currField);
            }
        }catch(final Exception exc){
            System.err.println("Error in DeviceParameters.addParam: " + exc);
        }
    }

    @Override
    protected void applyComponent(final NidData nidData) {
        try{
            for(int i = 0; i < this.parameters.size(); i++){
                this.parameters.elementAt(i).apply();
            }
        }catch(final Exception exc){
            System.err.println("Error in DeviceParameters.apply: " + exc);
        }
    }

    @Override
    protected String getComponentNameAt(final NidData nidData, final int idx) {
        String parName;
        NidData prevDefNid;
        final String paramName = this.getParameterName();
        if(idx < 10) parName = paramName + "_00" + (idx + 1);
        else if(idx < 100) parName = paramName + "_0" + (idx + 1);
        else parName = paramName + "_" + (idx + 1);
        try{
            prevDefNid = this.subtree.getDefault(0);
            this.subtree.setDefault(nidData, 0);
            NidData currNid;
            currNid = this.subtree.resolve(new PathData(parName + ":NAME"), 0);
            parName = this.subtree.evaluateData(currNid, idx).getString();
            this.subtree.setDefault(prevDefNid, 0);
        }catch(final Exception exc){
            JOptionPane.showMessageDialog(null, "Error getting Component Name in DeviceParameters: " + exc);
            parName = "";
        }
        return parName;
    }

    @Override
    protected NidData getComponentNidAt(final NidData nidData, final int idx) {
        String parName;
        NidData prevDefNid;
        final String paramName = this.getParameterName();
        if(idx < 10) parName = paramName + "_00" + (idx + 1);
        else if(idx < 100) parName = paramName + "_0" + (idx + 1);
        else parName = paramName + "_" + (idx + 1);
        try{
            prevDefNid = this.subtree.getDefault(0);
            this.subtree.setDefault(nidData, 0);
            NidData currNid;
            currNid = this.subtree.resolve(new PathData(parName), 0);
            this.subtree.setDefault(prevDefNid, 0);
            return currNid;
        }catch(final Exception exc){
            JOptionPane.showMessageDialog(null, "Error getting Component Nid in DeviceParameters: " + exc);
            return null;
        }
    }

    @Override
    protected int getNumComponents(final NidData nidData) {
        try{
            final NidData prevDefNid = this.subtree.getDefault(0);
            this.subtree.setDefault(nidData, 0);
            NidData currNid;
            currNid = this.subtree.resolve(new PathData(":NUM_ACTIVE"), 0);
            final int numComponents = this.subtree.evaluateData(currNid, 0).getInt();
            this.subtree.setDefault(prevDefNid, 0);
            return numComponents;
        }catch(final Exception exc){
            JOptionPane.showMessageDialog(null, "Error getting Num Components in DeviceParameters: " + exc);
            return 0;
        }
    }

    @SuppressWarnings("static-method")
    protected String getParameterName() {
        return "PAR";
    }

    @Override
    protected void resetComponent(final NidData nidData) {
        for(int i = 0; i < this.parameters.size(); i++){
            this.parameters.elementAt(i).reset();
        }
    }
}
