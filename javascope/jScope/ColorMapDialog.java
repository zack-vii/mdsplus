package jScope;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;

final public class ColorMapDialog extends JDialog{
    public class ColorPalette extends JPanel{
        static final long serialVersionUID = 4762377065452L;
        Color             colors[];

        ColorPalette(final Color colors[]){
            this.setBorder(BorderFactory.createLoweredBevelBorder());
            this.setColormap(colors);
        }

        @Override
        public void paintComponent(final Graphics g) {
            final Dimension d = this.getSize();
            final float dx = (float)d.width / this.colors.length;
            int x;
            int width;
            for(int i = 0; i < this.colors.length; i++){
                x = (int)(i * dx + 0.5);
                width = (int)(x + dx + 0.5);
                g.setColor(this.colors[i]);
                g.fillRect(x, 0, width, d.height);
            }
            super.paintBorder(g);
        }

        public void setColormap(final Color colors[]) {
            this.colors = colors;
            this.repaint();
        }
    }
    static final long                    serialVersionUID = 476237696543563L;
    JCheckBox                            bitClip;
    JPanel                               bitOptionPanel, colorMapPanel;
    JComboBox                            cmComboBox;
    ColorMap                             colorMap;
    private final Vector<ActionListener> colorMapListener = new Vector<ActionListener>();
    byte                                 colorTables[];
    ColorPalette                         cp;
    boolean                              is16BitImage     = false;
    JTextField                           minVal, maxVal;
    String                               nameColorTables[];
    JButton                              ok, apply, cancel;
    JSlider                              shiftSlider;
    // WaveformEditor weR, weG, weB;
    Waveform                             wave             = null;

    @SuppressWarnings("unchecked")
    /*
      ColorMapDialog(Frame f, Waveform wave)
      {
      //  this(f, wave.getColorMap());
          this.wave = wave;
      }
     */
    ColorMapDialog(final Frame f, String colorPaletteFile){
        super(f, "Color Palette");
        if(colorPaletteFile == null) colorPaletteFile = System.getProperty("user.home") + File.separator + "jScope" + File.separator + "colors1.tbl";
        this.readColorPalette(colorPaletteFile);
        this.getContentPane().setLayout(new GridLayout(3, 1));
        final JPanel pan1 = new JPanel();
        // pan1.setLayout(new GridLayout(2, 1));
        final JPanel pan2 = new JPanel();
        /*
            pan2.add(new JLabel("MIN : "));
            pan2.add(minVal = new JTextField(6));
            pan2.add(new JLabel("MAX : "));
            pan2.add(maxVal = new JTextField(6));
         */
        pan2.add(this.cmComboBox = new JComboBox());
        final int r[] = new int[256];
        final int g[] = new int[256];
        final int b[] = new int[256];
        for(int i = 0; i < this.nameColorTables.length; i++){
            for(int j = 0; j < 256; j++){
                r[j] = 0xFF & this.colorTables[i * (256 * 3) + j];
                g[j] = 0xFF & this.colorTables[i * (256 * 3) + 256 + j];
                b[j] = 0xFF & this.colorTables[i * (256 * 3) + 256 * 2 + j];
            }
            this.cmComboBox.addItem(new ColorMap(this.nameColorTables[i], r, g, b));
        }
        this.colorMap = (ColorMap)this.cmComboBox.getSelectedItem();
        this.cmComboBox.addItemListener(ev -> {
            final ColorMap cm = (ColorMap)ev.getItem();
            ColorMapDialog.this.cp.setColormap(cm.colors);
            ColorMapDialog.this.wave.applyColorModel(cm);
        });
        if(this.colorMap == null) this.colorMap = new ColorMap();
        this.cp = new ColorPalette(this.colorMap.colors);
        this.getContentPane().add(this.cp);
        pan1.add(pan2);
        this.bitOptionPanel = new JPanel();
        this.bitOptionPanel.setBorder(BorderFactory.createTitledBorder("16 bit  Option"));
        this.bitOptionPanel.add(this.shiftSlider = new JSlider(-8, 8, 0));
        this.shiftSlider.setName("Bit Offset");
        this.shiftSlider.setMajorTickSpacing(1);
        this.shiftSlider.setPaintTicks(true);
        this.shiftSlider.setPaintLabels(true);
        this.shiftSlider.setSnapToTicks(true);
        this.shiftSlider.addChangeListener(e -> {
            final JSlider source = (JSlider)e.getSource();
            if(!source.getValueIsAdjusting()){
                ColorMapDialog.this.wave.setFrameBitShift(ColorMapDialog.this.shiftSlider.getValue(), ColorMapDialog.this.bitClip.isSelected());
            }
        });
        this.bitOptionPanel.add(this.bitClip = new JCheckBox("Bit Clip"));
        this.bitClip.addItemListener(e -> ColorMapDialog.this.wave.setFrameBitShift(ColorMapDialog.this.shiftSlider.getValue(), ColorMapDialog.this.bitClip.isSelected()));
        final JPanel pan4 = new JPanel();
        pan4.add(this.ok = new JButton("Ok"));
        this.ok.addActionListener(e -> {
            // if (ColorMapDialog.this.wave.IsImage())
            {
                final ColorMap cm = (ColorMap)ColorMapDialog.this.cmComboBox.getSelectedItem();
                if(ColorMapDialog.this.is16BitImage){
                    cm.bitClip = ColorMapDialog.this.bitClip.isSelected();
                    cm.bitShift = ColorMapDialog.this.shiftSlider.getValue();
                }
                ColorMapDialog.this.wave.setColorMap(cm);
                ColorMapDialog.this.setVisible(false);
            }
        });
        pan4.add(this.cancel = new JButton("Cancel"));
        this.cancel.addActionListener(e -> {
            // if (ColorMapDialog.this.wave.IsImage())
            {
                ColorMapDialog.this.wave.setColorMap(ColorMapDialog.this.colorMap);
                ColorMapDialog.this.setVisible(false);
                if(ColorMapDialog.this.is16BitImage){
                    ColorMapDialog.this.bitClip.setSelected(ColorMapDialog.this.colorMap.bitClip);
                    ColorMapDialog.this.shiftSlider.setValue(ColorMapDialog.this.colorMap.bitShift);
                }
            }
        });
        this.getContentPane().add(pan1);
        // getContentPane().add(bitOptionPanel);
        this.getContentPane().add(pan4);
        this.pack();
        this.setSize(330, 350);
    }

    public void addColorMapListener(final ActionListener l) {
        this.colorMapListener.addElement(l);
    }

    public ColorMap getColorMap(final String name) {
        for(int i = 0; i < this.cmComboBox.getItemCount(); i++){
            final ColorMap cm = (ColorMap)this.cmComboBox.getItemAt(i);
            if(cm.name.equals(name)) return cm;
        }
        return new ColorMap();
    }

    public void processActionEvents(final ActionEvent avtionEvent) {
        for(int i = 0; i < this.colorMapListener.size(); i++)
            this.colorMapListener.elementAt(i).actionPerformed(avtionEvent);
    }

    public void readColorPalette(final String cmap) {
        DataInputStream dis;
        try{
            try{
                final FileInputStream bin = new FileInputStream(new File(cmap));
                dis = new DataInputStream(bin);
            }catch(final IOException exc){
                final InputStream pis = this.getClass().getClassLoader().getResourceAsStream("colors1.tbl");
                dis = new DataInputStream(pis);
            }
            final byte nColorTables = dis.readByte();
            this.nameColorTables = new String[nColorTables];
            final byte name[] = new byte[32];
            this.colorTables = new byte[nColorTables * 3 * 256];
            dis.readFully(this.colorTables);
            for(int i = 0; i < nColorTables; i++){
                dis.readFully(name);
                this.nameColorTables[i] = (new String(name)).trim();
            }
            dis.close();
        }catch(final Exception exc){
            // System.out.println("Color map exception : " + exc);
            this.nameColorTables = new String[0];
            this.colorTables = new byte[0];
        }
    }

    public void removeMapListener(final ActionListener l) {
        this.colorMapListener.remove(l);
    }

    public void setWave(final Waveform wave) {
        this.wave = wave;
        this.colorMap = wave.getColorMap();
        this.cmComboBox.setSelectedItem(this.colorMap);
        // if( wave.frames != null && wave.frames.frame_type.length > 0 && wave.frames.frame_type[0] == FrameData.BITMAP_IMAGE_16 )
        if(wave.frames != null && wave.frames.getFrameType() == FrameData.BITMAP_IMAGE_16){
            if(!this.is16BitImage){
                this.getContentPane().setLayout(new GridLayout(4, 1));
                this.getContentPane().add(this.bitOptionPanel, 2);
                this.setSize(330, 350);
            }
            this.is16BitImage = true;
            this.shiftSlider.setValue(this.colorMap.bitShift);
            this.bitClip.setSelected(this.colorMap.bitClip);
        }else{
            this.is16BitImage = false;
            this.getContentPane().remove(this.bitOptionPanel);
            this.getContentPane().setLayout(new GridLayout(3, 1));
            this.setSize(330, 250);
        }
    }
}
