import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

public class DeviceWaveParameters extends DeviceParameters{
    private static final long serialVersionUID = -3104891286776580295L;

    public DeviceWaveParameters(){}

    @Override
    protected void addParameter(final JPanel jp, final NidData nidData) {
        try{
            this.subtree.setDefault(nidData, 0);
            NidData currNid;
            currNid = this.subtree.resolve(new PathData(":DESCRIPTION"), 0);
            final String description = this.subtree.evaluateData(currNid, 0).getString();
            final NidData currXNid = this.subtree.resolve(new PathData(":X"), 0);
            final DeviceField currXField = new DeviceField();
            currXField.setSubtree(this.subtree);
            currXField.setBaseNid(currXNid.getInt());
            currXField.setOffsetNid(0);
            currXField.setLabelString("X:");
            currXField.setNumCols(30);
            this.parameters.add(currXField);
            final NidData currYNid = this.subtree.resolve(new PathData(":Y"), 0);
            final DeviceField currYField = new DeviceField();
            currYField.setSubtree(this.subtree);
            currYField.setBaseNid(currYNid.getInt());
            currYField.setOffsetNid(0);
            currYField.setLabelString("Y:");
            currYField.setNumCols(30);
            this.parameters.add(currYField);
            final JPanel jp1 = new JPanel();
            jp1.add(new JLabel(description));
            jp1.add(currXField);
            jp1.add(currYField);
            jp.add(jp1);
            currXField.configure(currXNid.getInt());
            currYField.configure(currYNid.getInt());
        }catch(final Exception exc){
            JOptionPane.showMessageDialog(null, "Error in DeviceWaveParameters.addParam: " + exc);
        }
    }

    @Override
    protected String getParameterName() {
        return "WAVE";
    }
}
