# rust-zmanim-test-generator
Automatic tests generator for [rust-zmanim](https://github.com/YSCohen/rust-zmanim), using results from [KosherJava](https://github.com/KosherJava/zmanim)

Yeah, it may be ugly, but it does the job

To run, you need to have KosherJava installed in your local repository. In the KosherJava repo, run
```
mvn clean install -Dmaven.test.skip=true -Dmaven.javadoc.skip=true
```

Then, to generate the tests, run the following in this repo:
```
# for sea-level tests
mvn exec:java -Dexec.mainClass="com.example.App" --quiet

# for elevation-adjusted tests
mvn exec:java -Dexec.mainClass="com.example.App" -Dexec.args="elev" --quiet
```
