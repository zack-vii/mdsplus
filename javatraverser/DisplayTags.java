// package jTraverser;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class DisplayTags extends NodeEditor implements ActionListener{
    private static final long serialVersionUID = 845904969809735591L;
    JLabel                    tagsLabel;
    JPanel                    tagsPanel;

    public DisplayTags(){
        this.setLayout(new BorderLayout());
        this.tagsPanel = new JPanel();
        this.tagsPanel.add(this.tagsLabel = new JLabel());
        this.add(this.tagsPanel, "North");
        final JPanel jp1 = new JPanel();
        final JButton cancel = new JButton("Cancel");
        jp1.add(cancel);
        this.add(jp1, "South");
        cancel.addActionListener(this);
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        this.frame.dispose();
    }

    @Override
    public void setNode(final Node _node) {
        String tags[] = new String[0];
        this.node = _node;
        this.frame.setTitle("Display Node Tags");
        try{
            tags = this.node.getTags();
        }catch(final Exception e){
            System.out.println("Error retieving Tags");
            return;
        }
        String tagNames = "";
        if(tags == null || tags.length == 0) tagNames = "No Tags";
        else{
            for(int i = 0; i < tags.length; i++){
                tagNames += tags[i];
                if(i < tags.length - 1) tagNames += ", ";
            }
        }
        this.tagsLabel.setText(tagNames);
    }
}