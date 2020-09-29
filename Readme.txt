Squawk instructions
===================


First
-----

    Install a J2SE system that is 1.3 or later


To build
--------

    java -jar build.jar [-msc] [-gcc] [-cc]

    (Use the approperate C compiler switch)


To clean
--------

    java -jar build.jar clean


To run the transator on port 9090
---------------------------------

    java -jar build.jar brazil


To run the build/test GUI
-------------------------

    java -jar build.jar -gui
