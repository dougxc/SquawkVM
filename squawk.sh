java -Xbootclasspath/a:j2se/classes\;vm/classes\;vmboot/classes\;j2me/tmpclasses\;j2me/classes -Xrunhprof:heap=sites,cpu=samples,depth=10,monitor=y,thread=y,doe=y \
    java.lang.VMPlatform $*
