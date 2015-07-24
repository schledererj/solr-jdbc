package com.s24.search.solr.analysis.jdbc;

import java.util.Map;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

/**
 * Factory to create {@link JdbcReader}s from a Solr params.
 */
@SuppressWarnings("unused") // API
public class JdbcReaderFactory {
   /**
    * Logger.
    */
   private static final Logger log = LoggerFactory.getLogger(JdbcReaderFactory.class);

   /**
    * Create the {@link JdbcReader}.
    *
    * Set synonyms file to a fixed name.
    * This is needed because our patched resource loader should load the synonyms exactly once.
    *
    * @param args
    *           Configuration.
    * @param originalParamName
    *           Default synonym file name.
    * @return JdbcReader.
    */
   @SuppressWarnings("unused") // API
   public static JdbcReader createFromSolrParams(Map<String, String> args, String originalParamName) {
      Preconditions.checkNotNull(args);

      // Set a fixed synonyms "file".
      // This "file" will be loaded from the database by the JdbcResourceLoader.
      args.put(originalParamName, JdbcResourceLoader.DATABASE);

      String sql = args.remove(JdbcReaderFactoryParams.SQL.toString());
      String ignoreString = args.remove(JdbcReaderFactoryParams.IGNORE.toString());
      boolean ignore = !"false".equals(ignoreString);

      String dataSourceName = args.remove(JdbcReaderFactoryParams.DATASOURCE.toString());
      String jndiName = args.remove(JdbcReaderFactoryParams.JNDI_NAME.toString());

      DataSource dataSource = null;
      if (dataSourceName != null) {
         dataSource = solrDataSource(dataSourceName);
      } else if (jndiName != null) {
         dataSource = jndiDataSource(fixJndiName(jndiName));
      } else {
         log.error("No data source configured.");
      }

      if (dataSource == null) {
         if (!ignore) {
            throw new IllegalArgumentException("No data source found.");
         }
      }

      return new SimpleJdbcReader(dataSource, sql, ignore);
   }

   /**
    * Look up data source via {@link JdbcDataSourceFactory}.
    *
    * @param dataSourceName Name of data source.
    */
   private static DataSource solrDataSource(String dataSourceName) {
      DataSource dataSource = JdbcDataSourceFactory.lookUp(dataSourceName);
      if (dataSource == null) {
         log.error("Data source {} not found.", dataSourceName);
         return null;
      }

      return dataSource;
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
    *
    * @param jndiName JNDI name of data source.
    */
   private static DataSource jndiDataSource(String jndiName) {
      try {
         Context ctx = new InitialContext();
         log.info("Looking up data source {} in JNDI.", jndiName);
         DataSource dataSource = (DataSource) ctx.lookup(jndiName);
         ctx.close();
         return dataSource;

      } catch (NameNotFoundException e) {
         log.error("Data source {} not found: {}.", jndiName, e.getMessage());
         return null;

      } catch (NamingException e) {
         log.error("JNDI error: {}.", e.getMessage());
         throw new IllegalArgumentException("JNDI error.", e);
      } catch (ClassCastException e) {
         log.error("The JNDI resource {} is no data source: {}.", jndiName, e.getMessage());
         throw new IllegalArgumentException("The JNDI resource is no data source.", e);
      }
   }
}
