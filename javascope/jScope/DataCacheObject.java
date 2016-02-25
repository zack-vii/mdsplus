package jScope;

/* $Id$ */
import java.io.Serializable;

class DataCacheObject implements Serializable{
    static final long serialVersionUID = 35346874385634L;
    float             data[];
    float             x[];
    double            x_double[];
    float             y[];
    float             up_err[];
    float             low_err[];
    int               dimension;
    String            title;
    String            x_label;
    String            y_label;
    String            z_label;
}
