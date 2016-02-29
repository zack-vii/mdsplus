package jScope;

/* $Id$ */
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.security.AccessControlException;
import java.util.StringTokenizer;
import javax.swing.ButtonGroup;
import javax.swing.JApplet;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

public class WaveDisplay extends JApplet implements WaveformListener{
    static final long serialVersionUID = 476443678461L;

    protected static String getParameterValue(final String context, final String param) {
        String value = null;
        final StringTokenizer st = new StringTokenizer(context);
        while(st.hasMoreTokens()){
            value = st.nextToken();
            if(value.equals(param)){
                if(st.nextToken().equals("=")){
                    value = st.nextToken();
                    break;
                }
                return null;
            }
            value = null;
        }
        return value;
    }

    protected static boolean translateToBoolean(final String value) {
        return(value.toUpperCase().equals("TRUE"));
    }
    ProfileDialog profDialog;
    JTextField    shot_txt;
    MultiWaveform w;
    WavePopup     wave_popup;

    public WaveDisplay(){
        final JPanel panel = new JPanel();
        this.w = new MultiWaveform();
        this.w.setWaveInterface(new WaveInterface(this.w));
        this.w.addWaveformListener(this);
        this.setBackground(Color.lightGray);
        this.profDialog = new ProfileDialog(null, null);
        this.wave_popup = new WavePopup(new SetupWaveformParams(null, "Waveform Params"), this.profDialog);
        this.getContentPane().add(this.wave_popup);
        panel.addMouseListener(new MouseAdapter(){
            @Override
            public void mousePressed(final MouseEvent e) {
                final Waveform w = (Waveform)e.getSource();
                if(WaveDisplay.this.wave_popup != null && w.GetMode() != Waveform.MODE_COPY){
                    WaveDisplay.this.wave_popup.Show(w, e.getX(), e.getY(), 0, 0);
                }
            }
        });
        final JRadioButton point = new JRadioButton("Point", false);
        point.addItemListener(new ItemListener(){
            @Override
            public void itemStateChanged(final ItemEvent e) {
                WaveDisplay.this.w.SetMode(Waveform.MODE_POINT);
            }
        });
        final JRadioButton zoom = new JRadioButton("Zoom", true);
        zoom.addItemListener(new ItemListener(){
            @Override
            public void itemStateChanged(final ItemEvent e) {
                WaveDisplay.this.w.SetMode(Waveform.MODE_ZOOM);
            }
        });
        final JRadioButton pan = new JRadioButton("Pan", false);
        pan.addItemListener(new ItemListener(){
            @Override
            public void itemStateChanged(final ItemEvent e) {
                WaveDisplay.this.w.SetMode(Waveform.MODE_PAN);
            }
        });
        final ButtonGroup pointer_mode = new ButtonGroup();
        pointer_mode.add(point);
        pointer_mode.add(zoom);
        pointer_mode.add(pan);
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new FlowLayout(FlowLayout.LEFT, 1, 3));
        panel1.add(point);
        panel1.add(zoom);
        panel1.add(pan);
        this.w.SetGridMode(Grid.IS_DOTTED, true, true);
        panel.setLayout(new BorderLayout());
        panel.add(this.w, BorderLayout.CENTER);
        this.getContentPane().setLayout(new BorderLayout());
        this.getContentPane().add(panel, BorderLayout.CENTER);
        this.getContentPane().add(panel1, BorderLayout.SOUTH);
    }

    public void addSignal(final String signalParams, final String globalAutentication, final boolean image) {
        String url = null, color, marker, name, title;
        String signalAutentication, autentication;
        if(signalParams == null) return;
        try{
            if(!image){
                signalAutentication = WaveDisplay.getParameterValue(signalParams, "autentication");
                if(signalAutentication != null){
                    autentication = signalAutentication;
                }else{
                    autentication = globalAutentication;
                }
                url = WaveDisplay.getParameterValue(signalParams, "url");
                final DataAccess da = DataAccessURL.getDataAccess(url);
                if(da != null){
                    da.setPassword(autentication);
                    da.setProvider(url);
                    final WaveInterface wi = this.w.getWaveInterface();
                    if(wi.signals != null) wi.Erase();
                    wi.SetDataProvider(da.getDataProvider());
                    wi.setExperiment(da.getExperiment());
                    System.out.println("Signal Name : " + da.getSignalName());
                    System.out.println("Shots : " + da.getShot());
                    wi.AddSignal(da.getSignalName());
                    wi.setShotArray(da.getShot());
                    if(wi.StartEvaluate()) wi.EvaluateOthers();
                    Signal s;
                    if(wi.signals != null && (s = wi.signals[0]) != null){
                        color = WaveDisplay.getParameterValue(signalParams, "color");
                        if(color != null){
                            s.setColor(new Color(Integer.decode(color).intValue()));
                        }
                        marker = WaveDisplay.getParameterValue(signalParams, "marker");
                        s.setMarker(marker);
                        name = WaveDisplay.getParameterValue(signalParams, "name");
                        if(name != null){
                            s.setName(name);
                        }else{
                            s.setName(wi.in_y[0]);
                        }
                        title = WaveDisplay.getParameterValue(signalParams, "title");
                        if(title != null){
                            this.w.SetTitle(title);
                        }
                    }else{
                        JOptionPane.showMessageDialog(this, "Evaluation Error : " + wi.getErrorTitle(false), "alert", JOptionPane.ERROR_MESSAGE);
                    }
                    this.w.Update(wi.signals);
                }
            }else{
                String aspect_ratio, horizontal_flip, vertical_flip;
                url = WaveDisplay.getParameterValue(signalParams, "url");
                final Frames f = new Frames();
                horizontal_flip = WaveDisplay.getParameterValue(signalParams, "h_flip");
                if(horizontal_flip != null && horizontal_flip.toLowerCase().equals("true")){
                    f.setHorizontalFlip(true);
                }
                vertical_flip = WaveDisplay.getParameterValue(signalParams, "v_flip");
                if(vertical_flip != null && vertical_flip.toLowerCase().equals("true")){
                    f.setVerticalFlip(true);
                }
                DataAccessURL.getImages(url, f);
                if(f != null){
                    name = WaveDisplay.getParameterValue(signalParams, "name");
                    if(name != null){
                        f.setName(name);
                    }
                    aspect_ratio = WaveDisplay.getParameterValue(signalParams, "ratio");
                    if(aspect_ratio != null && aspect_ratio.toLowerCase().equals("false")){
                        f.setAspectRatio(false);
                        this.w.UpdateImage(f);
                    }
                }
            }
        }catch(final Exception e){
            if(e instanceof AccessControlException){
                JOptionPane.showMessageDialog(this, e.toString() + "\n url " + url + "\nUse policytool.exe in  JDK or JRE installation directory to add socket access permission\n", "alert", JOptionPane.ERROR_MESSAGE);
            }else{
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, e.toString(), "alert", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // public void addSignals( String url, boolean isImage )
    public void addSignals() {
        final String url = "mds://150.178.3.242/a/14000/\\emra_it";
        final boolean isImage = false;
        try{
            System.out.println("Add signal : " + url);
            final DataAccess da = DataAccessURL.getDataAccess(url);
            if(da != null){
                // da.setPassword(autentication);
                da.setProvider(url);
                final WaveInterface wi = this.w.getWaveInterface();
                wi.SetDataProvider(da.getDataProvider());
                wi.setExperiment(da.getExperiment());
                System.out.println("Signal Name : " + da.getSignalName());
                if(!isImage){
                    wi.AddSignal(da.getSignalName());
                    System.out.println("Shots : " + da.getShot());
                    wi.setShotArray(da.getShot());
                    if(wi.StartEvaluate()) wi.EvaluateOthers();
                    if(wi.signals != null && (wi.signals[0]) != null){
                        this.w.Update(wi.signals);
                    }else{
                        JOptionPane.showMessageDialog(this, "Evaluation Error : " + wi.getErrorTitle(false), "alert", JOptionPane.ERROR_MESSAGE);
                    }
                }else{
                    final Frames f = new Frames();
                    DataAccessURL.getImages(url, f);
                    if(f != null){
                        this.w.UpdateImage(f);
                    }
                }
            }
        }catch(final Exception e){
            if(e instanceof AccessControlException){
                JOptionPane.showMessageDialog(this, e.toString() + "\n url " + url + "\nUse policytool.exe in  JDK or JRE installation directory to add socket access permission\n", "alert", JOptionPane.ERROR_MESSAGE);
            }else{
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, e.toString(), "alert", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    @Override
    public void destroy() {}

    private void getSignalsParameter() {
        String sig_param;
        String global_autentication;
        boolean image = false;
        global_autentication = this.getParameter("AUTENTICATION");
        if((sig_param = this.getParameter("SIGNAL")) == null){
            sig_param = this.getParameter("IMAGE");
            image = true;
        }
        this.addSignal(sig_param, global_autentication, image);
    }

    @Override
    public void init() {}

    public void print_xxx(final Graphics g) {
        final Dimension dim = new Dimension();
        /*
         * Dimension applet_size = getSize(); if(print_scaling != 100) { System.out.println("Proporzionale " + this.getBounds()); float ratio = (float)applet_size.width/applet_size.height; dim.width = (int)(applet_size.width/100.* print_scaling);
         * dim.height = (int)(ratio * dim.width); } else { dim.width = 530; dim.height = 816; }
         */
        dim.width = 530;
        dim.height = 816;
        this.w.paint(g, dim, Waveform.PRINT);
    }

    @Override
    public void processWaveformEvent(final WaveformEvent e) {
        String s = null;
        final WaveformEvent we = e;
        final MultiWaveform w = (MultiWaveform)we.getSource();
        final WaveInterface wi = w.getWaveInterface();
        final int we_id = we.getID();
        switch(we_id){
            case WaveformEvent.MEASURE_UPDATE:
            case WaveformEvent.POINT_UPDATE:
            case WaveformEvent.POINT_IMAGE_UPDATE:
                s = we.toString();
                if(wi.shots != null){
                    s = (s + " Expr : " + w.getSignalName(we.signal_idx) + " Shot = " + wi.shots[we.signal_idx]);
                }else{
                    s = (s + " Expr : " + w.getSignalName(we.signal_idx));
                }
                this.showStatus(s);
                break;
            case WaveformEvent.STATUS_INFO:
                s = we.status_info;
                this.showStatus(s);
                break;
            default:
                this.showStatus(we.toString());
        }
    }

    public void resetSignal() {
        final WaveInterface wi = this.w.getWaveInterface();
        if(wi.signals != null){
            wi.Erase();
        }
        this.w.Erase();
    }

    @Override
    public void start() {
        this.getSignalsParameter();
    }

    @Override
    public void stop() {
        DataAccessURL.close();
    }
}
