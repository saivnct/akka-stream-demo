#!/bin/bash


JAVAENV="$JAVAENV -Dlog4j.configurationFile=log4j2.properties"

java $JAVAENV -jar target/akka-research-1.0-SNAPSHOT-jar-with-dependencies.jar