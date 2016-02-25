// package jTraverser;
public class ConditionData extends CompoundData{
    public static final int   IGNORE_STATUS    = 9;
    public static final int   IGNORE_UNDEFINED = 8;
    public static final int   NEGATE_CONDITION = 7;
    private static final long serialVersionUID = -8677452655184157317L;

    public static Data getData() {
        return new ConditionData();
    }

    public ConditionData(){
        this.dtype = Data.DTYPE_CONDITION;
    }

    public ConditionData(final Data argument){
        this.dtype = Data.DTYPE_CONDITION;
        this.opcode = 0;
        this.descs = new Data[1];
        this.descs[0] = argument;
    }

    public ConditionData(final int modifier, final Data argument){
        this.dtype = Data.DTYPE_CONDITION;
        this.opcode = modifier;
        this.descs = new Data[1];
        this.descs[0] = argument;
    }

    public final Data getArgument() {
        return this.descs[0];
    }

    public final int getModifier() {
        return this.opcode;
    }
}