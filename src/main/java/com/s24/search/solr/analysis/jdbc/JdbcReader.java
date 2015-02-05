package com.s24.search.solr.analysis.jdbc;

import java.io.Reader;

import org.apache.commons.dbutils.QueryRunner;

/**
 * Reads "lines" of configuration out of JDBC.
 *
 * @author Shopping24 GmbH, Torsten Bøgh Köster (@tboeghk)
 */
public interface JdbcReader {
   /**
    *
    * @return a {@linkplain Reader}, never <code>null</code>
    */
   Reader getReader();

   /**
    * @return direct {@link QueryRunner} to execute query. If no datasource is defined, it could be null.
    */
   QueryRunner getJdbcRunner();

   /**
    * @return plain SQL query.
    */
   String getSql();
}
