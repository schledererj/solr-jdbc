package com.s24.search.solr.analysis.jdbc;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Reader;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

/**
 * A configurable {@linkplain JdbcReader} that executes a given SQL statement on a configured JNDI data source.
 *
 * @author Shopping24 GmbH, Torsten Bøgh Köster (@tboeghk)
 */
public abstract class AbstractJdbcReader implements JdbcReader {
   /**
    * Logger.
    */
   private static final Logger logger = LoggerFactory.getLogger(AbstractJdbcReader.class);

   /**
    * SQL to load synonyms.
    */
   private final String sql;

   /**
    * Ignore a missing database?.
    */
   protected final boolean ignore;

   /**
    * The data source.
    */
   protected DataSource dataSource = null;

   /**
    * Default single line {@link ResultSetHandler}.
    */
   private final static ResultSetHandler<List<String>> SINGLE_LINE_RESULT_SET_HANDLER = rs -> {
      List<String> result = Lists.newArrayList();
      while (rs.next()) {
         result.add(rs.getString(1));
      }
      return result;
   };

   /**
    * Constructor.
    * Concrete constructors of sub classes should invoke {@link #checkDatasource()}.
    *
    * @param sql
    *           SQL.
    * @param ignore
    *           Ignore a missing database?.
    */
   protected AbstractJdbcReader(String sql, boolean ignore) {
      this.sql = checkNotNull(sql);
      this.ignore = ignore;
   }

   /**
    * Check if the datasource is able to provide connections.
    */
   protected final void checkDatasource() {
      // Check database connection information of data source
      if (dataSource != null) {
         //noinspection unused
         try (Connection connection = dataSource.getConnection()) {
            // Just get the connection to check if data source parameters are configured correctly.
         } catch (SQLException e) {
            dataSource = null;
            logger.error("Failed to connect to database of data source: {}.", e.getMessage());
            if (!ignore) {
               throw new IllegalArgumentException("Failed to connect to the database.", e);
            }
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Reader getReader() {
      if (dataSource == null) {
         if (ignore) {
            return new StringReader("");
         }
         throw new IllegalArgumentException("Missing data source.");
      }

      QueryRunner runner = new QueryRunner(dataSource);
      try {
         logger.info("Querying for data using {}", sql);
         List<String> content = runner.query(sql, SINGLE_LINE_RESULT_SET_HANDLER);
         logger.info("Loaded {} lines", content.size());

         // return joined
         return new StringReader(Joiner.on('\n').join(content));
      } catch (SQLException e) {
         throw new IllegalArgumentException("Failed to load data from the database", e);
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public QueryRunner getJdbcRunner() {
      if (dataSource == null) {
         if (ignore) {
            logger.warn("Could not load Jdbc Datasource!");
            return null;
         }
         throw new IllegalArgumentException("Missing data source.");
      }

      return new QueryRunner(dataSource);
   }

   @Override
   public String getSql() {
      return sql;
   }
}
