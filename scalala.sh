#!/bin/sh

BASE=`readlink -f $0 | xargs dirname`;
JAR=`find $BASE/target/ -name 'scalala*-jar-with-dependencies.jar' 2>/dev/null | head -1`

# find jar in target/
if [ ! -e "$JAR" ] ; then
  echo "Could not find target/scalala*-jar-with-dependencies.jar."
  echo "Make sure to run mvn assembly:assembly first."
  exit 1;
fi

## find SCALA_HOME if none set and scala is in the path
#if [ -z "$SCALA_HOME" ] ; then
#  SCALA_HOME=`which scala | xargs dirname | xargs dirname`
#fi

# set up classpath, including jline if available
export CLASSPATH=$JAR
#if [ -e "$SCALA_HOME/lib/jline.jar" ] ; then
#  export CP=$CP:$SCALA_HOME/lib/jline.jar
#fi

scala scalala.ScalalaConsole

