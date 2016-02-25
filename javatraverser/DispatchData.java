// package jTraverser;
public class DispatchData extends CompoundData{
    public static final int   SCHED_ASYNCH     = 1;                  // 0x1000000;
    public static final int   SCHED_COND       = 3;                  // 0x3000000;
    public static final int   SCHED_NONE       = 0x0;
    public static final int   SCHED_SEQ        = 2;                  // 0x2000000;
    private static final long serialVersionUID = 269512865191103189L;

    public static Data getData() {
        return new DispatchData();
    }

    public DispatchData(){
        this.dtype = Data.DTYPE_DISPATCH;
    }

    public DispatchData(final int type, final Data ident, final Data phase, final Data when, final Data completion){
        this.dtype = Data.DTYPE_DISPATCH;
        this.opcode = type;
        this.descs = new Data[4];
        this.descs[0] = ident;
        this.descs[1] = phase;
        this.descs[2] = when;
        this.descs[3] = completion;
    }

    public final Data getCompletion() {
        return this.descs[3];
    }

    public final Data getIdent() {
        return this.descs[0];
    }

    public final Data getPhase() {
        return this.descs[1];
    }

    public final int getType() {
        return this.opcode;
    }

    public final Data getWhen() {
        return this.descs[2];
    }
}