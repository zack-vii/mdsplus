package jScope;

import java.awt.Color;
import java.awt.image.IndexColorModel;

final public class ColorMap{
    private static int[] getValues(final int nVal, final float p[], final float v[]) {
        final int out[] = new int[nVal];
        final float dx = (p[p.length - 1] - p[0]) / (nVal - 1);
        float val = 0;
        int idx = 0;
        float c1 = (v[0] - v[1]) / (p[0] - p[1]);
        for(int i = 0; i < nVal; i++, c1 = (v[idx] - v[idx + 1]) / (p[idx] - p[idx + 1])){
            if(p[idx] == p[idx + 1]){
                idx++;
                i--;
                continue;
            }
            val = i * dx;
            if(val > p[idx + 1]){
                idx++;
                i--;
                continue;
            }
            out[i] = (int)(c1 * val - p[idx] * c1 + v[idx]);
            if(out[i] > 255) out[i] = 255;
            else if(out[i] < 0) out[i] = 0;
        }
        return out;
    }
    byte            b[]             = new byte[256];
    public boolean  bitClip         = false;
    public int      bitShift        = 0;
    Color           colors[];
    byte            g[]             = new byte[256];
    IndexColorModel indexColorModel = null;
    float           max;
    float           min;
    public String   name            = "unnamed";
    int             numColors       = 0;
    float           pB[];
    float           pG[];
    float           pR[];
    byte            r[]             = new byte[256];
    float           vB[];
    float           vG[];
    float           vR[];

    public ColorMap(){
        this.name = "gray";
        this.numColors = 256;
        this.colors = new Color[256];
        for(int i = 0; i < this.numColors; i++){
            this.r[i] = (byte)(0xFF & i);
            this.g[i] = (byte)(0xFF & i);
            this.b[i] = (byte)(0xFF & i);
            this.colors[i] = new Color(i, i, i);
        }
    }

    ColorMap(final String name, final int r[], final int g[], final int b[]){
        this.name = name;
        this.numColors = 256;
        this.colors = new Color[256];
        for(int i = 0; i < this.numColors; i++){
            this.r[i] = (byte)(0xFF & r[i]);
            this.g[i] = (byte)(0xFF & g[i]);
            this.b[i] = (byte)(0xFF & b[i]);
            this.colors[i] = new Color(r[i], g[i], b[i]);
        }
    }

    public void computeColorMap() {
        int r[], g[], b[];
        this.colors = new Color[this.numColors];
        r = ColorMap.getValues(this.numColors, this.pR, this.vR);
        g = ColorMap.getValues(this.numColors, this.pG, this.vG);
        b = ColorMap.getValues(this.numColors, this.pB, this.vB);
        for(int i = 0; i < this.numColors; i++){
            this.colors[i] = new Color(r[i], g[i], b[i]);
        }
    }

    public void createColorMap(final int numColors, final int numPoints, final float min, final float max) {
        this.max = max;
        this.min = min;
        this.numColors = numColors;
        final float delta = max - min;
        this.pR = new float[5];
        this.pR[0] = min;
        this.pR[1] = min + delta * 0.2f;
        this.pR[2] = min + delta * 0.6f;
        this.pR[3] = min + delta * 0.8f;
        this.pR[4] = min + delta * 1.f;
        this.vR = new float[5];
        this.vR[0] = 255;
        this.vR[1] = 0;
        this.vR[2] = 0;
        this.vR[3] = 255;
        this.vR[4] = 255;
        this.pG = new float[5];
        this.pG[0] = min;
        this.pG[1] = min + delta * 0.2f;
        this.pG[2] = min + delta * 0.4f;
        this.pG[3] = min + delta * 0.8f;
        this.pG[4] = min + delta * 1.f;
        this.vG = new float[5];
        this.vG[0] = 0;
        this.vG[1] = 0;
        this.vG[2] = 255;
        this.vG[3] = 255;
        this.vG[4] = 0;
        this.pB = new float[4];
        this.pB[0] = min;
        this.pB[1] = min + delta * 0.4f;
        this.pB[2] = min + delta * 0.6f;
        this.pB[3] = min + delta * 1.f;
        this.vB = new float[4];
        this.vB[0] = 255;
        this.vB[1] = 255;
        this.vB[2] = 0;
        this.vB[3] = 0;
        this.computeColorMap();
    }

    public byte[] getBlueIntValues() {
        int c[];
        final byte b[] = new byte[this.numColors];
        c = ColorMap.getValues(this.numColors, this.pB, this.vB);
        for(int i = 0; i < this.numColors; b[i] = (byte)c[i], i++);
        return b;
    }

    public float[] getBluePoints() {
        return this.pB;
    }

    public float[] getBlueValues() {
        return this.vB;
    }

    public Color getColor(final float val) {
        if(val < this.min) return this.colors[0];
        else if(val > this.max) return this.colors[this.colors.length - 1];
        final int idx = (int)((val - this.max) / (this.max - this.min) + 1) * (this.colors.length - 1);
        return this.colors[idx];
    }

    public Color getColor(final float val, final float min, final float max) {
        final int idx = (int)((val - min) / (max - min) * (this.numColors - 1));
        return this.colors[idx];
    }

    public Color[] getColors() {
        return this.colors;
    }

    public byte[] getGreenIntValues() {
        int c[];
        final byte b[] = new byte[this.numColors];
        c = ColorMap.getValues(this.numColors, this.pG, this.vG);
        for(int i = 0; i < this.numColors; b[i] = (byte)c[i], i++);
        return b;
    }

    public float[] getGreenPoints() {
        return this.pG;
    }

    public float[] getGreenValues() {
        return this.vG;
    }

    public IndexColorModel getIndexColorModel(final int numBit) {
        this.indexColorModel = new IndexColorModel(numBit, this.numColors, this.r, this.g, this.b);
        return this.indexColorModel;
    }

    public float getMax() {
        return this.max;
    }

    public float getMin() {
        return this.min;
    }

    public byte[] getRedIntValues() {
        int c[];
        final byte b[] = new byte[this.numColors];
        c = ColorMap.getValues(this.numColors, this.pR, this.vR);
        for(int i = 0; i < this.numColors; b[i] = (byte)c[i], i++);
        return b;
    }

    public float[] getRedPoints() {
        return this.pR;
    }

    public float[] getRedValues() {
        return this.vR;
    }

    public void setBlueParam(final float p[], final float v[]) {
        this.pB = p;
        this.vB = v;
        int b[];
        b = ColorMap.getValues(this.numColors, p, v);
        for(int i = 0; i < this.numColors; i++){
            this.colors[i] = new Color(this.colors[i].getRed(), this.colors[i].getGreen(), b[i]);
        }
    }

    public void setGreenParam(final float p[], final float v[]) {
        this.pG = p;
        this.vG = v;
        int g[];
        g = ColorMap.getValues(this.numColors, p, v);
        for(int i = 0; i < this.numColors; i++){
            this.colors[i] = new Color(this.colors[i].getRed(), g[i], this.colors[i].getBlue());
        }
    }

    public void setMax(final float max) {
        this.max = max;
    }

    public void setMin(final float min) {
        this.min = min;
    }

    public void setRedParam(final float p[], final float v[]) {
        this.pR = p;
        this.vR = v;
        int r[];
        r = ColorMap.getValues(this.numColors, p, v);
        for(int i = 0; i < this.numColors; i++){
            this.colors[i] = new Color(r[i], this.colors[i].getGreen(), this.colors[i].getBlue());
        }
    }

    @Override
    public String toString() {
        return this.name;
    }
}