
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;

public class BrazilShell extends GuiShell {

    Option[]  options;
    Command[] commands;

    BrazilShell() {
        options = new Option[] {
            new Option("classpath",      true,  "j2me/classes;samples/classes;tck/classes;tck/agent.jar;tck/tck.jar", " "),
            new Option("traceloading",   true),
            new Option("tracefields",    false),
            new Option("tracepool",      false),
            new Option("tracebytecodes", false),
            new Option("traceir0",       false),
            new Option("traceir1",       false),
            new Option("tracelocals",    false),
            new Option("traceip",        false),
            new Option("verbose",        false),
            new Option("cachedir",       false, "translator_responses" ," "),
            new Option("preload",        false, "j2me/classes"         ," "),
            new Option("matching",       false, ""),
            new Option("Other",          true, "-J-Xmx128")
        };
        commands = new Command[] {
            new Command("Start translator", "brazil", false, options),
            new Command("Stop translator",  "stopbrazil")
        };
    }

    Option[]  getOptions()                { return options;  }
    Command[] getCommands()               { return commands; }
    String    getDefaultLogFileNameBase() { return "brazil";    }
}