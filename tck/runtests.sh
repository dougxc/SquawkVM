#!/usr/bin/jsh
# Copyright (c) 2000 by Sun Microsystems, Inc.
# All rights reserved.

trap killrun 2 3 4 6 9 10 15

pids=""
id=`/usr/bin/who am i | awk '{print $1}'`.$$
pfile=/tmp/passed.txt.$id
ffile=/tmp/failed.txt.$id
lfile=/tmp/log.txt.$id

killrun () {
     /usr/bin/echo "\nSee $pfile $ffile $lfile"
     exit 1
}


if [ $# -lt 1 ]
then
  echo "Usage: `basename $0` <java interpreter>"
  exit 1
fi

interpreter=$1

if [ ! -f $interpreter ]
then
  echo "No such  file : $interpreter"
  exit 1
fi

CLASSPATH=./classes:./sharedclasses:./agent.jar
export CLASSPATH 

args=""
rm -f $pfile $ffile $lfile

sdate=`date`
echo $sdate > $pfile
echo $sdate > $ffile
echo $sdate > $lfile

echo "The tests started. See tail -f $lfile"

classlist=./positiveclasses.txt
for test in `cat $classlist| sed -e 's/ /|/g'`
do
    args=`echo $test | sed -e 's/|/ /g'`
    echo "$interpreter $args " >> $lfile
    $interpreter $args >> $lfile 2>&1
    
    if [ $? -ne 95 ]
    then
     echo $args >> $ffile
    else
      echo "$args  PASSED" >> $pfile    
    fi    
done

classlist=./negativeclasses.txt
for test in `cat $classlist| sed -e 's/ /|/g'`
do
    args=`echo $test | sed -e 's/|/ /g'`
    echo "$interpreter $args " >> $lfile
    $interpreter $args >> $lfile 2>&1
    
    if [ $? -eq 95 ]
    then
     echo $args >> $ffile
    else
      echo "$args  PASSED" >> $pfile    
    fi    
done


fdate=`date`

echo $fdate >> $pfile
echo $fdate >> $ffile
echo $fdate >> $lfile

plines=`cat $pfile | wc -l`
flines=`cat $ffile| wc -l`
echo "Passed: `expr $plines - 2`"
echo "Failed: `expr $flines - 2`"
echo "The results are stored in files: $pfile, $ffile, $lfile"
