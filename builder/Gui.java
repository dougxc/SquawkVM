
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;

public class Gui extends JPanel implements WindowListener, ComponentListener {

    // holds the command  args (aprt from -gui) that were passed to Build
    static Build protoBuild;

    BrazilShell    southeast = new BrazilShell();
    AppsTabbedPane southwest = new AppsTabbedPane();
    GuiShell north = new BuildShell();


    public void init() {
        southeast.init();
        southwest.init(new GuiShell[] {
                new RomizerShell(),
                new SquawkShell(),
                new TraceviewerShell(),
                new TCKShell(),
                new AppsTabbedPane.SingleCommandGuiShell("TestGC",  "testgc"),
                new AppsTabbedPane.SingleCommandGuiShell("xencode", "runxencode"),
                new AppsTabbedPane.SingleCommandGuiShell("Count3 ", "Count3"),
                new AppsTabbedPane.SingleCommandGuiShell("Count4 ", "Count4")
            }, new String[] {
                "Romizer",
                "RunVM",
                "Traceviewer",
                "TCK",
                "TestGC",
                "xencode",
                "Count3",
                "Count4"
            }
        );
        north.init();

        setLayout(new BorderLayout());
        //JSplitPane north = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,   northwest, northeast);

        JViewport southeastPort = new JViewport();  southeastPort.add(southeast);
        JViewport southwestPort = new JViewport();  southwestPort.add(southwest);
        JSplitPane south = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, southeastPort, southwestPort);

        JSplitPane whole = new JSplitPane(JSplitPane.VERTICAL_SPLIT, north, south);
        add("Center", whole);
        addComponentListener(this);
        validate();
        setVisible(true);
    }


    public void windowClosing(WindowEvent e) {
        System.exit(0);
    }

    public void windowClosed(WindowEvent e) {}

    public void windowActivated(WindowEvent e) {}

    public void windowDeactivated(WindowEvent e) {}

    public void windowIconified(WindowEvent e) {}

    public void windowDeiconified(WindowEvent e) {}

    public void windowOpened(WindowEvent e) {}

    public void componentHidden(ComponentEvent e) {}

    public void componentMoved(ComponentEvent e) {}

    public void componentShown(ComponentEvent e) {}

    public void componentResized(ComponentEvent e) {
        validate();
    }

    public static void main(Build builder) {
        protoBuild = builder;
        Gui gui = new Gui();
        gui.init();
        JFrame f = new JFrame();
        f.setTitle("Squawk builder");
        f.addWindowListener(gui);
        f.getContentPane().setLayout(new BorderLayout());
        f.getContentPane().add("Center", gui);
        f.validate();
        f.pack();
        f.setVisible(true);

    }
/*
    public Insets getInsets() {
        Insets in = super.getInsets();
        return new Insets(in.top+2,in.left+2,in.bottom+2,in.right+2);
    }
*/
}
