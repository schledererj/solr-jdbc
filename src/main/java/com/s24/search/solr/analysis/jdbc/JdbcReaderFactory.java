package com.s24.search.solr.analysis.jdbc;

import java.util.Map;

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

      DataSource dataSource = JdbcDataSourceFactory.getDataSource(config);
      String sql = config.remove(JdbcReaderFactoryParams.SQL);
      String ignoreString = config.remove(JdbcReaderFactoryParams.IGNORE);
      boolean ignore = !"false".equals(ignoreString);

      return new SimpleJdbcReader(dataSource, sql, ignore);
   }
}
