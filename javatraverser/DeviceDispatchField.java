import java.awt.GridLayout;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

public class DeviceDispatchField extends DeviceComponent{
    private static final long   serialVersionUID = 3063498160136657048L;
    protected ActionData        action;
    protected DispatchData      dispatch;
    protected LabeledExprEditor ident, phase, when, completion;
    protected JCheckBox         state;

    @Override
    protected void displayData(final Data data, final boolean is_on) {
        if(this.ident == null) return;
        if(!(data instanceof ActionData)){
            System.out.println("\nError: DeviceDispatchField used for non action data");
            return;
        }
        final DispatchData dispatch = (DispatchData)((ActionData)data).getDispatch();
        this.ident.setData(dispatch.getIdent());
        this.phase.setData(dispatch.getPhase());
        this.when.setData(dispatch.getWhen());
        this.completion.setData(dispatch.getCompletion());
        this.state.setSelected(is_on);
    }

    @Override
    protected Data getData() {
        if(this.dispatch == null) return null;
        return new ActionData(new DispatchData(this.dispatch.getType(), this.ident.getData(), this.phase.getData(), this.when.getData(), this.completion.getData()), this.action.getTask(), this.action.getErrorlogs(), this.action.getCompletionMessage(), this.action.getPerformance());
    }

    @Override
    protected boolean getState() {
        return this.state.isSelected();
    }

    @Override
    protected void initializeData(final Data data, final boolean is_on) {
        if(!(data instanceof ActionData)){
            System.out.println("\nError: DeviceDispatchField used for non action data");
            return;
        }
        this.action = (ActionData)data;
        this.dispatch = (DispatchData)((ActionData)data).getDispatch();
        if(this.dispatch == null) return;
        this.setLayout(new GridLayout(4, 1));
        this.add(this.ident = new LabeledExprEditor("Ident:         ", new ExprEditor(this.dispatch.getIdent(), true)));
        this.add(this.phase = new LabeledExprEditor("Phase:        ", new ExprEditor(this.dispatch.getPhase(), true)));
        this.add(this.completion = new LabeledExprEditor("Completion:", new ExprEditor(this.dispatch.getCompletion(), true)));
        final JPanel jp = new JPanel();
        jp.add(this.when = new LabeledExprEditor("Sequence:  ", new ExprEditor(this.dispatch.getWhen(), false, 1, 6)));
        jp.add(this.state = new JCheckBox("Is On", is_on));
        this.add(jp);
    }

    /*Allow writing only if model */
    @Override
    protected boolean isDataChanged() {
        try{
            if(this.subtree.getShot() == -1) return true;
            else return false;
        }catch(final Exception exc){
            return false;
        }
    }

    @Override
    public void setEnabled(final boolean state) {}
}
