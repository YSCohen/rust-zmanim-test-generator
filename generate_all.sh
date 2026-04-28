#!/bin/bash

mvn exec:java -Dexec.mainClass="io.github.YSCohen.rustZmanimTestGenerator.GenerateAcTests" --quiet > "$1/test_ac_generated.rs"

mvn exec:java -Dexec.mainClass="io.github.YSCohen.rustZmanimTestGenerator.GenerateCzcTests" --quiet > "$1/test_czc_generated_sea_level.rs"
mvn exec:java -Dexec.mainClass="io.github.YSCohen.rustZmanimTestGenerator.GenerateCzcTests" -Dexec.args="elev" --quiet > "$1/test_czc_generated_elevation.rs"

mvn exec:java -Dexec.mainClass="io.github.YSCohen.rustZmanimTestGenerator.GenerateCzcShaahTests" --quiet > "$1/test_czc_shaah_generated_sea_level.rs"
mvn exec:java -Dexec.mainClass="io.github.YSCohen.rustZmanimTestGenerator.GenerateCzcShaahTests" -Dexec.args="elev" --quiet > "$1/test_czc_shaah_generated_elevation.rs"
