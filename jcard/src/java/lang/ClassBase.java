
package java.lang;

public class ClassBase {

    static int foo1;
    static int foo2;
    static int foo3;
    static int foo4;
    static int foo5;
    static int foo6;

    Class    CLS_self;
    int      CLS_classIndex;
    int      CLS_accessFlags;
    int      CLS_gctype;

    int      CLS_length;
    String   CLS_className;
    Class    CLS_superClass;
    Class    CLS_elementType;

    Class[]  CLS_interfaces;
    byte[][] CLS_vtable;
    int      CLS_vstart;
    int      CLS_vcount;

    byte[][] CLS_fvtable;
    short[]  CLS_itable;
    int      CLS_istart;
    short[]  CLS_iftable;

    short[]  CLS_sftable;
    Object[] CLS_constTable;
    byte[]   CLS_oopMap;
    byte[]   CLS_debugInfo;


}
