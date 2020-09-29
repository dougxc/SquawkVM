#
# Note the file ../build.jar will contain enough of the JDK 1.3.1 tools.jar to run javac
#

#!sh
javac *.java com/sun/tools/javac/v8/Main.java
#cp javac.jar ../build.jar
#jar uvfm ../build.jar MANIFEST.MF *.class
jar cvfm ../build.jar MANIFEST.MF *.class
rm *.class
