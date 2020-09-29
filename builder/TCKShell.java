
public class TCKShell extends GuiShell {

    Option[]  options;
    Command[] commands;

    TCKShell() {
        options = new Option[] {
            new Option("negative",           false),
            new Option("logURL",             true,  "file://tck/log.txt;append=true",     ":" ),
            new Option("passedURL",          true,  "file://tck/passed.txt",  ":" ),
            new Option("failedURL",          true,  "file://tck/failed.txt",  ":" ),
            new Option("Other",              false,  "")
        };
        commands = new Command[] {
            new Command("Start TCK", "squawk -Ximage:image -Xms40M TCK", false, options)
        };
    }

    Option[]  getOptions()                { return options;  }
    Command[] getCommands()               { return commands; }
    String    getDefaultLogFileNameBase() { return "tck";    }

}
