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
import com.s24.search.solr.ConfiguringHttpShardHandlerFactory;

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
    * Create the {@link JdbcReader} from configuration.
    *
    * Set synonyms file to a fixed name.
    * This is needed because our patched resource loader should load the synonyms exactly once.
    *
    * @param config
    *           Configuration.
    * @param originalParamName
    *           Default synonym file name.
    * @return JdbcReader.
    */
   @SuppressWarnings("unused") // API
   public static JdbcReader createFromSolrParams(Map<String, String> config, String originalParamName) {
      Preconditions.checkNotNull(config);

      // Set a fixed synonyms "file".
      // This "file" will be loaded from the database by the JdbcResourceLoader.
      if (originalParamName != null) {
         config.put(originalParamName, JdbcResourceLoader.DATABASE);
      }

      String sql = config.remove(JdbcReaderFactoryParams.SQL);
      String ignoreString = config.remove(JdbcReaderFactoryParams.IGNORE);
      boolean ignore = !"false".equals(ignoreString);

      String dataSourceName = config.remove(JdbcReaderFactoryParams.DATASOURCE);
      DataSource dataSource = null;
      if (dataSourceName != null) {
         dataSource = ConfiguringHttpShardHandlerFactory.lookUp(dataSourceName, DataSource.class);
         if (dataSource == null) {
            dataSource = jndiDataSource(fixJndiName(dataSourceName));
         }

         if (dataSource == null) {
            log.error("Data source {} not found.", dataSourceName);
            if (!ignore) {
               throw new IllegalArgumentException("No data source found.");
            }
         }
         } else {
         log.error("No data source configured.");
         if (!ignore) {
            throw new IllegalArgumentException("No data source configured.");
         }
      }

      return new SimpleJdbcReader(dataSource, sql, ignore);
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
