#
# Build the classes in a JBuilder project file
if [ -n "`uname | grep -i 'win'`" ]; then
    sep=";"
else
    sep=":"
fi


if [ $# -ne 1 ]; then
    echo "usage: $0 <jpx file>"
    exit
fi

JPX=$1

files=`sed -n '/<file path=/s:.*"\([^"]*\)".*:\1:p' < $JPX | tr '\n' ' '`
dir=`sed -n '/category="sys" name="OutPath"/s:.*value="\([^"]*\)".*:\1:p' <$JPX`
sp=`sed -n '/category="sys" name="SourcePath"/s:.*value="\([^"]*\)".*:\1:p' <$JPX | eval "tr ';' '${sep}'"`

if [ -n "$sp" ]; then
  sp="-sourcepath \"$sp\""
else
  sp=""
fi

echo "Compiling ..."
cmd="javac -g $sp -d $dir $files"
echo $cmd
eval $cmd
if [ "$JPX" = "j2me.jpx" ]; then
    echo "Preverifying ..."
    cmd="./tools/preverify -d j2me/classes $dir"
    echo $cmd
    eval $cmd
fi
