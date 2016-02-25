import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

public abstract class DeviceComponent extends JPanel{
    public static final int   DATA             = 0, STATE = 1, DISPATCH = 2;
    private static final long serialVersionUID = 7247369948407771866L;

    // Copy-Paste management
    protected static Object copyData() {
        return null;
    }
    public int        baseNid        = 0, offsetNid = 0;
    protected NidData baseNidData;
    protected Data    curr_data, init_data;
    protected boolean curr_on, init_on;
    protected boolean editable       = true;
    private boolean   enabled        = true;
    protected String  identifier;
    private boolean   is_initialized = false;
    protected boolean isHighlighted  = false;
    // Event handling in DW setup
    DeviceSetup       master         = null;
    public int        mode           = DeviceComponent.DATA;
    protected NidData nidData;
    RemoteTree        subtree;
    protected String  updateIdentifier;

    public void apply() throws Exception {
        if(!this.enabled) return;
        if(this.mode == DeviceComponent.DATA){
            this.curr_data = this.getData();
            /*            if(curr_data instanceof PathData)
            {
                try {
                    curr_data = subtree.resolve((PathData)curr_data, Tree.context);
                }catch(Exception exc){}
            }
              */
            if(this.editable && this.isDataChanged()){
                try{
                    this.subtree.putData(this.nidData, this.curr_data, Tree.context);
                }catch(final Exception e){
                    System.out.println("Error writing device data: " + e);
                    System.out.println(this.curr_data);
                    throw e;
                }
            }
        }
        if(this.mode != DeviceComponent.DISPATCH && this.supportsState()){
            this.curr_on = this.getState();
            try{
                this.subtree.setOn(this.nidData, this.curr_on, Tree.context);
            }catch(final Exception e){
                System.out.println("Error writing device state: " + e);
            }
        }
    }

    public void apply(final int currBaseNid) throws Exception {
        final NidData currNidData = new NidData(currBaseNid + this.offsetNid);
        if(!this.enabled) return;
        if(this.mode == DeviceComponent.DATA){
            this.curr_data = this.getData();
            if(this.editable)// && isDataChanged())
            {
                try{
                    this.subtree.putData(currNidData, this.curr_data, Tree.context);
                }catch(final Exception e){
                    System.out.println("Error writing device data: " + e);
                    System.out.println(this.curr_data);
                    throw e;
                }
            }
        }
        if(this.mode != DeviceComponent.DISPATCH && this.supportsState()){
            this.curr_on = this.getState();
            try{
                this.subtree.setOn(currNidData, this.curr_on, Tree.context);
            }catch(final Exception e){
                System.out.println("Error writing device state: " + e);
            }
        }
    }

    public void configure(final int baseNid) {
        this.baseNid = baseNid;
        this.nidData = new NidData(baseNid + this.offsetNid);
        this.baseNidData = new NidData(baseNid);
        if(this.mode == DeviceComponent.DATA){
            try{
                this.init_data = this.curr_data = this.subtree.getData(this.nidData, Tree.context);
            }catch(final Exception e){
                this.init_data = this.curr_data = null;
            }
        }else this.init_data = null;
        // if(mode != DISPATCH)
        {
            try{
                this.init_on = this.curr_on = this.subtree.isOn(this.nidData, Tree.context);
            }catch(final Exception e){
                System.out.println("Error configuring device: " + e);
            }
        }
        if(!this.is_initialized){
            this.initializeData(this.curr_data, this.curr_on);
            this.is_initialized = true;
        }else this.displayData(this.curr_data, this.curr_on);
    }

    public void configure(final int baseNid, final boolean readOnly) {
        this.configure(baseNid);
    }

    protected void dataChanged(final int offsetNid, final Object data) {}

    protected abstract void displayData(Data data, boolean is_on);

    public void fireUpdate(final String updateId, final Data newExpr) {}

    public int getBaseNid() {
        return this.baseNid;
    }

    protected abstract Data getData();

    // Get an object incuding all related info (will be data except for DeviceWaveform
    protected Object getFullData() {
        return this.getData();
    }

    public String getIdentifier() {
        return this.identifier;
    }

    public int getOffsetNid() {
        return this.offsetNid;
    }

    protected abstract boolean getState();

    RemoteTree getSubtree() {
        return this.subtree;
    }

    public String getUpdateId(final DeviceSetup master) {
        this.master = master;
        return this.updateIdentifier;
    }

    public String getUpdateIdentifier() {
        return this.updateIdentifier;
    }

    // To be subclassed
    protected abstract void initializeData(Data data, boolean is_on);

    protected boolean isChanged() {
        try{
            final String initDecompiled = Tree.dataToString(this.init_data);
            final String currDecompiled = Tree.dataToString(this.curr_data);
            // System.out.println("Comparing " + initDecompiled + " " + currDecompiled);
            return !(initDecompiled.equals(currDecompiled));
        }catch(final Exception exc){
            return false;
        }
    }

    @SuppressWarnings("static-method")
    protected boolean isDataChanged() {
        return true;
    }

    protected boolean isStateChanged() {
        return !(this.init_on == this.curr_on);
    }

    protected void pasteData(final Object objData) {}

    void postApply() {}

    public void postConfigure() {}

    protected void redisplay() {
        Container curr_container;
        Component curr_component = this;
        do{
            curr_container = curr_component.getParent();
            curr_component = curr_container;
        }while((curr_container != null) && !(curr_container instanceof Window));
        /* if(curr_container != null)
        {
            ((Window)curr_container).pack();
            ((Window)curr_container).setVisible(true);
        }*/
    }

    public void reportDataChanged(final Object data) {
        if(this.master == null) return;
        this.master.propagateData(this.offsetNid, data);
    }

    public void reportStateChanged(final boolean state) {
        if(this.master == null) return;
        this.master.propagateState(this.offsetNid, state);
    }

    public void reset() {
        this.curr_data = this.init_data;
        this.curr_on = this.init_on;
        this.displayData(this.curr_data, this.curr_on);
    }

    public void setBaseNid(final int nid) {
        this.baseNid = nid;
    }

    public void setDisable() {
        this.enabled = false;
    }

    public void setEnable() {
        this.enabled = true;
    }

    public void setHighlight(final boolean isHighlighted) {
        this.isHighlighted = isHighlighted;
        Component currParent, currGrandparent = this;
        do{
            currParent = currGrandparent;
            currGrandparent = currParent.getParent();
            if(currGrandparent instanceof JTabbedPane){
                final int idx = ((JTabbedPane)currGrandparent).indexOfComponent(currParent);
                ((JTabbedPane)currGrandparent).setForegroundAt(idx, isHighlighted ? Color.red : Color.black);
            }
        }while(!(currGrandparent instanceof DeviceSetup));
    }

    public void setIdentifier(final String identifier) {
        this.identifier = identifier;
    }

    public void setOffsetNid(final int nid) {
        this.offsetNid = nid;
    }

    void setSubtree(final RemoteTree subtree) {
        this.subtree = subtree;
    }

    public void setUpdateIdentifier(final String updateIdentifier) {
        this.updateIdentifier = updateIdentifier;
    }

    protected void stateChanged(final int offsetNid, final boolean state) {}

    @SuppressWarnings("static-method")
    protected boolean supportsState() {
        return false;
    }
}
