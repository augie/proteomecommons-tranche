#!/bin/sh

# assume a local JVM would be in the 'jre' directory
export PATH=./jre/bin:$PATH

# build up the classpath
LIB="."

# add all the JARs
for FILE in $( ls ./lib/*  )
do
  LIB=$LIB:$FILE
done

# make the run command
RUN="java -Xmx512m -server -classpath $LIB org.proteomecommons.dfs.mirror.peptideatlas.Main"

# run the code
$RUN

