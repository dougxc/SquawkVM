
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;

public class AppsTabbedPane extends JTabbedPane {

    static class SingleCommandGuiShell extends GuiShell {
        Command[] commands;
        String    logFileName;

        SingleCommandGuiShell(String cmdLabel, String cmd) {
            commands = new Command[] { new Command(cmd) };
            logFileName = cmdLabel;
        }

        Option[]  getOptions()                { return null;        }
        Command[] getCommands()               { return commands;    }
        String    getDefaultLogFileNameBase() { return logFileName; }

    }

    void init(GuiShell[] apps, String[] appNames) {
        for (int i = 0; i != apps.length; i++) {
            apps[i].init();
            addTab(appNames[i],apps[i]);
        }
    }
}