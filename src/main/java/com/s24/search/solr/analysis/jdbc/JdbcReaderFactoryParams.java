package com.s24.search.solr.analysis.jdbc;

import com.s24.search.solr.ConfiguringHttpShardHandlerFactory;

/**
 * Additional Parameters for configuring JDBC based readers.
 * 
 * @author Shopping24 GmbH, Torsten Bøgh Köster (@tboeghk)
 */
public interface JdbcReaderFactoryParams {
   /**
    * Parameter: Name of data source in {@link ConfiguringHttpShardHandlerFactory} or in JNDI.
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
}
