package java.lang.isolate;

public class Isolate extends java.lang.Isolate {

//    public Isolate(String mainClassName, String[] args, java.lang.URL[] classpath, String name) {
//    }

   /**
    * Constructor
    */
    public Isolate(String mainClassName, String[] args, String classpath, String name) {
        super(mainClassName, args, classpath, name);
    }

}
