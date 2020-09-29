
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;

public class SquawkShell extends GuiShell {

    Option[]  options;
    Command[] commands;

    SquawkShell() {
        options = new Option[] {
            new Option("Ximage",             true,  "image",  ":"                 ),
            new Option("Xms",                true,  "2M",     ""                  ),
            new Option("XdebugIO",           false                                ),
            new Option("XtraceThreshold",    false, "0",      ":"                 ),
            new Option("XtraceInstructions", false                                ),
            new Option("XtraceMethods",      false                                ),
            new Option("XtraceAllocation",   false                                ),
            new Option("XtraceGC",           false                                ),
            new Option("XtraceGCVerbose",    false                                ),
            new Option("XtraceURL",          true,  "socket://localhost:5555", ":"),
            new Option("Other",              false,  "")
        };
        commands = new Command[] {
            new Command("Start VM", "squawk", false, options)
        };
    }

    Option[]  getOptions()                { return options;  }
    Command[] getCommands()               { return commands; }
    String    getDefaultLogFileNameBase() { return "squawk"; }

}
