package com.s24.search.solr.analysis.jdbc;

/**
 * Additional Parameters for configuring JDBC based readers.
 * 
 * @author Shopping24 GmbH, Torsten Bøgh Köster (@tboeghk)
 */
public interface JdbcReaderFactoryParams {
   /**
    * Parameter: Name of data source in {@link JdbcDataSourceFactory} or in JNDI.
    */
   String DATASOURCE = "dataSource";

   /**
    * Parameter: SQL to load synonyms.
    */
   String SQL = "sql";

   /**
    * Parameter: Ignore a missing database?.
    */
   String IGNORE = "ignoreMissingDatabase";

   /**
    * Parameter prefix: Prefix for data source specific parameters.
    */
   String POOL = "pool";
}
