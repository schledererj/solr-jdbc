package com.s24.search.solr.analysis.jdbc;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource;

import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.BeanUtilsBean2;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.AbstractSolrEventListener;
import org.apache.solr.core.SolrCore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dummy field type which just creates a data source.
 */
@SuppressWarnings("unused") // API
public class JdbcDataSourceFactory extends AbstractSolrEventListener {
   /**
    * Logger.
    */
   private static final Logger log = LoggerFactory.getLogger(JdbcDataSourceFactory.class);

   /**
    * All data sources by name.
    */
   private static final Map<String, DataSource> dataSources = new ConcurrentHashMap<>();

   /**
    * Reflection helper.
    */
   private final BeanUtilsBean utils = new BeanUtilsBean2();

   /**
    * Look up database connection pool by name.
    *
    * @param name Name of database connection pool.
    */
   public static DataSource lookUp(String name) {
      return dataSources.get(name);
   }

   /**
    * Remove all registered data sources.
    */
   static void clear() {
      dataSources.clear();
   }

   /**
    * Constructor.
    *
    * @param core Core.
    */
   public JdbcDataSourceFactory(SolrCore core) {
      super(core);
   }

   @Override
   public void init(NamedList poolDefinition) {
      String name = (String) poolDefinition.remove(JdbcReaderFactoryParams.DATASOURCE);
      log.info("Registering data source {}.", name);
      String poolClassName = (String) poolDefinition.remove("class");
      NamedList<?> poolConfig = (NamedList<?>) poolDefinition.remove("params");
      // Ignore errors regarding the database connection pool?
      boolean ignore = !"false".equals(poolDefinition.remove(JdbcReaderFactoryParams.IGNORE));

      // Blocks other threads that are trying to create a data source with the same name.
      DataSource dataSource =
            dataSources.computeIfAbsent(name, poolName -> createDataSource(poolClassName, poolConfig, ignore));

      if (dataSource != null) {
         log.info("Successfully registered data source {}.", name);
      }
   }

   /**
    * Create database connection pool.
    *
    * @param poolClassName Database connection pool implementation.
    * @param poolConfig    Database connection pool configuration.
    * @param ignore        Ignore any errors?.
    */
   private DataSource createDataSource(String poolClassName, NamedList<?> poolConfig, boolean ignore) {
      Class<? extends DataSource> poolClass;
      DataSource pool;
      try {
         //noinspection unchecked
         poolClass = (Class<? extends DataSource>) Class.forName(poolClassName);
         pool = poolClass.newInstance();

      } catch (Exception e) {
         log.error("Failed to instantiate database connection pool {}: {}.", poolClassName, e.getMessage());
         if (ignore) {
            return null;
         }
         throw new IllegalArgumentException("Failed to instantiate database connection pool.", e);
      }

      for (Entry<String, ?> entry : poolConfig) {
         try {
            utils.setProperty(pool, entry.getKey(), entry.getValue());

         } catch (InvocationTargetException | IllegalAccessException e) {
            log.error("Failed to configure database connection pool {}#{}: {}.",
                  poolClassName, entry.getKey(), e.getMessage());
            if (ignore) {
               return null;
            }
            throw new IllegalArgumentException("Failed to configure database connection pool.", e);
         }
      }

      return pool;
   }
}