A simple Solr JDBC connection holder that can be injected by JNDI.
==================

![travis ci build status](https://travis-ci.org/shopping24/solr-jdbc.png)

...

## Building the project

This should install the current version into your local repository

    $ export JAVA_HOME=$(/usr/libexec/java_home -v 1.7)
    $ export MAVEN_OPTS="-Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true -Dmaven.wagon.http.ssl.ignore.validity.dates=true"
    $ mvn clean install
    
### Releasing the project to maven central
    
Define new versions
    
    $ export NEXT_VERSION=<version>
    $ export NEXT_DEVELOPMENT_VERSION=<version>-SNAPSHOT

Then execute the release chain

    $ mvn org.codehaus.mojo:versions-maven-plugin:2.0:set -DgenerateBackupPoms=false -DnewVersion=$NEXT_VERSION
    $ git commit -a -m "pushes to release version $NEXT_VERSION"
    $ mvn -P release
    
Log into [OSS](https://oss.sonatype.org/#stagingRepositories), look for the `coms24-XXXX` repository and
"close" it. If all checks passed, "release" it in OSS. Then, increment to next development version:
    
    $ git tag -a v$NEXT_VERSION -m "`curl -s http://whatthecommit.com/index.txt`"
    $ mvn org.codehaus.mojo:versions-maven-plugin:2.0:set -DgenerateBackupPoms=false -DnewVersion=$NEXT_DEVELOPMENT_VERSION
    $ git commit -a -m "pushes to development version $NEXT_DEVELOPMENT_VERSION"
    $ git push origin tag v$NEXT_VERSION && git push origin

Some link regarding Maven central deployment:

* http://central.sonatype.org/pages/ossrh-guide.html
* http://central.sonatype.org/pages/apache-maven.html
* http://central.sonatype.org/pages/working-with-pgp-signatures.html#generating-a-key-pair
* http://maven.apache.org/guides/mini/guide-encryption.html
* http://central.sonatype.org/pages/releasing-the-deployment.html
* https://oss.sonatype.org/#stagingRepositories

## License

This project is licensed under the [Apache License, Version 2](http://www.apache.org/licenses/LICENSE-2.0.html).
