
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;

public abstract class GuiShell extends JPanel implements  ComponentListener {


    /**
     * The Option class represents a GUI version of command line option. An Option
     * can be attached to one or more Commands.
     */
    class Option {
        /**
         * Constructor for an option that has no text field.
         * @param cmdName The name of the option (as expected on the command line preceeded by a '-').
         * @param selected The initial selection state of the option.
         */
        public Option(String cmdName, boolean selected) {
            this(cmdName, cmdName, selected, (String)null, (String)null);
        }

        /**
         * Constructor for an option that has a text field.
         * @param cmdName The name of the option (as expected on the command line preceeded by a '-').
         * @param selected The initial selection state of the option.
         * @param text The default value of the text field.
         * @param separator The separator inserted between the option name and the option value on the command line.
         */
        public Option(String cmdName, boolean selected, String text, String separator) {
            this(cmdName, cmdName, selected, text, separator);
        }

        /**
         * Constructor for an option that is really a back door for adding other miscellaneous options to
         * the command line.
         * @param selected The initial selection state of the option.
         * @param text The default value of the text field specifying the other options.
         */
        public Option(String label, boolean selected, String text) {
            this(label, (String)null, selected, text, (String)null);
        }

        private Option(String label, String cmdName, boolean selected, String text, String sep) {
            checkBox = new JCheckBox(label, selected);
            checkBox.setName(cmdName);
            if (text != null) {
                textField = new JTextField(text, 10);
                separator = sep;
            }
            else {
                textField = null;
                separator = null;
            }
        }

        /**
         * Get the command line representation of this option.
         */
        public String toString() {
            if (checkBox.isSelected()) {
                String res = checkBox.getName();
                if (res == null) {
                    res = "";
                }
                else {
                    res = "-" + res;
                }
                if (separator != null) {
                    res += separator;
                }
                if (textField != null) {
                    res = res + textField.getText();
                }
                return res;
            }
            return "";
        }

        final JCheckBox  checkBox;
        final JTextField textField;
        final String     separator;
    }

    /**
     * The Command class represents a GUI version of command that can be passed to Build.
     */
    class Command extends JButton {

        /**
         * Construct a command.
         * @param label The label for the button.
         * @param cmd The name of the command passed to Build.
         */
        public Command(String label, final String cmd) {
            this(label, cmd, false, null);
        }

        /**
         * Construct a command.
         * @param cmd The name of the command passed to Build. Also used as the button label.
         */
        public Command(final String cmd) {
            this(cmd, cmd, false, null);
        }

        /**
         * Construct a command.
         * @param label The label for the button.
         * @param cmd The name of the command passed to Build.
         * @param prependOptions If true, any options attached to the command are prepended
         * to the command name otherwise they are appended.
         * @param options The options that apply to this command.
         */
        public Command(String label, final String cmd, final boolean prependOptions, final Option[] options) {
            super(label);
            ActionListener listener = new ActionListener() {
                public void actionPerformed(ActionEvent  event) {
                    String optionString = "";
                    String execCmd = cmd;
                    if (options != null) {
                        for (int i = 0; i != options.length; i++) {
                            optionString += options[i] + " ";
                        }
                    }
                    optionString = optionString.trim();
                    if (optionString.length() != 0) {
                        if (prependOptions) {
                            execCmd = optionString + " " + execCmd;
                        }
                        else {
                            execCmd += " " + optionString;
                        }
                    }
                    exec(execCmd);
                }
            };
            addActionListener(listener);
        }

    }

    Build builder;

    // Text area components
    final JTextArea text = new JTextArea();
    PrintStream stderr;
    PrintStream stdout;
    JCheckBox  verboseBox;
    JCheckBox  useShellBox;

    // Command button panel components
    JButton    stopButton;
    Command[]  commands;
    Thread     execThread;

    JPanel createButtonPanel() {
        commands = getCommands();
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        for (int i = 0; i != commands.length; i++) {
            buttonPanel.add(commands[i]);
        }
        stopButton = new JButton("Stop");
        stopButton.setEnabled(false);
        stopButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                /*
                 * This will not immediately stop a Build process that
                 * is pure java unless some part of that application is
                 * in a wait state. However, it should immediately stop
                 * any Build process that uses exec as that thread is
                 * almost certainly in a wait state (i.e. Process.waitFor()
                 * has been called).
                 * This is preferrable to Thread.stop given all the potential
                 * for corruption of locked objects it poses.
                 */
                execThread.interrupt();
            }
        });
        buttonPanel.add(stopButton);

        // Add button for clearing text area
        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent  event) {
                text.setText("");
            }
        });
        buttonPanel.add(clearButton);


        return buttonPanel;
    }

    /**
     * Create the text area panel that displays the output of the shell command.
     * The panel includes controls for manipulating the output.
     */
    JPanel createTextAreaPanel() {
        final TextAreaOutputStream taos = new TextAreaOutputStream(text);
        final JScrollPane pane = new JScrollPane(text);
        stderr = new PrintStream(taos);
        stdout = new PrintStream(taos);


        text.setEditable(false);
        text.setFont(new Font("monospaced", Font.PLAIN, 12));
        pane.setPreferredSize(new Dimension(400, 300));
        JPanel textAreaOptions = null;

        textAreaOptions = new JPanel(new GridLayout(2,1));

        JPanel textAreaOptionsRow1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 1, 3));
        JPanel textAreaOptionsRow2 = new JPanel();

        textAreaOptionsRow1.setBorder(BorderFactory.createEtchedBorder());
        textAreaOptionsRow2.setBorder(BorderFactory.createEtchedBorder());

        if (!Build.demoMode) {
            textAreaOptions.add(textAreaOptionsRow1);
            textAreaOptions.add(textAreaOptionsRow2);
        }

        // Add option to turn off output to text area (or log file)
        final JCheckBox txtEnabled = new JCheckBox("Enabled",taos.writeEnabled);
        txtEnabled.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getSource() == txtEnabled) {
                    taos.setWriteEnabled(e.getStateChange() == ItemEvent.SELECTED);
                }
            }
        });
        textAreaOptionsRow1.add(txtEnabled);

        // Add option to turn off auto scrolling of text area
        final JCheckBox autoScroll = new JCheckBox("Autoscroll",taos.autoScroll);
        autoScroll.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getSource() == autoScroll) {
                    taos.setAutoScroll(e.getStateChange() == ItemEvent.SELECTED);
                }
            }
        });
        textAreaOptionsRow1.add(autoScroll);

        // Add verbose option
        verboseBox = new JCheckBox("Verbose", Gui.protoBuild.verbose);
        textAreaOptionsRow1.add(verboseBox);

        // Add useShell option
        useShellBox = new JCheckBox("Shell", Gui.protoBuild.useShell);
        textAreaOptionsRow1.add(useShellBox);

        // Add wrap option
        final JCheckBox wrapBox = new JCheckBox("Wrap", false);
        wrapBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getSource() == wrapBox) {
                    text.setLineWrap(wrapBox.isSelected());
                }
            }
        });
        textAreaOptionsRow1.add(wrapBox);

        // Add option to redirect output to a log file
        String cwd = "";
        try {
            cwd = System.getProperty("user.dir") + File.separator;
        } catch (SecurityException se) {
        }
        final JCheckBox  logToFile    = new JCheckBox("Log to file:",taos.fos != null);
        final JTextField logFile      = new JTextField(cwd + getDefaultLogFileNameBase() + ".log",20);
        final JButton    logFileClear = new JButton("Clear");
        logToFile.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getSource() == logToFile) {
                    try {
                        if (e.getStateChange() == ItemEvent.SELECTED) {
                                FileOutputStream fos = new FileOutputStream(logFile.getText(),true);
                                taos.setLogFile(fos);
                        }
                        else {
                            taos.setLogFile(null);
                        }
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                }
            }
        });
        logFileClear.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    taos.clearLogFile(logFile.getText());
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        });

        textAreaOptionsRow2.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        JPanel row = textAreaOptionsRow2;
        row.add(logToFile,   new GridBagConstraints(c.gridx,c.gridy,c.gridwidth,c.gridheight,1.0,  c.weighty,c.EAST,c.fill,      c.insets,c.ipadx,c.ipady));
        row.add(logFile,     new GridBagConstraints(c.gridx,c.gridy,c.gridwidth,c.gridheight,100.0,c.weighty,c.WEST,c.HORIZONTAL,c.insets,c.ipadx,c.ipady));
        row.add(logFileClear,new GridBagConstraints(c.gridx,c.gridy,c.gridwidth,c.gridheight,1.0,  c.weighty,c.EAST,c.fill,      c.insets,c.ipadx,c.ipady));


        JPanel textAreaPanel = new JPanel(new BorderLayout());
        textAreaPanel.add("Center",pane);
        textAreaPanel.add("South",textAreaOptions);
        return textAreaPanel;
    }

    /**
     * Initialise the GUI for this shell command component.
     */
    public void init() {
        setLayout(new BorderLayout());

        JPanel optPanel = getOptionPanel();
        if (optPanel != null) {
            add("East", optPanel);
        }
        add("North", createButtonPanel());
        add("Center", createTextAreaPanel());

        addComponentListener(this);
        validate();
        setVisible(true);
    }


    /**
     * Return an array of Option objects that configure this shell.
     */
    abstract Option[]  getOptions();
    abstract Command[] getCommands();

    abstract String getDefaultLogFileNameBase();

    /*
     * getOptions
     */
    JPanel getOptionPanel() {
        Option[] options = getOptions();
        if (options != null) {
            JPanel panel = new JPanel(new GridLayout(options.length, 1));
            for (int i = 0; i != options.length; i++) {
                Option opt = options[i];
                if (opt.textField != null) {
                    FlowLayout layout = new FlowLayout(FlowLayout.LEFT);
                    layout.setHgap(1);
                    JPanel boxAndText = new JPanel(layout);
                    boxAndText.add(opt.checkBox);
                    boxAndText.add(opt.textField);
                    panel.add(boxAndText);
                }
                else {
                    panel.add(opt.checkBox);
                }
            }
            return panel;
        }
        return null;
    }


    public void componentHidden(ComponentEvent e) {}
    public void componentMoved(ComponentEvent e) {}
    public void componentShown(ComponentEvent e) {}
    public void componentResized(ComponentEvent e) {
        validate();
    }

    /*
     * exec
     */
    protected void exec(final String cmd) {
        execThread = new Thread() {
            public void run() {
                StringTokenizer tok = new StringTokenizer(cmd);
                String[] args = new String[tok.countTokens()];
                int arg = 0;
                while (tok.hasMoreTokens()) {
                    args[arg++] = tok.nextToken();
                }
                try {
                    Build b = new Build(args,stdout,stderr);
//System.err.println("Starting build process: "+cmd);
                    b.verbose = verboseBox.isSelected();
                    b.useShell = useShellBox.isSelected();
                    b.run();
                } catch (InterruptedException e) {
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
//System.err.println("Finished build process: "+cmd);
                    for (int i = 0; i != commands.length; i++) {
                        commands[i].setEnabled(true);
                    }
                    stopButton.setEnabled(false);
                }

            }
        };

        for (int i = 0; i != commands.length; i++) {
            if (!commands[i].getText().equals("Stop translator")) {
                commands[i].setEnabled(false);
            }
        }
        stopButton.setEnabled(!useShellBox.isSelected());
        execThread.start();

    }


}