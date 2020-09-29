package com.sun.squawk.traceviewer;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import javax.swing.text.*;
import sunlabs.brazil.util.regexp.*;
import javax.microedition.io.*;
import com.sun.squawk.io.connections.*;

public class TraceViewer extends JFrame implements WindowListener, ComponentListener {

    static final int EXPANSION_LIMIT = 100;

    static final String FRAME_TITLE_PREFIX = "Squawk trace viewer";

    /**
     * Regular expression matching thread prefix.
     */
    static final String THREAD_ID_RE = "Thread-([^:]*):";

    /**
     * Regular expression matching a method enter line. The captured groups are:
     *   1 - level identifier (e.g. "4")
     *   2 - the fully qualified method name (e.g. "java.lang.Object.wait")
     *   3 - the method's argument types (e.g. "Object,int,boolean")
     *   4 - the method's class number and slot (e.g. "4@47")
     */
    static final String METHOD_ENTER_RE =
        " \\(([1-9][0-9]*)\\) => *([^\\(]*)\\(([^\\)]*)\\) *\\(([^@]*@[^\\)]*)\\)";

    /**
     * Regular expression matching a method exit line. The captured groups are:
     *   1 - level identifier (e.g. "4")
     *   2 - the fully qualified method name (e.g. "java.lang.Object.wait")
     *   3 - the method's argument types (e.g. "Object,int,boolean")
     */
    static final String METHOD_EXIT_RE =
        " \\(([1-9][0-9]*)\\) <= *([^\\(]*)\\(([^\\)]*)\\)";

    /**
     * Regular expression matching the extra trace info for a primitive method enter. The captured groups are:
     *   1 - primitive opcode (e.g. "MENTER")
     *   2 - primitive operands (e.g. "ar=15302,ip=14377,rs1=9891,rs2=2")
     */
    static final String PRIMITIVE_METHOD_ENTER_RE =
        " *<PRIMITIVE: *([^ ]*) *([^>]*)";

    /**
     * Regular expression matching an instruction line. The captured groups are:
     *   1 - the source file name (e.g. "Object.java")
     *   2 - the source line number (e.g. "234")
     *   3 - the bytecode address (e.g. "34")
     *   4 - the opcode (e.g. "INVOKE")
     *   5 - the rest of the line (e.g. "offset = 47, nparms = 2, parms = { 466 9 }")
     */
    static final String INSTRUCTION_RE =
        "\\(([^:]*):([1-9][0-9]*)\\) *([1-9][0-9]*): *([^ ]*) *(.*)";

    /**
     * Regular expression matching a stack trace line. The captured groups are:
     *   1 - the fully qualified method name (e.g. "java.lang.Object.wait")
     *   2 - the source file name (e.g. "Object.java")
     *   3 - the source line number (e.g. "234")
     */
    static final String STACK_TRACE_RE = "([A-Za-z_][A-Za-z0-9_\\.\\$]*)\\((.*\\.java):([1-9][0-9]*)\\)";

    JTree                  threads;
    DefaultTreeModel       model;
    DefaultMutableTreeNode root;
    ClasspathConnection    sourcePath;
    Hashtable              sourceFiles;
    JScrollPane            sourceView;

    /**
     * Return the top level thread node for a given thread ID. creating it first if necessary.
     * @param threadID A thread's ID.
     */
    ThreadNode getThreadNode(String threadID) {
        Enumeration e = root.children();
        ThreadNode thread = null;
        while (e.hasMoreElements()) {
            ThreadNode childThread = (ThreadNode)e.nextElement();
            if (childThread.name.equals(threadID)) {
                thread = childThread;
            }
        }
        if (thread == null) {
            thread = new ThreadNode(threadID);
            root.add(thread);
        }
        return thread;
    }

    /**
     * Set the source path.
     */
    void setSourcePath(String path) {
        if (path == null) {
            return;
        }
        try {
            sourcePath = (ClasspathConnection)Connector.open("classpath://"+path);
            sourceFiles = new Hashtable();
        } catch (IOException ioe) {
            System.err.println("Couldn't open sourcepath:");
            ioe.printStackTrace();
        }
    }

    static String getMatch(int i, String str, int[] indices) {
        return str.substring(indices[i*2],indices[(i*2)+1]);
    }
    /**
     * Split a string based on a token.
     */
    public String[] split(String str, String tokens) {
        StringTokenizer st = new StringTokenizer(str, tokens);
        String res[] = new String[st.countTokens()];
        int i = 0;
        while (st.hasMoreTokens()) {
            res[i++] = st.nextToken();
        }
        return res;
    }

    static int toInt(String s, String name) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException nfe) {
            BaseFunctions.fatalError("Bad number format for "+name);
        }
        return -1;
    }

    /**
     * Parse a trace file and build the corresponding tree.
     */
    void parseTraceFile(InputStream is, boolean showAll, int truncateDepth) {
        try {
            Regexp threadRE      = new Regexp(THREAD_ID_RE);
            Regexp methodEnterRE = new Regexp(METHOD_ENTER_RE);
            Regexp primitiveRE   = new Regexp(PRIMITIVE_METHOD_ENTER_RE);
            Regexp methodExitRE  = new Regexp(METHOD_EXIT_RE);
            Regexp instructionRE = new Regexp(INSTRUCTION_RE);
            Regexp stackTraceRE  = new Regexp(STACK_TRACE_RE);

            int[] indices = new int[100];

            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line;
            ThreadNode thread = getThreadNode("<startup>");
            int inputLineNumber = 0;
            while ((line = br.readLine()) != null) {
            try {
                inputLineNumber++;

                if ((inputLineNumber % 10000) == 0) {
                    System.err.println("Read " +inputLineNumber + " lines of trace input");
                }

                // Ignore blank lines
                line = line.trim();
                if (line.length() == 0) {
                    continue;
                }

                if (threadRE.match(line,indices)) {
                    String threadID  = getMatch(1,line,indices);
                    thread = getThreadNode(threadID);
                    line = line.substring(indices[3] + 1);
                }

                // Eat bars indicating call depth as these are not handled efficiently
                // bar the regex parser when there are many/
                int bars = 0;
                if (line.length() > 0) {
                    while (line.length() > bars && line.charAt(bars) == '|') {
                        bars++;
                    }
                    line = line.substring(bars);
                }

                if (methodEnterRE.match(line,indices)) {
                    int level        = toInt(getMatch(1,line,indices),"call depth");
                    String name      = getMatch(2,line,indices);
                    String args      = getMatch(3,line,indices);
                    String slot      = getMatch(4,line,indices);

                    String packageName = "";
                    String className;

                    String[] components = split(name,".");
                    int index = components.length - 1;
                    name      = components[index--];
                    className = components[index--];

                    for (int i = 0; i <= index; i++) {
                        packageName += components[i];
                        if (i != index) {
                            packageName += ".";
                        }
                    }
                    if (packageName.length() == 0) {
                        packageName = null;
                    }
                    MethodNode node;
                    line = line.substring(indices[4*2+1]);
                    if (primitiveRE.match(line,indices)) {
                        String opcode   = getMatch(1,line,indices);
                        String operands = getMatch(2,line,indices);
                        node = new MethodNode(level,packageName,className,name,args,slot,opcode,operands);
                    }
                    else {
                        node = new MethodNode(level,packageName,className,name,args,slot,null,null);
                    }
                    BaseFunctions.assume(bars == level);
                    thread.addMethodNode(node, true, truncateDepth);
                }
                else
                if (methodExitRE.match(line,indices)) {
                    int level        = toInt(getMatch(1,line,indices),"call depth");
                    String name      = getMatch(2,line,indices);
                    String args      = getMatch(3,line,indices);

                    String packageName = "";
                    String className;

                    String[] components = split(name,".");
                    int index = components.length - 1;
                    name      = components[index--];
                    className = components[index--];

                    for (int i = 0; i <= index; i++) {
                        packageName += components[i];
                        if (i != index) {
                            packageName += ".";
                        }
                    }
                    if (packageName.length() == 0) {
                        packageName = null;
                    }
                    thread.addMethodNode(new MethodNode(level,packageName,className,name,args,null,null,null), false, truncateDepth);
                }
                else
                if (instructionRE.match(line,indices)) {
                    String fileName   = getMatch(1,line,indices);
                    int lineNumber    = toInt(getMatch(2,line,indices),"line number");
                    int ip            = toInt(getMatch(3,line,indices),"ip");
                    String opcode     = getMatch(4,line,indices);
                    String operands   = getMatch(5,line,indices);

                    thread.addInstructionNode(fileName,lineNumber,ip,opcode,operands);
                }
                else {
                    if (showAll) {
                        if (stackTraceRE.match(line,indices)) {
                            String name       = getMatch(1,line,indices);
                            String fileName   = getMatch(2,line,indices);
                            String lineNumber = getMatch(3,line,indices);

                            // Strip off class name and method name
                            try {
                                name = name.substring(0,name.lastIndexOf('.'));
                                name = name.substring(0,name.lastIndexOf('.'));

                                // Convert package name to path
                                name = name.replace('.','/');
                                thread.addStackTraceNode(line,name+"/"+fileName,Integer.parseInt(lineNumber));
                                continue;
                            } catch (NumberFormatException nfe) {
                            } catch (StringIndexOutOfBoundsException se) {
                            }
                        }
                        thread.addTraceLineNode(line);
                    }
                }
            } catch (OutOfMemoryError ome) {
                threads = null;
                throw new RuntimeException("Ran out of memory after reading " + inputLineNumber + " lines");
            }
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    /**
     * Find the line number for a given method's declaration.
     */
    static void setMethodLineNo(JTextArea source, MethodNode method) {
        String name = method.name;
        if (name.equals("_SQUAWK_INTERNAL_init")) {
            name = method.className;
        }
        else if (name.equals("_SQUAWK_INTERNAL_main") || name.equals("_SQUAWK_INTERNAL_run")) {
            name = name.substring("_SQUAWK_INTERNAL_".length());
        }

        StringBuffer sb = new StringBuffer(100);
        sb.append(name);
        sb.append("\\(");
        StringTokenizer st = new StringTokenizer(method.args,",");
        st.nextToken(); // ignore receiver which is always Object and implicit in source code
        int argCount = st.countTokens();
        while (st.hasMoreTokens()) {
            String arg = st.nextToken();
            int brackets = arg.indexOf('[');
            if (brackets != -1) {
                sb.append(arg.substring(0,brackets));
                sb.append(".*");
                for (int i = brackets; i != arg.length(); i++) {
                    sb.append("\\");
                    sb.append(arg.charAt(i));
                }
            }
            else {
                sb.append(arg);
            }
            if (argCount != 1) {
                sb.append("[^,]*, *");
            }
            else {
                sb.append("[^\\)]*");
            }
            argCount--;
        }
        sb.append("\\)");

        String reString = sb.toString();
        try {
            Regexp re = new Regexp(reString);
            int lineCount = source.getLineCount();
            for (int i = 0; i != lineCount; i++) {
                try {
                    int start   = source.getLineStartOffset(i);
                    int length  = source.getLineEndOffset(i) - start;
                    if (length < 1) {
                        continue;
                    }
                    String line = source.getText(start,length);
                    if (re.match(line) != null) {
                        method.lineNumber = i + 1;
                        return;
                    }
                } catch (BadLocationException ble) {
                    break;
                }
            }
        } catch (IllegalArgumentException iae) {
        }

        method.lineNumber = 1;
    }


    JTextArea getSourceFile(String path) {
        JTextArea text = (JTextArea)sourceFiles.get(path);
        if (text == null) {
            text = new JTextArea();
            text.setEditable(false);
            text.setFont(new Font("monospaced", Font.PLAIN, 12));
            try {
                InputStream is = sourcePath.openInputStream(path);
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                StringBuffer sb = new StringBuffer(is.available() * 2);
                String line = br.readLine();
                int lineNo = 1;
                while (line != null) {
                    sb.append(lineNo++);
                    sb.append(":\t");
                    sb.append(line);
                    sb.append("\n");
                    line = br.readLine();
                }
                text.setText(sb.toString());
                sourceFiles.put(path,text);
            } catch (IOException ioe) {
//                ioe.printStackTrace();
                text.setText("An exception occurred while reading "+path+":\n\t"+ioe);
                JViewport view = sourceView.getViewport();
                view.setView(text);
                return null;
            }
        }
        return text;
    }

    /**
     * Constructor.
     */
    TraceViewer(InputStream is, String sPath, int truncateDepth, boolean showAll) {

        setSourcePath(sPath);
        root    = new DefaultMutableTreeNode("All threads");
        model   = new DefaultTreeModel(root);

        threads = new JTree(model);
        threads.putClientProperty("JTree.lineStyle","Angled");
        threads.setShowsRootHandles(true);
        threads.setCellRenderer(new TraceRenderer());
        threads.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        threads.setScrollsOnExpand(false);

        parseTraceFile(is, showAll, truncateDepth);

        // Create the listener that displays the source for a given line or method
        // when its corresponding tree node is selected
        if (sourceFiles != null) {
        threads.addTreeSelectionListener(new TreeSelectionListener() {

            public void valueChanged(TreeSelectionEvent e) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode)threads.getLastSelectedPathComponent();
                /*
                 * Source cannot be shown when:
                 *    i) there is no selected node
                 *   ii) the node does not map to a source file
                 */
                if (node == null || !(node instanceof SourcePathItem)) return;

                MethodNode method = null;
                if (node instanceof MethodNode) {
                    method = (MethodNode)node;
                    // Don't show source for methods that are collapsed and have children.
                    if (method.nestedInstructionCount != 0 && threads.isCollapsed(new TreePath(node.getPath()))) {
                        return;
                    }
                }

                SourcePathItem item = (SourcePathItem)node;
                String path = item.getSourcePath();
                if (path == null) {
                    return;
                }

                JTextArea text = getSourceFile(path);
                if (text == null) {
                    setTitle(FRAME_TITLE_PREFIX + " - ??/" + path + ":" + item.getLineNumber());
                    return;
                }

                if (method != null && method.lineNumber == -1) {
                    setMethodLineNo(text, method);
                }

                JViewport view = sourceView.getViewport();
                view.setView(text);
                try {
                    final int lineNo   = item.getLineNumber();
                    final int startPos = text.getLineStartOffset(lineNo - 1);
                    final int endPos   = text.getLineEndOffset(lineNo - 1);
                    text.setCaretPosition(endPos);
                    text.moveCaretPosition(startPos);
                    text.getCaret().setSelectionVisible(true);

                    setTitle(FRAME_TITLE_PREFIX + " - " + path + ":" + lineNo);

                    final JTextArea textArea = text;

                    // Scroll so that the highlighted text is in the center if
                    // if is not already visible on the screen. The delayed
                    // invocation is necessary as the view for the text
                    // area will not have been computed yet.
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            // Compute the desired area of the text to show.
                            Rectangle textScrollRect = new Rectangle();
                            textScrollRect.y = (lineNo - 1) * textArea.getFontMetrics(textArea.getFont()).getHeight();
                            Rectangle visible = textArea.getVisibleRect();

                            textScrollRect.height = visible.height;
                            textScrollRect.y -= (visible.height >> 1);

                            // Compute the upper and lower bounds of what
                            // is acceptable.
                            int upper = visible.y + (visible.height >> 2);
                            int lower = visible.y - (visible.height >> 1);

                            // See if we really should scroll the text area.
                            if ((textScrollRect.y < lower) ||
                                (textScrollRect.y > upper)) {
                                // Check that we're not scrolling past the
                                // end of the text.
                                int newbottom = textScrollRect.y +
                                    textScrollRect.height;
                                int textheight = textArea.getHeight();
                                if (newbottom > textheight) {
                                    textScrollRect.y -= (newbottom - textheight);
                                }
                                // Perform the text area scroll.
                                textArea.scrollRectToVisible(textScrollRect);
                            }
                        }
                    });

                } catch (BadLocationException ble) {
                } catch (IllegalArgumentException iae) {
                }
            }
        });
        }

        try {
            // Compute nested instruction counts
            for (Enumeration e = root.children(); e.hasMoreElements(); ) {
                ThreadNode thread = (ThreadNode)e.nextElement();
                if (thread.entryMethod != null) {
                    thread.entryMethod.computeNestedInstructionCount();
                }
            }

            // Expand the path to the last instruction in each thread
            for (Enumeration e = root.children(); e.hasMoreElements(); ) {
                ThreadNode thread = (ThreadNode)e.nextElement();
                if (thread.last != null) {
                    thread.executionPath.addElement(thread.last);
                }
                for (Enumeration p = thread.executionPath.elements(); p.hasMoreElements(); ) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode)p.nextElement();
                    if (node.isLeaf()) {
                        node = (DefaultMutableTreeNode)node.getParent();
                    }
                    if (node != null) {
                        threads.expandPath(new TreePath(node.getPath()));
                    }
                }
            }
        } catch (Exception e) {
            // Don't want this to prevent viewing of trace built up after a potentially long execution
            e.printStackTrace();
        }

        /*
         * Initialise the GUI
         */
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());

        // Put the tree in a scrollable pane
        JScrollPane treeScrollPane = new JScrollPane(threads);

        // Place holder until a node is selected
        JPanel noSourcePanel = new JPanel(new GridBagLayout());
        noSourcePanel.add(new JLabel("No source file selected/available"));
        sourceView = new JScrollPane(noSourcePanel);

        // Only create a source view panel if a valid source path was provided
        if (sourceFiles != null) {
            JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,treeScrollPane,sourceView);
            splitPane.setDividerLocation(500);
            mainPanel.add("Center", splitPane);
        }
        else {
            mainPanel.add("Center", treeScrollPane);
        }

        mainPanel.setPreferredSize(new Dimension(1275,970));
        setTitle(FRAME_TITLE_PREFIX);
        getContentPane().add(mainPanel);
        addWindowListener(this);
        addComponentListener(this);
        validate();
        pack();
    }

    /**
     * WindowListener implementation.
     */
    public void windowClosing(WindowEvent e) {
        System.exit(0);
    }
    public void windowClosed(WindowEvent e) {}
    public void windowActivated(WindowEvent e) {}
    public void windowDeactivated(WindowEvent e) {}
    public void windowIconified(WindowEvent e) {}
    public void windowDeiconified(WindowEvent e) {}
    public void windowOpened(WindowEvent e) {}

    /**
     * ComponentListener implementation.
     */
    public void componentHidden(ComponentEvent e) {}
    public void componentMoved(ComponentEvent e) {}
    public void componentShown(ComponentEvent e) {}
    public void componentResized(ComponentEvent e) {
        validate();
    }

    /**
     * Print usage message.
     * @param errMsg An error message or null.
     */
    static void usage(String errMsg) {
        PrintStream out = System.out;
        if (errMsg != null) {
            out.println(errMsg);
        }
        out.println("Usage: TraceViewer [-options] [ tracefile ] ");
        out.println("where options include:");
        out.println("    -sourcepath <path>  where to find source files");
        out.println("    -port <port>        port to open for receiving trace input");
        out.println("    -truncate <level>   truncate subtree of methods that returned to be <depth> deep.");
        out.println("                        The value -1 indicates no truncation (default=0)");
        out.println("    -showAll            show all trace messages, not just instructions and methods");
        out.println("    -help               show this message and exit");
        out.println();
        out.println("Either a tracefile must be provided or a -port option must be given");
        out.println();
    }

    /**
     * Command line entrance point.
     */
    public static void main(String[] args) {
        String sourcePath  = null;
        boolean showAll    = false;
        int port           = -1;
        String traceFile   = null;
        int truncateDepth  = -1;

        int i = 0;
        for ( ; i < args.length ; i++) {
            if (args[i].charAt(0) != '-') {
                break;
            }
            String arg = args[i];
            if (arg.equals("-sourcepath") || arg.equals("-sp")) {
                sourcePath = args[++i];
            }
            else
            if (arg.equals("-port")) {
                port = toInt(args[++i], " port number");
            }
            else
            if (arg.equals("-showAll")) {
                showAll = true;
            }
            else
            if (arg.equals("-truncate")) {
                truncateDepth = toInt(args[++i]," truncation depth");
            } else {
                usage("Bad switch: "+arg);
                return;
            }
        }
        if (port == -1) {
            if (i >= args.length) {
                usage("Missing tracefile");
                return;
            }
            else {
                traceFile = args[i];
            }
        }

        InputStream is = null;
        try {
            if (port != -1) {
                StreamConnectionNotifier ssocket = (StreamConnectionNotifier)Connector.open("serversocket://:"+port);
                System.out.println("listening on port " + port + " for trace input...");
                is = ssocket.acceptAndOpen().openInputStream();
                System.out.println("reading trace...");
            }
            else {
                is = new FileInputStream(traceFile);
            }
        }
        catch (IOException ioe) {
            ioe.printStackTrace();
            return;
        }

        TraceViewer instance = new TraceViewer(is,sourcePath,truncateDepth,showAll);
        instance.setVisible(true);

    }

}

class ThreadNode extends DefaultMutableTreeNode {
    MethodNode      entryMethod;
    MethodNode      currentMethod;
    String          name;
    String          sourceFile;
    TraceLineNode   last;
    Vector          executionPath = new Vector();


    ThreadNode(String name) {
        this.name = name;
        setUserObject("Thread-"+name);
    }


    void setEntryMethod(MethodNode node, boolean entering) {
        // Create dummy methods if necessary
        if (node.level != 1) {
            int level = 1;
            currentMethod = entryMethod = new MethodNode(level,null,"?","?","?","?",null,null);
            while ((++level) != node.level) {
                MethodNode dummy = new MethodNode(level,null,"?","?","?","?",null,null);
                currentMethod.add(dummy);
                currentMethod = dummy;
            }
            if (entering) {
                currentMethod.add(node);
                currentMethod = node;
            }
        }
        else {
            entryMethod = node;
            currentMethod = node;
        }
        add(entryMethod);
    }

    static void removeAllChildren(DefaultMutableTreeNode node, int depth) {
        if (depth == 0) {
            node.removeAllChildren();
            return;
        }
        for (Enumeration children = node.children(); children.hasMoreElements();) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode)children.nextElement();
            if (!child.isLeaf()) {
                removeAllChildren(child,depth - 1);
            }
        }
    }

    void addMethodNode(MethodNode node, boolean entering, int truncateDepth) {
        if (entering) {
            if (entryMethod == null) {
                setEntryMethod(node,true);
            }
            else {
                addChildNode(node);
                currentMethod = node;
            }
        }
        else {
            if (entryMethod == null) {
                setEntryMethod(node,false);
                return;
            }
            int leavingLevel = currentMethod.level;
            int currentLevel  = leavingLevel;
            MethodNode leavingMethod = currentMethod;
            while (currentLevel != (node.level - 1)) {
                BaseFunctions.assume(currentLevel > 0);
                currentLevel--;
                currentMethod = (MethodNode)currentMethod.getParent();
            }
            if (leavingLevel != node.level) {
                CommentNode theThrow = new CommentNode("throw to exception handler in "+currentMethod.label);
                leavingMethod.add(theThrow);
                executionPath.addElement(theThrow);
            }
            else {
                if (leavingMethod.className == "?") {
                    leavingMethod.updateFrom(node);
                }
                else {
                    if (truncateDepth >= 0) {
                       removeAllChildren(leavingMethod, truncateDepth);
                    }
                }
            }
        }
        last = currentMethod;
    }

    void addInstructionNode(String fileName, int lineNumber, int ip, String opcode, String operands) {
        addChildNode(new InstructionNode(fileName,lineNumber,ip,opcode,operands,currentMethod));
    }

    void addChildNode(TraceLineNode node) {
        if (currentMethod != null) {
            currentMethod.add(node);
        } else {
            this.add(node);
        }
        last = node;
    }

    void addStackTraceNode(String line, String filePath, int lineNumber) {
        addChildNode(new StackTraceNode(line, filePath, lineNumber));
    }
    void addTraceLineNode(String line) {
        addChildNode(new CommentNode(line));
    }

}

interface SourcePathItem {
    String getSourcePath();
    int    getLineNumber();
}

/**
 * Represents a line of trace info.
 */
abstract class TraceLineNode extends DefaultMutableTreeNode {
    String label;
    TraceLineNode(String label) {
        setLabel(label);
    }
    void setLabel(String label) {
        this.label = label;
        setUserObject(label);
    }
}

class CommentNode extends TraceLineNode {
    CommentNode(String label) {
        super(label);
    }
}


class MethodNode extends TraceLineNode implements SourcePathItem {
    String packageName;
    String className;
    String name;
    String args;
    final String slot;
    final String primOpcode;
    final String primOperands;
    final int    level;
    int   nestedInstructionCount;
    InstructionNode firstInstruction;
    MethodNode(int level, String packageName, String className, String name, String args,
        String slot, String primOpcode, String primOperands)
    {
        super((primOpcode == null ? "" : primOpcode + "(" + primOperands + ") ") +
                           (packageName == null ? "" : packageName + ".") +
                           className + "." +
                           name + "(" +
                           args + ")" +
                           " (" + slot + ")");
        this.level       = level;
        this.packageName = packageName;
        this.className   = className;
        this.name        = name;
        this.args        = args;
        this.slot        = (slot == null ? null : slot);
        this.primOpcode  = (primOpcode == null ? null : primOpcode);
        this.primOperands= (primOperands == null ? null : primOperands);
    }

    void updateFrom(MethodNode other) {
        this.packageName = other.packageName;
        this.className   = other.className;
        this.name        = other.name;
        this.args        = other.args;
        setLabel((packageName == null ? "" : packageName + ".") +
                           className + "." +
                           name + "(" +
                           args + ")" +
                           " (" + slot + ")");
    }

    String fileName;
    public String getSourcePath() {
        if (fileName == null) {
            if (firstInstruction != null) {
                fileName = firstInstruction.getSourcePath();
            }
            else {
                fileName = "";
                if (packageName != null) {
                    fileName += packageName.replace('.', '/') + '/';
                }
                int index = className.indexOf('$');
                if (index == -1) {
                    fileName += className;
                } else {
                    fileName += className.substring(0,index);
                }
                fileName += ".java";
                fileName = fileName;
            }
        }
        return fileName;
    }
    int lineNumber = -1;
    public int getLineNumber() {
        if (firstInstruction != null) {
            return firstInstruction.getLineNumber();
        }
        return lineNumber;
    }

    int computeNestedInstructionCount() {
        nestedInstructionCount = 0;
        for (Enumeration e = children(); e.hasMoreElements(); ) {
            Object child = e.nextElement();
            if (child instanceof InstructionNode) {
                nestedInstructionCount++;
            }
            else
            if (child instanceof MethodNode) {
                nestedInstructionCount += ((MethodNode)child).computeNestedInstructionCount();
            }
        }
        return nestedInstructionCount;
    }
}

class StackTraceNode extends CommentNode implements SourcePathItem {
    final String filePath;
    final int    lineNumber;
    StackTraceNode(String label, String filePath, int lineNumber) {
        super(label);
        this.filePath   = filePath;
        this.lineNumber = lineNumber;
    }
    public String getSourcePath() {
        return filePath;
    }
    public int getLineNumber() {
        return lineNumber;
    }
}

class InstructionNode extends TraceLineNode  implements SourcePathItem {
    final String fileName;
    final int    lineNumber;
    final int    ip;
    final String opcode;
    final String operands;
    InstructionNode(String fileName, int lineNumber, int ip, String opcode, String operands, MethodNode parent) {
        super(ip + ": " + opcode + " " + operands);
        this.ip         = ip;
        this.opcode     = opcode;
        this.operands   = operands;
        if (parent != null && parent.name != "?") {
            if (parent.packageName == null) {
                this.fileName = fileName;
            }
            else {
                this.fileName = (parent.packageName.replace('.','/')+'/' + fileName);
            }
            this.lineNumber = lineNumber;
            if (parent.firstInstruction == null) {
                parent.firstInstruction = this;
            }
        }
        else {
            // No parent so source file has no package/class context - show file name anyway
            this.fileName   = fileName;
            this.lineNumber = lineNumber;
        }
    }
    public String getSourcePath() {
        return fileName;
    }
    public int getLineNumber() {
        return lineNumber;
    }
}

abstract class BaseFunctions {
    static void assume(boolean c) {
        if (!c) {
            fatalError("assume failure");
        }
    }
    static void assume(boolean c, String msg) {
        if (!c) {
            fatalError("assume failure: "+msg);
        }
    }

    static void fatalError(String msg) {
        throw new RuntimeException(msg);
    }
}

class TraceRenderer extends DefaultTreeCellRenderer {

    Font plain;
    Font italic;
    Font bold;
    Object v;

    TraceRenderer () {
        plain  = new Font(null,Font.PLAIN,12);
        italic = plain.deriveFont(Font.ITALIC);
        bold   = plain.deriveFont(Font.BOLD);
    }
    public Component getTreeCellRendererComponent(
                        JTree tree,
                        Object value,
                        boolean sel,
                        boolean expanded,
                        boolean leaf,
                        int row,
                        boolean hasFocus) {

        super.getTreeCellRendererComponent(
                        tree, value, sel,
                        expanded, leaf, row,
                        hasFocus);
        if (value instanceof CommentNode) {
            setIcon(null);
            setFont(italic);
        }
        else
        if (value instanceof MethodNode) {
            setFont(bold);
            if (!expanded) {
                int nestedCount = ((MethodNode)value).nestedInstructionCount;
                setText("["+nestedCount+"] "+getText());
            }
        }
        else {
            setFont(plain);
        }
        v = value;
        return this;
    }
}






