package jScope;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.Vector;

public class ContourSignal{
    static final int   CASE_A          = 0;
    static final int   CASE_B          = 1;
    static final int   CASE_C          = 2;
    static final int   CASE_D          = 3;
    private static int rPoint_A[]      = {0, 1, 3};
    private static int rPoint_B[]      = {0, 2, 2};
    private static int rPoint_C[]      = {0, 1, 2};
    private static int rPoint_D[]      = {1, 1, 2};
    private static int succCase_A[]    = {ContourSignal.CASE_C, ContourSignal.CASE_A, ContourSignal.CASE_B};
    private static int succCase_B[]    = {ContourSignal.CASE_A, ContourSignal.CASE_B, ContourSignal.CASE_D};
    private static int succCase_C[]    = {ContourSignal.CASE_D, ContourSignal.CASE_C, ContourSignal.CASE_A};
    private static int succCase_D[]    = {ContourSignal.CASE_B, ContourSignal.CASE_D, ContourSignal.CASE_C};
    private static int xNear_A[]       = {0, 1, 1, 0};
    private static int xNear_B[]       = {1, 1, 0, 0};
    private static int xNear_C[]       = {0, 0, 1, 1};
    private static int xNear_D[]       = {0, -1, -1, 0};
    private static int yNear_A[]       = {0, 0, 1, 1};
    private static int yNear_B[]       = {0, 1, 1, 0};
    private static int yNear_C[]       = {0, -1, -1, 0};
    private static int yNear_D[]       = {0, 0, -1, -1};
    boolean            automaticLimits = true;
    // private boolean equalZ1;
    private boolean    equalZ2;
    double             x[];
    private boolean    xflag[][];
    double             xmin            = -1, xmax = 1;
    float              y[];
    double             ymin            = -1, ymax = 1;
    float              z[][];
    double             zmin            = -1, zmax = 1;

    ContourSignal(final double x[], final float y[], final float z[][]){
        if(x.length != z.length || y.length != z[0].length){ throw(new IllegalArgumentException("Z colum must be equals to x element end Z row to y elements")); }
        this.x = x;
        this.y = y;
        this.z = z;
        this.computeMinMax();
    }

    // private boolean edge = false;
    ContourSignal(final Signal s){
        if(s.getType() == Signal.TYPE_2D){
            this.setMinMaxX(s.getX2Dmin(), s.getX2Dmax());
            this.setMinMaxY(s.getY2Dmin(), s.getY2Dmax());
            this.setMinMaxZ(s.getZ2Dmin(), s.getZ2Dmax());
            this.x = s.getX2D();
            this.y = s.getY2D();
            this.z = s.getZ2D();
        }
    }

    private final boolean checkIntersection(final double level, final double z1, final double z2) {
        boolean out;
        out = (z1 < level && level < z2) || (z2 < level && level < z1) || (this.equalZ2 = (level == z2));
        return out;
    }

    private void computeMinMax() {
        this.xmin = this.xmax = this.x[0];
        this.ymin = this.ymax = this.y[0];
        this.zmin = this.zmax = this.z[0][0];
        for(int i = 0; i < this.x.length; i++){
            if(this.x[i] < this.xmin){
                this.xmin = this.x[i];
            }
            if(this.x[i] > this.xmax){
                this.xmax = this.x[i];
            }
            for(int j = 0; j < this.z[0].length; j++){
                if(this.z[i][j] < this.zmin){
                    this.zmin = this.z[i][j];
                }
                if(this.z[i][j] > this.zmax){
                    this.zmax = this.z[i][j];
                }
            }
        }
        for(final float element : this.y){
            if(element < this.ymin){
                this.ymin = element;
            }
            if(element > this.ymax){
                this.ymax = element;
            }
        }
        this.equalCases();
    }

    public Vector<Vector<Point2D.Double>> contour(final double level) {
        final Vector<Vector<Point2D.Double>> contours = new Vector<Vector<Point2D.Double>>();
        Vector<Point2D.Double> contour = new Vector<Point2D.Double>();
        double x1, y1, z1;
        double x2, y2, z2;
        double xc, yc, c1;
        // System.out.println("Livello " + level);
        int xNear[] = null;
        int yNear[] = null;
        int rPoint[] = null;
        int succCase[] = null;
        Point2D.Double firstCPoint = new Point2D.Double();
        Point2D.Double currCPoint = new Point2D.Double();
        this.xflag = new boolean[this.x.length][this.y.length];
        int edgeCase = ContourSignal.CASE_A;
        int ri = 0;
        int rj = 0;
        final int maxIteractions = this.x.length * this.y.length;
        for(int i = 0; i < this.x.length; i++){
            for(int j = 0; j < this.y.length - 1; j++){
                if(this.xflag[i][j]){
                    continue;
                }
                x1 = this.x[i];
                y1 = this.y[j];
                z1 = this.z[i][j];
                x2 = this.x[i];
                y2 = this.y[j + 1];
                z2 = this.z[i][j + 1];
                // xflag[i][j] = true;
                // System.out.println("Manin Case A set "+i+" "+j);
                if(this.checkIntersection(level, z1, z2)){
                    c1 = (level - z1) / (z2 - z1);
                    xc = x1 + (x2 - x1) * c1;
                    yc = y1 + (y2 - y1) * c1;
                    contour.addElement((firstCPoint = new Point2D.Double(xc, yc)));
                    edgeCase = ContourSignal.CASE_A;
                    ri = i;
                    rj = j;
                    if(this.equalZ2){
                        try{
                            this.xflag[i][j - 1] = true;
                            this.xflag[i][j] = true;
                        }catch(final Exception exc){}
                    }
                }else{
                    continue;
                }
                boolean contourCompleted = false;
                int l;
                int numIteractions = 0;
                while(!contourCompleted){
                    do{
                        try{
                            // edge = false;
                            // System.out.println("Riferimento ["+(ri)+","+(rj)+"]");
                            switch(edgeCase){
                                case CASE_A:
                                    // System.out.println("CASE_A");
                                    xNear = ContourSignal.xNear_A;
                                    yNear = ContourSignal.yNear_A;
                                    rPoint = ContourSignal.rPoint_A;
                                    succCase = ContourSignal.succCase_A;
                                    this.xflag[ri][rj] = true;
                                    break;
                                case CASE_B:
                                    // System.out.println("CASE_B");
                                    xNear = ContourSignal.xNear_B;
                                    yNear = ContourSignal.yNear_B;
                                    rPoint = ContourSignal.rPoint_B;
                                    succCase = ContourSignal.succCase_B;
                                    break;
                                case CASE_C:
                                    // System.out.println("CASE_C");
                                    xNear = ContourSignal.xNear_C;
                                    yNear = ContourSignal.yNear_C;
                                    rPoint = ContourSignal.rPoint_C;
                                    succCase = ContourSignal.succCase_C;
                                    break;
                                case CASE_D:
                                    // System.out.println("CASE_D");
                                    xNear = ContourSignal.xNear_D;
                                    yNear = ContourSignal.yNear_D;
                                    rPoint = ContourSignal.rPoint_D;
                                    succCase = ContourSignal.succCase_D;
                                    this.xflag[ri][rj - 1] = true;
                                    break;
                            }
                            int rri = 0;
                            int rrj = 0;
                            for(l = 0; l < 3; l++){
                                rri = ri + xNear[l];
                                rrj = rj + yNear[l];
                                x1 = this.x[rri];
                                y1 = this.y[rrj];
                                z1 = this.z[rri][rrj];
                                final int rrii = ri + xNear[l + 1];
                                final int rrjj = rj + yNear[l + 1];
                                x2 = this.x[rrii];
                                y2 = this.y[rrjj];
                                z2 = this.z[rrii][rrjj];
                                // System.out.print("["+(ri + xNear[l])+","+(rj + yNear[l])+"] "+" ["+(ri + xNear[l+1])+","+(rj + yNear[l+1])+"] " + l);
                                if(this.checkIntersection(level, z1, z2)){
                                    if(this.equalZ2){
                                        try{
                                            this.xflag[rrii][rrjj - 1] = true;
                                            this.xflag[rrii][rrjj] = true;
                                        }catch(final Exception exc){}
                                    }
                                    c1 = (level - z1) / (z2 - z1);
                                    xc = x1 + (x2 - x1) * c1;
                                    yc = y1 + (y2 - y1) * c1;
                                    contour.addElement((currCPoint = new Point2D.Double(xc, yc)));
                                    ri += xNear[rPoint[l]];
                                    rj += yNear[rPoint[l]];
                                    edgeCase = succCase[l];
                                    break;
                                }
                            }
                            if(l == 3){
                                System.out.println("Errore creazione curva di livello");
                                currCPoint = firstCPoint;
                            }
                        }catch(final Exception exc){
                            if(!(exc instanceof IOException)){
                                // System.out.println("Eccezzione");
                                // Quando una curva di livello esce dalla griglia
                                // si verifica una eccezione che gestisco andando
                                // alla ricerca sul bordo dove rientra la curva
                                // e riprendendo quindi la ricerca dei punti
                                // di contour
                                boolean found = false;
                                int xi, yj;
                                int border;
                                // edge = true;
                                for(border = 0; border < 4 && !found; border++){
                                    switch(edgeCase){
                                        case CASE_B:
                                            yj = this.y.length - 1;
                                            for(xi = ri; xi > 0; xi--){
                                                x2 = this.x[xi];
                                                y2 = this.y[yj];
                                                z2 = this.z[xi][yj];
                                                x1 = this.x[xi - 1];
                                                y1 = this.y[yj];
                                                z1 = this.z[xi - 1][yj];
                                                if(this.checkIntersection(level, z1, z2)){
                                                    found = true;
                                                    ri = xi - 1;
                                                    rj = yj;
                                                    edgeCase = ContourSignal.CASE_C;
                                                    // System.out.println("CASE B Trovata int succ CASE C");
                                                    break;
                                                }
                                            }
                                            // Non ho trovato nessun punto sul lato
                                            // superiore devo cercare un punto sul
                                            // bordo laterale CASE_A devo partire dal primo
                                            // punto a differenza del caso generico in cui
                                            // devo partire dal punto successivo al segmento in cui e'
                                            // stato individuato il punto di uscita della curva di livello
                                            // in esame, valgono considerazioni analoghe per gli altri casi.
                                            if(!found){
                                                // System.out.println("CASE B NON Trovata int succ CASE A");
                                                edgeCase = ContourSignal.CASE_D;
                                                rj = this.y.length - 1;
                                            }
                                            break;
                                        case CASE_A:
                                            xi = this.x.length - 1;
                                            for(yj = rj + 1; yj < this.y.length - 1; yj++){
                                                x1 = this.x[xi];
                                                y1 = this.y[yj];
                                                z1 = this.z[xi][yj];
                                                x2 = this.x[xi];
                                                y2 = this.y[yj + 1];
                                                z2 = this.z[xi][yj + 1];
                                                this.xflag[xi][yj] = true;
                                                if(this.checkIntersection(level, z1, z2)){
                                                    found = true;
                                                    ri = xi;
                                                    rj = yj + 1;
                                                    edgeCase = ContourSignal.CASE_D;
                                                    // System.out.println("CASE A Trovata int succ CASE D");
                                                    break;
                                                }
                                            }
                                            if(!found){
                                                // System.out.println("CASE A NON Trovata int succ CASE C");
                                                edgeCase = ContourSignal.CASE_B;
                                                ri = this.x.length - 1;
                                            }
                                            break;
                                        case CASE_C:
                                            yj = 0;
                                            // for (xi = ri - 1; xi >= 0; xi--)
                                            for(xi = ri + 1; xi < this.x.length - 1; xi++){
                                                x1 = this.x[xi];
                                                y1 = this.y[yj];
                                                z1 = this.z[xi][yj];
                                                x2 = this.x[xi + 1];
                                                y2 = this.y[yj];
                                                z2 = this.z[xi + 1][yj];
                                                if(this.checkIntersection(level, z1, z2)){
                                                    found = true;
                                                    ri = xi;
                                                    rj = yj;
                                                    edgeCase = ContourSignal.CASE_B;
                                                    // System.out.println("CASE C Trovata int succ CASE B");
                                                    break;
                                                }
                                            }
                                            if(!found){
                                                // System.out.println("CASE C NON Trovata int succ CASE D");
                                                edgeCase = ContourSignal.CASE_A;
                                                rj = -1;
                                            }
                                            break;
                                        case CASE_D:
                                            xi = 0;
                                            for(yj = rj - 1; yj > 0; yj--){
                                                x1 = this.x[xi];
                                                y1 = this.y[yj];
                                                z1 = this.z[xi][yj];
                                                x2 = this.x[xi];
                                                y2 = this.y[yj - 1];
                                                z2 = this.z[xi][yj - 1];
                                                this.xflag[xi][yj] = true;
                                                if(this.checkIntersection(level, z1, z2)){
                                                    found = true;
                                                    ri = xi;
                                                    rj = yj - 1;
                                                    edgeCase = ContourSignal.CASE_A;
                                                    // System.out.println("CASE D Trovata int succ CASE A");
                                                    break;
                                                }
                                            }
                                            if(!found){
                                                // System.out.println("CASE D NON Trovata int succ CASE B");
                                                edgeCase = ContourSignal.CASE_C;
                                                ri = -1;
                                            }
                                            break;
                                    }
                                }
                                /*
                                 * Per gestire correttamente le curve di livello che escono
                                 * dalla griglia come curve spezzate devo memorizzare ogni
                                 * singola spezzata separatamente per evitare che in fase
                                 * di plot vengano congiunti con un segmento i punti di
                                 * uscita dalla griglia della curva di livello in esame.
                                 */
                                if(contour.size() >= 2){
                                    contours.addElement(contour);
                                    contour = new Vector<Point2D.Double>();
                                }else{
                                    contour.clear();
                                }
                                c1 = (level - z1) / (z2 - z1);
                                xc = x1 + (x2 - x1) * c1;
                                yc = y1 + (y2 - y1) * c1;
                                contour.addElement((currCPoint = new Point2D.Double(xc, yc)));
                                if(!found && border == 4){
                                    // System.out.println("Completato il bordo");
                                    numIteractions = maxIteractions;
                                }
                            }
                        }
                        // System.out.println("First " + firstPoint );
                        // System.out.println("" + numIteractions + " Curr " + currPoint );
                        // System.out.println("Edge case" + edgeCase );
                        numIteractions++;
                        if(numIteractions > maxIteractions) break;
                    }
                    /* La curva di livello si ritiene conclusa quando
                       si ritorna al punto di partenza
                     */
                    while(!(currCPoint.equals(firstCPoint)));
                    /*
                              if (numIteractions > maxIteractions)
                                System.out.println("Raggiunto numero massimo di iterazioni");
                     */
                    if(contour.size() >= 2){
                        contours.addElement(contour);
                        contour = new Vector<Point2D.Double>();
                    }else{
                        contour.clear();
                    }
                    // if(true) return contours;
                    contourCompleted = true;
                }
            }
        }
        this.xflag = null;
        return contours;
    }

    private void equalCases() {
        if(this.xmax == this.xmin){
            this.xmin -= this.xmax / 10.f;
            this.xmax += this.xmax / 10.f;
        }
        if(this.ymax == this.ymin){
            this.ymin -= this.ymax / 10.f;
            this.ymax += this.ymax / 10.f;
        }
        if(this.zmax == this.zmin){
            this.zmin -= this.zmax / 10.f;
            this.zmax += this.zmax / 10.f;
        }
    }

    public void setMinMax(final float xmin, final float xmax, final float ymin, final float ymax, final float zmin, final float zmax) {
        this.setMinMaxX(xmin, xmax);
        this.setMinMaxY(ymin, ymax);
        this.setMinMaxZ(zmin, zmax);
    }

    public void setMinMaxX(final double xmin, final double xmax) {
        this.xmin = xmin;
        this.xmax = xmax;
    }

    public void setMinMaxY(final double ymin, final double ymax) {
        this.ymin = ymin;
        this.ymax = ymax;
    }

    public void setMinMaxZ(final double zmin, final double zmax) {
        this.zmin = zmin;
        this.zmax = zmax;
    }
}
