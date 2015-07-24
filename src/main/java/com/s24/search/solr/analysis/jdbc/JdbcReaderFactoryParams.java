package com.s24.search.solr.analysis.jdbc;

/**
 * Additional Parameters for configuring JDBC based readers.
 * 
 * @author Shopping24 GmbH, Torsten Bøgh Köster (@tboeghk)
 */
public enum JdbcReaderFactoryParams {
   /**
    * Parameter: Name of data source in {@link JdbcDataSourceFactory} or in JNDI.
    */
   DATASOURCE("dataSource"),

   /**
    * Parameter: SQL to load synonyms.
    */
   SQL("sql"),

   /**
    * Parameter: Ignore a missing database?.
    */
   IGNORE("ignoreMissingDatabase");

   /**
    * Name of this parameter.
    */
   private final String name;

   /**
    * Constructor.
    *
    * @param name Name of the parameter.
    */
   JdbcReaderFactoryParams(String name) {
      this.name = name;
   }

   @Override
   public String toString() {
      return name;
   }
}
