import javax.swing.JFrame;

/** This class is used only to record the jTraverser frame and to communicate it to devices */
class FrameRepository{
    static JFrame frame = null;

    static void setFrame(final JFrame frame) {
        FrameRepository.frame = frame;
    }
}
