
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;


public class TextAreaOutputStream extends OutputStream {

    JTextArea textArea;
    FileOutputStream fos;
    boolean autoScroll   = true;
    boolean writeEnabled = true;

    public TextAreaOutputStream(JTextArea textArea) {
        this.textArea = textArea;
    }

    private int getLength() {
        return textArea.getDocument().getLength();
    }
    public synchronized void write(int b) throws IOException {
        if (!writeEnabled) {
            return;
        }
        if (fos != null) {
            fos.write(b);
            return;
        }
        textArea.append(""+((char) b));
        if (autoScroll) {
            scroll();
        }
    }

    public synchronized void write(byte b[], int off, int len) throws IOException {
        if (!writeEnabled) {
            return;
        }
        if (fos != null) {
            fos.write(b,off,len);
            return;
        }
        String s = new String(b, off, len);
        textArea.append(s);
        if (autoScroll) {
            scroll();
        }
    }

    void scroll() {
        int length = getLength();
        if (length > 100000) {
            textArea.replaceRange("", 0, length-100000);
        }
        textArea.setCaretPosition(getLength());
    }

    synchronized void setAutoScroll(boolean set) {
        autoScroll = set;
        if (set) {
            scroll();
        }
    }

    synchronized void setWriteEnabled(boolean set) {
        writeEnabled = set;
    }

    synchronized void setLogFile(FileOutputStream fos) throws IOException {
        if (this.fos != null && fos == null) {
            this.fos.close();
        }
        this.fos = fos;
    }

    synchronized void clearLogFile(String name) throws IOException {
        boolean open = this.fos != null;
        if (open) {
            this.fos.close();
        }
        (new File(name)).delete();
        if (open) {
            this.fos = new FileOutputStream(name);
        }
    }
}


