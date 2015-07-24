package com.s24.search.solr.analysis.jdbc;

import static com.google.common.base.Preconditions.checkNotNull;

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
 * A configurable {@linkplain JdbcReader} that executes a given SQL statement on a configured JNDI data source.
 *
 * @author Shopping24 GmbH, Torsten Bøgh Köster (@tboeghk)
 */
public class JndiJdbcReader extends AbstractJdbcReader {
   /**
    * Logger.
    */
   private static final Logger logger = LoggerFactory.getLogger(JndiJdbcReader.class);

   /**
    * JNDI name of the data source.
    */
   private final String jndiName;

   /**
    * Create the {@link JndiJdbcReader}. Set synonyms file to a fixed name. This is needed because our patched resource
    * loader should load the synonyms exactly once.
    *
    * @param args
    *           Configuration.
    * @param originalParamName
    *           Default synonym file name.
    * @return Configuration.
    */
   @SuppressWarnings("unused") // API
   public static JndiJdbcReader createFromSolrParams(Map<String, String> args, String originalParamName) {
      Preconditions.checkNotNull(args);

      // Set a fixed synonyms "file".
      // This "file" will be loaded from the database by the JdbcResourceLoader.
      args.put(originalParamName, JdbcResourceLoader.DATABASE);

      String name = args.remove(JdbcParams.JNDI_NAME.toString());
      String sql = args.remove(JdbcParams.SQL.toString());
      String ignore = args.remove(JdbcParams.IGNORE.toString());
      return new JndiJdbcReader(name, sql, "true".equals(ignore));
   }

   /**
    * Constructor.
    *
    * @param jndiName
    *           JNDI name.
    * @param sql
    *           SQL.
    * @param ignore
    *           Ignore a missing database?.
    */
   public JndiJdbcReader(String jndiName, String sql, boolean ignore) {
      super(sql, ignore);

      this.jndiName = fixJndiName(checkNotNull(jndiName));

      init();
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
   private void init() {
      try {
         Context ctx = new InitialContext();
         logger.info("Looking up data source {} in JNDI.", jndiName);
         dataSource = (DataSource) ctx.lookup(jndiName);
         ctx.close();
      } catch (NameNotFoundException e) {
         logger.error("Data source {} not found: {}.", jndiName, e.getMessage());
         if (!ignore) {
            throw new IllegalArgumentException("Missing data source.", e);
         }
      } catch (NamingException e) {
         logger.error("JNDI error: {}.", e.getMessage());
         throw new IllegalArgumentException("JNDI error.", e);
      } catch (ClassCastException e) {
         logger.error("The JNDI resource {} is no data source: {}.", jndiName, e.getMessage());
         throw new IllegalArgumentException("The JNDI resource is no data source.", e);
      }

      checkDatasource();
   }
}
