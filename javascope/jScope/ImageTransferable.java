package jScope;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.IOException;

class ImageTransferable implements Transferable, ClipboardOwner{
    BufferedImage ri;

    ImageTransferable(BufferedImage img){
        ri = img;
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[]{DataFlavor.imageFlavor};
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return DataFlavor.imageFlavor.equals(flavor);
    }

    @Override
    public Object getTransferData(DataFlavor flavor) throws IOException, UnsupportedFlavorException {
        if(ri == null) return null;
        if(!isDataFlavorSupported(flavor)){ throw new UnsupportedFlavorException(flavor); }
        return ri;
    }

    @Override
    public void lostOwnership(Clipboard clipboard, Transferable contents) {
        /*
        System.out.println ("ImageTransferable lost ownership of "  +clipboard.getName());
        System.out.println ("data: " + contents);
        */
    }
}