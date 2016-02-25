// package jTraverser;
public class ActionData extends CompoundData{
    private static final long serialVersionUID = 3939211025217846692L;

    public static Data getData() {
        return new ActionData();
    }

    public ActionData(){
        this.dtype = Data.DTYPE_ACTION;
    }

    public ActionData(final Data dispatch, final Data task, final Data errorlogs, final Data completion_message, final Data performance){
        this.dtype = Data.DTYPE_ACTION;
        this.descs = new Data[5];
        this.descs[0] = dispatch;
        this.descs[1] = task;
        this.descs[2] = errorlogs;
        this.descs[3] = completion_message;
        this.descs[4] = performance;
    }

    public final Data getCompletionMessage() {
        if(this.descs.length >= 4) return this.descs[3];
        else return null;
    }

    public final Data getDispatch() {
        return this.descs[0];
    }

    public final Data getErrorlogs() {
        if(this.descs.length >= 3) return this.descs[2];
        else return null;
    }

    public final Data getPerformance() {
        if(this.descs.length >= 5) return this.descs[4];
        else return null;
    }

    public final Data getTask() {
        return this.descs[1];
    }
}