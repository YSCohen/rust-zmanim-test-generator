#!/bin/bash

# Each generator writes its own test file(s) into the directory given as $1

for gen in GenerateAcTests GenerateCzcTests GenerateCzcShaahTests GenerateSolarPositionTests; do
    mvn exec:java -Dexec.mainClass="io.github.YSCohen.rustZmanimTestGenerator.$gen" -Dexec.arguments="$1" --quiet
done
