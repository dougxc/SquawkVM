# Doug's version of the launcher
if [ $# -gt 0 -a "$1" = "builder" ]; then 
    cd builder;
    bld.sh
    cd ..
    exit
fi

# Find all available C compilers ...
comp=""
#if [ -z "$comp" ]; then cl -v      >/dev/null 2>&1 && comp="-msc"; fi
#if [ -z "$comp" ]; then gcc -v     >/dev/null 2>&1 && comp="-gcc"; fi
#if [ -z "$comp" ]; then cc -flags  >/dev/null 2>&1 && comp="-cc";  fi
cl -v      >/dev/null 2>&1 && comp="$comp -msc"
cc -flags  >/dev/null 2>&1 && comp="$comp -cc"
gcc -v     >/dev/null 2>&1 && comp="$comp -gcc"

cmd="java -cp ${JAVA_HOME}/lib/tools.jar:build.jar Build -noshell $comp $*"
echo $cmd
exec $cmd
