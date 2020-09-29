
package java.lang;

public class Class extends ClassBase {

    /**
     * Creates a new array of of a class.
     */
    static Object newArray(int cno, int length) throws InstantiationException, IllegalAccessException {
        return null;
    }

    /**
     * addDimension
     */
    static void addDimension(Object[] array, int nextDimention) throws InstantiationException, IllegalAccessException {
    }

    /**
     * Creates a new instance of a class.
     */
    static Object newInstance(int cno) throws InstantiationException, IllegalAccessException {
        return newInstance(cno,false);
    }

    static Object newInstance(int cno, boolean callConstructor) throws InstantiationException, IllegalAccessException {
        return null;
    }

}
