import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import jScope.Waveform;

public class DeviceWaveDisplay extends DeviceComponent{
    private static final long serialVersionUID = 7336737112620278118L;
    protected Data            oldData;
    protected int             prefHeight       = 200;
    protected Waveform        wave;
    float                     x[]              = null, y[] = null;

    public DeviceWaveDisplay(){}

    @Override
    protected void displayData(final Data data, final boolean is_on) {
        try{
            final Data xData = this.subtree.evaluateData(Data.fromExpr("DIM_OF(" + data + ")"), 0);
            final Data yData = this.subtree.evaluateData(data, 0);
            this.x = xData.getFloatArray();
            this.y = yData.getFloatArray();
            this.wave.Update(this.x, this.y);
        }catch(final Exception exc){}
    }

    @Override
    protected Data getData() {
        return this.oldData;
    }

    public int getPrefHeight() {
        return this.prefHeight;
    }

    @Override
    protected boolean getState() {
        return true;
    }

    @Override
    protected void initializeData(final Data data, final boolean is_on) {
        this.oldData = data;
        this.setLayout(new BorderLayout());
        this.wave = new Waveform();
        this.wave.setPreferredSize(new Dimension(300, 200));
        this.add(this.wave, "Center");
        this.displayData(data, is_on);
    }

    @Override
    public boolean isDataChanged() {
        return false;
    }

    @Override
    void postApply() {
        this.displayData(this.oldData, true);
    }

    @Override
    public void print(final Graphics g) {
        this.wave.paintComponent(g);
    }

    @Override
    public void reset() {}

    @Override
    public void setEnabled(final boolean state) {}

    public void setPrefHeight(final int prefHeight) {
        this.prefHeight = prefHeight;
    }
}
