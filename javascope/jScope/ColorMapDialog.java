package jScope;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;

import java.io.*;

final public class ColorMapDialog extends JDialog
{
    static final long serialVersionUID = 476237696543563L;
    private Vector<ActionListener> colorMapListener = new Vector<ActionListener>();
    ColorMap colorMap;
    ColorPalette cp;
    JComboBox cmComboBox;

    JTextField minVal, maxVal;
    JButton ok, apply, cancel;
    JSlider shiftSlider;
    JCheckBox bitClip;
    JPanel bitOptionPanel,colorMapPanel;
    boolean is16BitImage = false;
//WaveformEditor weR, weG, weB;
    Waveform wave = null;

    String nameColorTables[];
    byte colorTables[];

    public class ColorPalette
        extends JPanel
    {
        static final long serialVersionUID = 4762377065452L;
        Color colors[];

        ColorPalette(Color colors[])
        {
            this.setBorder(BorderFactory.createLoweredBevelBorder());
            setColormap(colors);
        }

        public void setColormap(Color colors[])
        {
            this.colors = colors;
            repaint();
        }

        public void paintComponent(Graphics g)
        {
            Dimension d = getSize();
            float dx = (float) d.width / colors.length;
            int x;
            int width;

            for (int i = 0; i < colors.length; i++)
            {
                x = (int) (i * dx + 0.5);
                width = (int) (x + dx + 0.5);
                g.setColor(colors[i]);
                g.fillRect(x, 0, width, d.height);
            }
            super.paintBorder(g);
        }

    }

    /*
      ColorMapDialog(Frame f, Waveform wave)
      {
      //  this(f, wave.getColorMap());
          this.wave = wave;
      }
     */

    ColorMapDialog(Frame f, String colorPaletteFile)
    {
        super(f, "Color Palette");

        if(colorPaletteFile == null)
            colorPaletteFile = System.getProperty("user.home") + File.separator + "jScope" + File.separator + "colors1.tbl";
        readColorPalette(colorPaletteFile);

        getContentPane().setLayout(new GridLayout(3, 1));

        JPanel pan1 = new JPanel();
//        pan1.setLayout(new GridLayout(2, 1));

        JPanel pan2 = new JPanel();

        /*
            pan2.add(new JLabel("MIN : "));
            pan2.add(minVal = new JTextField(6));
            pan2.add(new JLabel("MAX : "));
            pan2.add(maxVal = new JTextField(6));
         */

        pan2.add(cmComboBox = new JComboBox());
        int r[] = new int[256];
        int g[] = new int[256];
        int b[] = new int[256];
        int colorMapIdx = 0;
        for (int i = 0; i < nameColorTables.length; i++)
        {
            for (int j = 0; j < 256; j++)
            {
                r[j] = 0xFF & this.colorTables[i * (256 * 3) + j];
                g[j] = 0xFF & this.colorTables[i * (256 * 3) + 256 + j];
                b[j] = 0xFF & this.colorTables[i * (256 * 3) + 256 * 2 + j];
            }
            cmComboBox.addItem(new ColorMap(nameColorTables[i], r, g, b));
        }
        colorMap = (ColorMap)cmComboBox.getSelectedItem();
        cmComboBox.addItemListener(new ItemListener()
        {
            public void itemStateChanged(ItemEvent ev)
            {
                ColorMap cm = (ColorMap)ev.getItem();
                cp.setColormap( cm.colors );
                ColorMapDialog.this.wave.applyColorModel( cm );
            }
        });
        if(colorMap == null)
            colorMap = new ColorMap();
        cp = new ColorPalette(colorMap.colors);
        getContentPane().add(cp);
        pan1.add(pan2);

        bitOptionPanel = new JPanel();
        bitOptionPanel.setBorder(BorderFactory.createTitledBorder("16 bit  Option"));
        bitOptionPanel.add(shiftSlider = new JSlider(-8, 8, 0) );
        shiftSlider.setName("Bit Offset");
        shiftSlider.setMajorTickSpacing(1);
        shiftSlider.setPaintTicks(true);
        shiftSlider.setPaintLabels(true);
        shiftSlider.setSnapToTicks(true);


        shiftSlider.addChangeListener(new ChangeListener()
        {
            public void stateChanged(ChangeEvent e)
            {
                JSlider source = (JSlider)e.getSource();
                if (!source.getValueIsAdjusting()) {
                    ColorMapDialog.this.wave.setFrameBitShift(shiftSlider.getValue(), bitClip.isSelected());
                }
            }
        });

        bitOptionPanel.add(bitClip = new JCheckBox("Bit Clip"));
        bitClip.addItemListener(new ItemListener()
        {
            public void itemStateChanged(ItemEvent e)
            {
                ColorMapDialog.this.wave.setFrameBitShift(shiftSlider.getValue(), bitClip.isSelected());
            }
        });

        JPanel pan4 = new JPanel();
        pan4.add(ok = new JButton("Ok"));
        ok.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
//                if (ColorMapDialog.this.wave.IsImage())
                {
                    ColorMap cm = (ColorMap)cmComboBox.getSelectedItem();
                    if(is16BitImage)
                    {
                        cm.bitClip = bitClip.isSelected();
                        cm.bitShift = shiftSlider.getValue();
                    }
                    ColorMapDialog.this.wave.setColorMap(cm);
                    ColorMapDialog.this.setVisible(false);
                }
            }
        });

        pan4.add(cancel = new JButton("Cancel"));
        cancel.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
//                if (ColorMapDialog.this.wave.IsImage())
                {
                    ColorMapDialog.this.wave.setColorMap(colorMap);
                    ColorMapDialog.this.setVisible(false);
                    if(is16BitImage)
                    {
                        bitClip.setSelected(colorMap.bitClip);
                        shiftSlider.setValue(colorMap.bitShift);
                    }

                }
            }
        });

        getContentPane().add(pan1);
//        getContentPane().add(bitOptionPanel);
        getContentPane().add(pan4);

        pack();
        setSize(330, 350);
    }

    public ColorMap getColorMap(String name)
    {
        for(int i=0; i < cmComboBox.getItemCount(); i++)
        {
            ColorMap cm = (ColorMap)cmComboBox.getItemAt(i);
            if(cm.name.equals(name))
                return cm;
        }
        return new ColorMap();
    }

    public void setWave(Waveform wave)
    {
        this.wave = wave;
        colorMap = wave.getColorMap();
        cmComboBox.setSelectedItem(colorMap);

 //       if( wave.frames != null && wave.frames.frame_type.length > 0 && wave.frames.frame_type[0] == FrameData.BITMAP_IMAGE_16 )
       if( wave.frames != null && wave.frames.getFrameType() == FrameData.BITMAP_IMAGE_16 )
        {
            if(!is16BitImage)
            {
                getContentPane().setLayout(new GridLayout(4, 1));
                getContentPane().add(bitOptionPanel, 2);
                setSize(330, 350);

            }
            is16BitImage = true;
            shiftSlider.setValue(colorMap.bitShift);
            bitClip.setSelected(colorMap.bitClip);
        }
        else
        {
            is16BitImage = false;
            getContentPane().remove(bitOptionPanel);
            getContentPane().setLayout(new GridLayout(3, 1));
            setSize(330, 250);
        }
    }

    public void addColorMapListener(ActionListener l)
    {
        colorMapListener.addElement(l);
    }

    public void removeMapListener(ActionListener l)
    {
        colorMapListener.remove(l);
    }

    public void processActionEvents(ActionEvent avtionEvent)
    {
        for (int i = 0; i < colorMapListener.size(); i++)
            colorMapListener.elementAt(i).actionPerformed(avtionEvent);
    }

    public void readColorPalette(String cmap)
    {
        DataInputStream dis;

        try
        {
            try
            {
              FileInputStream bin = new FileInputStream(new File(cmap));
              dis = new DataInputStream(bin);
            }
            catch (IOException exc)
            {
              InputStream pis = getClass().getClassLoader().getResourceAsStream("colors1.tbl");
              dis = new DataInputStream(pis);
            }

            byte nColorTables = dis.readByte();

            nameColorTables = new String[nColorTables];
            byte name[] = new byte[32];

            colorTables = new byte[nColorTables * 3 * 256];
            dis.readFully(colorTables);

            for (int i = 0; i < nColorTables; i++)
            {
                dis.readFully(name);
                nameColorTables[i] = (new String(name)).trim();
            }

            dis.close();
        }
        catch(Exception exc)
        {
            //System.out.println("Color map exception : " + exc);
            nameColorTables = new String[0];
            colorTables = new byte[0];
        }
    }

}
