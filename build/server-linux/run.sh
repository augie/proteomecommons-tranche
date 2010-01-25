#!/bin/sh

# assume a local JVM would be in the 'jre' directory
export PATH=./jre/bin:$PATH

# This is a helper script to run the newsbot
LIB="."

# add all the JARs from the './lib' directory
for FILE in $( ls ./lib/*  )
do
  LIB=$LIB:$FILE
done

# make the run command fallback on 512m of JVM memory
RUN="java -Xmx${1-1512}m -Djava.io.tmpdir=./temp -classpath $LIB org.proteomecommons.tranche.LocalDataServer"

# run
$RUN
