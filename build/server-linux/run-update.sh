#!/bin/sh

# assume a local JVM would be in the 'jre' directory
export PATH=./jre/bin:$PATH

# run the code forever - commented out for now - need a safe way to shut down current Tranche server.
#while true
#do
  # build up the classpath
  LIB="."
  # add all the JARs
  for FILE in $( ls lib/*  )
  do
    LIB=$LIB:$FILE
  done

  # make the run command
  RUN_UPDATE="java -Xmx${1-1512}m -Djava.io.tmpdir=./temp -classpath $LIB org.proteomecommons.tranche.AutoUpdater"

  # try to update the code
  $RUN_UPDATE


  # build up the classpath again, just in case JARs are added or removed
  LIB="."
  # add all the JARs
  for FILE in $( ls lib/*  )
  do
    LIB=$LIB:$FILE
  done

  # make the run command
  RUN="java -Xmx${1-1512}m -Djava.io.tmpdir=./temp -classpath $LIB org.proteomecommons.tranche.LocalDataServer"

  # run the code
  $RUN

#  # wait for a week
#  sleep 604800
#done

