package com.s24.search.solr.analysis.jdbc;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Reader;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

/**
 * A configurable {@linkplain JdbcReader} that executes a given SQL statement on a configured JNDI data source.
 *
 * @author Shopping24 GmbH, Torsten Bøgh Köster (@tboeghk)
 */
public class JndiJdbcReader implements JdbcReader {
   /**
    * Logger.
    */
   private static final Logger logger = LoggerFactory.getLogger(JndiJdbcReader.class);

   /**
    * JNDI name of the data source.
    */
   private final String jndiName;

   /**
    * SQL to load synonyms.
    */
   private final String sql;

   /**
    * Ignore a missing database?.
    */
   private final boolean ignore;

   /**
    * The data source.
    */
   private DataSource dataSource = null;

   /**
    * Default single line {@link ResultSetHandler}.
    */
   private final static ResultSetHandler<List<String>> SINGLE_LINE_RESULT_SET_HANDLER = new ResultSetHandler<List<String>>() {
      @Override
      public List<String> handle(ResultSet rs) throws SQLException {
         List<String> result = Lists.newArrayList();
         while (rs.next()) {
            result.add(rs.getString(1));
         }
         return result;
      }
   };


   /**
    * Create the {@link JndiJdbcReader}. Set synonyms file to a fixed name. This is needed because our patched resource
    * loader should load the synonyms exactly once.
    *
    * @param args
    *           Configuration.
    * @return Configuration.
    */
   public static JndiJdbcReader createFromSolrParams(Map<String, String> args, String originalParamName) {
      Preconditions.checkNotNull(args);

      // Set a fixed synonyms "file".
      // This "file" will be loaded from the database by the JdbcResourceLoader.
      args.put(originalParamName, JdbcResourceLoader.DATABASE);

      String name = args.remove(JdbcFilterFactoryParams.JNDI_NAME.toString());
      String sql = args.remove(JdbcFilterFactoryParams.SQL.toString());
      String ignore = args.remove(JdbcFilterFactoryParams.IGNORE.toString());
      return new JndiJdbcReader(name, sql, "true".equals(ignore));
   }

   /**
    * Constructor.
    *
    * @param jndiName
    *           JNDI name.
    * @param sql
    *           SQL.
    * @param specificResultSetHandler
    * @param ignoreMissingDatabase
    *           Ignore a missing database?.
    */
   public JndiJdbcReader(String jndiName, String sql, boolean ignore) {
      this.jndiName = fixJndiName(checkNotNull(jndiName));
      this.sql = checkNotNull(sql);
      this.ignore = ignore;

      initDatabase();
   }

   /**
    * Add prefix "java:comp/env/" to the JNDI name, if it is missing.
    *
    * @param jndiName
    *           JNDI name.
    */
   private static String fixJndiName(String jndiName) {
      return jndiName.startsWith("java:comp/env/") ? jndiName : "java:comp/env/" + jndiName;
   }

   /**
    * Initializes the database and lookups a {@linkplain DataSource} in JNDI.
    */
   protected void initDatabase() {
      try {
         Context ctx = new InitialContext();
         logger.info("Looking up data source {} in JNDI.", jndiName);
         dataSource = (DataSource) ctx.lookup(jndiName);
         ctx.close();
      } catch (NameNotFoundException e) {
         logger.error("Data source {} not found.", jndiName, e);
         if (!ignore) {
            throw new IllegalArgumentException("Missing data source.", e);
         }
      } catch (NamingException e) {
         logger.error("JNDI error.", e);
         throw new IllegalArgumentException("JNDI error.", e);
      } catch (ClassCastException e) {
         logger.error("The JNDI resource {} is no data source.", jndiName, e);
         throw new IllegalArgumentException("The JNDI resource is no data source.", e);
      }

      // Check database connection information of data source
      if (dataSource != null) {
         try (Connection connection = dataSource.getConnection()) {
            // Just get the connection to check if data source parameters are
            // configured correctly.
         } catch (SQLException e) {
            dataSource = null;
            logger.error("Failed to connect to database of data source {}.", jndiName, e);
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
