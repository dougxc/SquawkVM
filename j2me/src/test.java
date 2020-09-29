import java.io.*;

public class test {
      static double aDoubleStatic;
      byte aByte;
      short aShort;
      double aDouble;
      static int initValue = 1000000;

      public static void main(String[] args) {
          try{
              Class.forName(args[0]);
          } catch (NullPointerException npe) {
              npe = null;
          } catch (ArrayIndexOutOfBoundsException e) {
              e = null;
          } catch (ClassNotFoundException e) {
              e = null;
          }
          try{
              Class.forName(args[0]);
          } catch (ClassNotFoundException e) {
              e = null;
          } finally {
              System.out.println();
          }

      }

      public double doDoubleAdd(double d1, double d2) {
          long l1 = (long)d1;
          long l2 = (long)d2;
          if (l1 < l2)
              d1 = d2;
          if (d1 > d2)
              d1 = d2;
          return d1 + d2 + aDouble;
      }
}

class base {
    // length = 3
    // sizeInBytes = 9
    int word1;
    byte b1;
    byte b2;
    byte b3;
    byte b4;
    byte b5;

    int lookup(char ch, int i) {
	    switch (ch){
	    case '-':
		return 1;
		//FALLTHROUGH
	    case '+':
		i++;
	    }
      return i;
    }
}

class base2 extends base {
    // length = 3
    // sizeInBytes = 11
    double d1;
    byte b1;
    short s1;
}

class base3 extends base2 {
    Object ref1;
    byte b1;
}
