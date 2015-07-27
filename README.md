A simple Solr JDBC connection holder that can be injected by JNDI.
==================

![travis ci build status](https://travis-ci.org/shopping24/solr-jdbc.png)

# JDBC support for Solr

This project allows for loading data via JDBC and making it available to Solr components, e.g. token filters.
The data sources can be defined via JNDI or via the solr.xml.

## solr.xml data sources

To define data sources during startup of Solr you need a custom component in your solr.xml: 
The ConfiguringHttShardHandlerFactory. This is a standard HttpShardHandlerFactory which additionally
creates some global unique beans, e.g. data sources. It is something like a poor mans IOC container.

The ConfiguringHttShardHandlerFactory has an additional config option "beans".
This a named list containing all bean definitions. 
A bean definition is a named list which name will be used as bean name.
It contains a property "class" which defines the bean class. 
All other properties are properties of the bean which will be set 
after the bean instance has been created via its default constructor.

### Example using data source defined in solr.xml

Data sources are defined in your solr.xml:

    <shardHandlerFactory name="shardHandlerFactory" class="com.s24.search.solr.ConfiguringHttShardHandlerFactory">
       <lst name="beans">
          <lst name="jdbc/shopping24-search">
            <str name="class">org.apache.commons.dbcp2.BasicDataSource</str>
            <str name="driverClassName">org.postgresql.Driver</str>
            <str name="url">jdbc:postgresql://localhost:5432/database</str>
            <str name="username">username</str>
            <str name="password">password</str>
            <str name="maxIdle">3</str>
            <str name="maxTotal">10</str>
            <str name="maxWaitMillis">10000</str>
          </lst>
          ...
       </lst>
       ...
    </shardHandlerFactory>


In your Solr schema.xml you can reference that data source via its name:

    <filter class="com.s24.search.solr.analysis.jdbc.JdbcSynonymFilterFactory"
            dataSource="jdbc/shopping24-search" sql="select * from synonyms"
            ignoreMissingDatabase="true" ignoreCase="true" expand="false"/>

## JNDI data sources

Defining data sources via JNDI requires Solr to be deployed into a server with JNDI support.
The standard Solr installation does not support JNDI and needs to be patched to support it.

### Example using a Tomcat data source

Data sources are defined in your Tomcat context.xml:

    <Resource name="jdbc/someName" auth="Container" type="javax.sql.DataSource" factory="org.apache.tomcat.jdbc.pool.DataSourceFactory"
              driverClassName="org.postgresql.Driver" 
              url="jdbc:postgresql://localhost:5432/database" defaultAutoCommit="true"
              username="username" password="password"
              maxActive="2" maxIdle="2" minIdle="1" maxWait="10000" validationQuery="select 1" testWhileIdle="true" /> 

In your Solr schema.xml you can reference that data source via its name:

    <filter class="com.s24.search.solr.analysis.jdbc.JdbcSynonymFilterFactory"
            dataSource="jdbc/shopping24-search" sql="select * from synonyms"
            ignoreMissingDatabase="true" ignoreCase="true" expand="false"/>


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
    
Wait for the relase to be accepted. Then increment to next development version:
    
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
