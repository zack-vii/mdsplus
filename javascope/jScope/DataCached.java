package jScope;

/* $Id$ */
import java.io.Serializable;

public class DataCached implements Serializable{
    static final long serialVersionUID = 56436845874835L;
    String            expression;
    String            experiment;
    long              shot;
    Object            data;

    DataCached(String expression, String experiment, long shot, Object data){
        this.expression = expression;
        this.shot = shot;
        this.experiment = experiment;
        this.data = data;
    }

    public boolean equals(String expression, String experiment, long shot) {
        return(this.expression.equals(expression) && this.shot == shot && this.experiment.equals(experiment));
    }
}
