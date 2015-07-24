package com.s24.search.solr.analysis.jdbc;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource;

import org.apache.commons.beanutils.PropertyUtilsBean;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.AbstractSolrEventListener;
import org.apache.solr.core.SolrCore;

/**
 * Dummy event listener which just creates a data source upon {@link #init(NamedList)}.
 */
@SuppressWarnings("unused") // API
public class JdbcDataSourceFactory extends AbstractSolrEventListener {
   /**
    * All data sources by name.
    */
   private static final Map<String, DataSource> datasources = new ConcurrentHashMap<>();

   /**
    * Constructor.
    *
    * @param core Core.
    */
   public JdbcDataSourceFactory(SolrCore core) {
      super(core);
   }

   /**
    * Look up database connection pool by name.
    *
    * @param name Name of database connection pool.
    */
   public static DataSource lookUp(String name) {
      return datasources.get(name);
   }

   @Override
   public void init(NamedList args) {
      SolrParams params = SolrParams.toSolrParams(args);

      String name = params.get("name");
      String poolClassName = params.get("class");
      NamedList<?> poolParams = (NamedList<?>) args.get("params");
      // Ignore errors regarding the database connection pool?
      boolean ignore = params.getBool("ignore", true);
      datasources.computeIfAbsent(name, poolName -> createDataSource(poolClassName, poolParams, ignore));
   }

   /**
    * Create database connection pool.
    *
    * @param poolClassName Name of database connection pool class.
    * @param poolParams Properties for database connection pool.
    * @param ignore Ignore any errors?.
    */
   private DataSource createDataSource(String poolClassName, NamedList<?> poolParams, boolean ignore) {
      Class<? extends DataSource> poolClass;
      DataSource pool;
      try {
         //noinspection unchecked
         poolClass = (Class<? extends DataSource>) Class.forName(poolClassName);
         pool = poolClass.newInstance();

      } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | ClassCastException e) {
         log.error("Failed to instantiate database connection pool {}: {}.",
               poolClassName, e.getMessage());
         if (ignore) {
            return null;
         }
         throw new IllegalArgumentException("Failed to instantiate database connection pool.");
      }

      for (Entry<String, ?> entry : poolParams) {
         try {
            PropertyUtilsBean utils = new PropertyUtilsBean();
            utils.setProperty(pool, entry.getKey(), entry.getValue());

         } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            log.error("Failed to configure database connection pool {}#{}: {}.",
                  poolClassName, entry.getKey(), e.getMessage());
            if (ignore) {
               return null;
            }
            throw new IllegalArgumentException("Failed to configure database connection pool.");
         }
      }

      return pool;
   }
}
