
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;

public class RomizerShell extends GuiShell {

    Option[]  options;
    Command[] commands;

    RomizerShell() {
        options = new Option[] {
            new Option("image",              true,  "image",          " "),
            new Option("dis",                false, "dis.txt",        " "),
            new Option("toc",                false, "toc.txt",        " "),
            new Option("heap",               false, "heap.txt",       " "),
            new Option("classStats",         false),
            new Option("traceLoading",       false),
            new Option("traceParser",        false),
            new Option("disassemble",        false),
            new Option("traceImage",         false),
            new Option("help",               false),
            new Option("Other",              true,  "@j2me/classes;samples/classes;tck/classes;tck/agent.jar")
        };
        commands = new Command[] {
            new Command("Romize", "romizer", false, options)
        };
    }

    Option[]  getOptions()                { return options;  }
    Command[] getCommands()               { return commands; }
    String    getDefaultLogFileNameBase() { return "romizer";}

}
