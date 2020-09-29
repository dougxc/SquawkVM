package java.lang;
import java.util.Hashtable;

public class JavaApplicationManager {

   /*
    * main
    */
    public static void main(String[] args) {
        try {
            try {
                new JavaApplicationManager().runJam();
                Native.exit(0);
            } catch (Throwable ex) {
                System.out.println("Uncaught exception in JAM ");
                System.out.println(ex);
                ex.printStackTrace();
            }
        } catch (Throwable ex) {
        }
        Native.fatalVMError();
    }

   /**
    * This is the entry point to the application manager.
    */
    private void runJam() {
        String[] args = Native.getCommandLineArgs();
        if (args.length == 0) {
            for (int i = 1; ; i++) {
                System.out.println("creating Isolate to run tests");
                Hashtable props = new Hashtable();
                props.put("squawk.trace.classloading.disassemble", "true");
                Isolate test = new Isolate("TestHarness",new String[0], "j2me/classes", "test"+i, props);
                System.out.println(Isolate.getCurrentIsolate() + ": created " + test);
                System.out.println(Isolate.getCurrentIsolate() + ": starting " + test);
                test.start();
                try {
                    test.join();
                } catch (InterruptedException ie) {
                }
            }
        }
        else {
            String mainClassName = args[0];
            Object old = args;
            args = new String[args.length - 1];
            System.arraycopy(old, 1, args, 0, args.length);
            System.out.print("JAM running: " + mainClassName);
            for (int i = 0; i < args.length; i++) {
                System.out.print(" " + args[i]);
            }
            System.out.println();
            Isolate app = new Isolate(mainClassName, args, "", mainClassName);
            app.start();
                try {
                    app.join();
                } catch (InterruptedException ie) {
                }
        }
    }
}
