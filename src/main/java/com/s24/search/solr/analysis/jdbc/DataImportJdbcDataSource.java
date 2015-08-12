package com.s24.search.solr.analysis.jdbc;


import java.sql.Connection;
import java.util.Properties;
import java.util.concurrent.Callable;

import org.apache.solr.handler.dataimport.Context;
import org.apache.solr.handler.dataimport.JdbcDataSource;

/**
 * {@link JdbcDataSource} for data import handlers using data sources from {@link JdbcDataSourceFactory}.
 */
public class DataImportJdbcDataSource extends JdbcDataSource {
   @Override
   protected Callable<Connection> createConnectionFactory(Context context, Properties initProps) {
      // Suppress failures due to not configured data source.
      initProps.setProperty(JNDI_NAME, "dummy");
      // Init some internal parameters of JdbcDataSource
      super.createConnectionFactory(context, initProps);

      javax.sql.DataSource dataSource = JdbcDataSourceFactory.getDataSource(initProps);
      return factory = dataSource::getConnection;
   }
}
