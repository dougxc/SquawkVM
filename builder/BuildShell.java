
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;

public class BuildShell extends GuiShell {

    Option[]  options;
    Command[] commands;

    BuildShell() {
        options = new Option[] {
            new Option("msc",  Gui.protoBuild.msc),
            new Option("gcc",  Gui.protoBuild.gcc),
            new Option("cc",   Gui.protoBuild.cc),
            new Option("fp",  !Gui.protoBuild.nofp),
            new Option("o2",   Gui.protoBuild.o2)
        };
        commands = new Command[] {
            new Command("Build all", "all", true, options),
            new Command("j2me"),
            new Command("translator"),
            new Command("j2se"),
            new Command("vm", "vm", true, options),
            new Command("vmboot"),
            new Command("xencode"),
            new Command("tck"),
            new Command("jcard"),
            new Command("clean")
        };
    }

    Option[]  getOptions()                { return options;  }
    Command[] getCommands()               { return commands; }
    String    getDefaultLogFileNameBase() { return "build";  }
}