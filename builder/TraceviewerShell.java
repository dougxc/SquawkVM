
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;

public class TraceviewerShell extends GuiShell {

    Option[]  options;
    Command[] commands;

    TraceviewerShell() {
        options = new Option[] {
            new Option("sourcepath", true,  "vm/src;j2me/src;j2se/src;samples/src", " "),
            new Option("port",       true,  "5555", " "),
            new Option("truncate",   true,  "2", " "),
            new Option("showAll",    true),
            new Option("Other",      false, "squawk.trace")
        };
        commands = new Command[] {
            new Command("Launch viewer", "traceviewer", false, options)
        };
    }

    Option[]  getOptions()                { return options;       }
    Command[] getCommands()               { return commands;      }
    String    getDefaultLogFileNameBase() { return "traceviewer"; }
}
