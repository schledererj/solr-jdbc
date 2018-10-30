A simple Solr JDBC connection holder that can be injected by JNDI.
==================

![travis ci build status](https://travis-ci.org/shopping24/solr-jdbc.png)

# JDBC support for Solr

This project allows for loading data via JDBC and making it available to Solr components, e.g. token filters and
data import handlers. The data sources can be defined via JNDI or via the solr.xml.

Based on that there are Solr synonym filters for reading synonyms and stop words out of JDBC.

## solr.xml data sources

To define data sources during startup of Solr you need a custom component in your solr.xml: 
The ConfiguringHttpShardHandlerFactory. This is a standard HttpShardHandlerFactory which additionally
creates some global unique beans, e.g. data sources. It is something like a poor mans IOC container.

The ConfiguringHttpShardHandlerFactory has an additional config option "beans",
being a named list containing all bean definitions. 
A bean definition is a named list which name will be used as bean name.
It contains a property "class" which defines the bean class. 
All other properties are properties of the bean which will be set 
after the bean instance has been created via its default constructor.

Required libs in the `lib` folder of Solr:

* [`solr-jdbc-<VERSION>-jar-with-dependencies.jar`](https://github.com/shopping24/solr-jdbc-synonyms/releases/download/v2.2.0/solr-jdbc-synonyms-2.2.0-jar-with-dependencies.jar) 
* Database pool of your choice, e.g. dbcp2.
* Your SQL driver, e.g. postgresql.

### Example using data source defined in solr.xml

Data sources are defined in your solr.xml:

    <shardHandlerFactory name="shardHandlerFactory" class="com.s24.search.solr.ConfiguringHttpShardHandlerFactory">
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

Required libs in the `lib` folder of Solr:
* [`solr-jdbc-<VERSION>-jar-with-dependencies.jar`](https://github.com/shopping24/solr-jdbc-synonyms/releases/download/v2.1.0/solr-jdbc-synonyms-2.1.0-jar-with-dependencies.jar) 

Required libs in the `lib` folder of the servlet container:
* Your SQL driver, e.g. postgresql.

### Example using a Tomcat data source

Data sources are defined in your Tomcat context.xml (e.g. <tomcat>/conf/Catalina/localhost/solr.xml), see
[Look here for more information on configuring a JDBC pool in Tomcat](http://tomcat.apache.org/tomcat-7.0-doc/jndi-datasource-examples-howto.html):

    <Resource name="jdbc/someName" auth="Container" type="javax.sql.DataSource" factory="org.apache.tomcat.jdbc.pool.DataSourceFactory"
              driverClassName="org.postgresql.Driver" 
              url="jdbc:postgresql://localhost:5432/database" defaultAutoCommit="true"
              username="username" password="password"
              maxActive="2" maxIdle="2" minIdle="1" maxWait="10000" validationQuery="select 1" testWhileIdle="true" /> 

In your Solr schema.xml you can reference that data source via its name:

    <filter class="com.s24.search.solr.analysis.jdbc.JdbcSynonymFilterFactory"
            dataSource="jdbc/shopping24-search" sql="select * from synonyms"
            ignoreMissingDatabase="true" ignoreCase="true" expand="false"/>

## Configuring the synonym filter

The `JdbcSynonymFilterFactory` behaves exactly like the Solr 
[`SynonymFilterFactory`](https://wiki.apache.org/solr/AnalyzersTokenizersTokenFilters#solr.SynonymFilterFactory),
except that it does load the synonyms from a JDBC database and not from a file resource.
Configure the filter in your Solr analyzer chain like this:

    <filter class="com.s24.search.solr.analysis.jdbc.JdbcSynonymFilterFactory"   
            sql="SELECT concat(left, '=>', array_to_string(right, ',')) as line FROM synonyms;" 
            dataSource="jdbc/synonyms" ignoreCase="false" expand="true" />

The filter takes two arguments over the 
[`SynonymFilterFactory`](https://wiki.apache.org/solr/AnalyzersTokenizersTokenFilters#solr.SynonymFilterFactory):
	
* `dataSource`: The name of your JDBC `DataSource` as configured. In the example above, this would be `jdbc/synonyms`.
   
* `sql`: A SQL statement returning valid Solr synonym lines in the first SQL result column.  
  * Valid synonym formats include `x=>a`, `x=>a,b,c`, `x,y=>a,b,c` or `x,a,b,c`.
  * You might have your left and right hand side of your synonym definitions stored
    in separate columns in your database. Use a `concat` function to create a valid synonym line.
    * In [PostgreSQL](http://www.postgresql.org/docs/9.3/static/functions-string.html), you might use `SELECT concat(lhs, '=>', rhs) as line FROM synonyms;`
    * In [PostgreSQL](http://www.postgresql.org/docs/9.3/static/functions-array.html) with arrays, you might use `SELECT concat(lhs, '=>', array_to_string(rhs, ',')) as line FROM synonyms;`
    * In [Mysql](http://dev.mysql.com/doc/refman/5.6/en/string-functions.html#function_concat) your might use `SELECT concat(lhs, '=>', rhs) as line FROM synonyms;`

A complete field type might look like this example:

	<fieldType name="synonym_test" class="solr.TextField">
         <analyzer>
            <tokenizer class="solr.PatternTokenizerFactory" pattern="[\s]+" />
            <filter class="com.s24.search.solr.analysis.jdbc.JdbcSynonymFilterFactory"   
               sql="SELECT concat(left, '=>', array_to_string(right, ',')) as line FROM synonyms;" 
               jndiName="jdbc/synonyms" ignoreCase="false" expand="true" />
         </analyzer>
      </fieldType>

## Configuring the stop word filter

Since version 1.1 there's a `JdbcStopFilterFactory` available, that reads stopwords from a JDBC database. 
It behaves exactly like the Solr [`StopFilterFactory`](https://wiki.apache.org/solr/AnalyzersTokenizersTokenFilters#solr.StopFilterFactory)
and is meant to be a drop-in replacement:

    <filter class="com.s24.search.solr.analysis.jdbc.JdbcStopFilterFactory"   
            sql="SELECT stopword FROM stopwords" 
            jndiName="jdbc/synonyms"/>

The filter has the same configuration parameters as the `JdbcSynonymFilterFactory`.

# Using data import handlers

Since 2.2 there's a `DataImportJdbcDataSource` available, that enables data import handlers 
to use a data source like defined above. In your data handler configuration use:

    <dataConfig>
        <dataSource type="com.s24.search.solr.analysis.jdbc.DataImportJdbcDataSource" 
                    name="dataImportName" 
                    dataSource="jdbc/dataSourceName" />
        ...
    </dataConfig>

## Building the project

This should install the current version into your local repository

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
