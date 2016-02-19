package jScope;

/* $Id$ */
final class jScopeDefaultValues{
    String          experiment_str, shot_str;
    private boolean is_evaluated     = false;
    private String  public_variables = null;
    long            shots[];
    String          title_str, xlabel, ylabel;
    String          upd_event_str, def_node_str;
    boolean         upd_limits       = true;
    String          xmin, xmax, ymax, ymin;

    public boolean getIsEvaluated() {
        return this.is_evaluated || this.public_variables == null || this.public_variables.length() == 0;
    }

    public String getPublicVariables() {
        return this.public_variables;
    }

    public boolean isSet() {
        return(this.public_variables != null && this.public_variables.length() > 0);
    }

    public void Reset() {
        this.shots = null;
        this.xmin = this.xmax = this.ymax = this.ymin = null;
        this.title_str = this.xlabel = this.ylabel = null;
        this.experiment_str = this.shot_str = null;
        this.upd_event_str = this.def_node_str = null;
        this.is_evaluated = false;
        this.upd_limits = true;
    }

    public void setIsEvaluated(final boolean evaluated) {
        this.is_evaluated = evaluated;
    }

    public void setPublicVariables(final String public_variables) {
        if(this.public_variables == null || public_variables == null || !this.public_variables.equals(public_variables)){
            this.is_evaluated = false;
            this.public_variables = public_variables.trim();
        }
    }
}
