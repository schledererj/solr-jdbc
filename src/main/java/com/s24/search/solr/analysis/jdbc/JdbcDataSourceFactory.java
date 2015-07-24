package com.s24.search.solr.analysis.jdbc;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource;

import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.BeanUtilsBean2;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.transform.DocTransformer;
import org.apache.solr.response.transform.TransformerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dummy event listener which just creates a data source upon {@link #init(NamedList)}.
 */
@SuppressWarnings("unused") // API
public class JdbcDataSourceFactory extends TransformerFactory {
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

   @Override
   public DocTransformer create(String field, SolrParams params, SolrQueryRequest req) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + " just creates a data source.");
   }

   /**
    * Look up database connection pool by name.
    *
    * @param name Name of database connection pool.
    */
   public static DataSource lookUp(String name) {
      return dataSources.get(name);
   }

   @Override
   public void init(NamedList args) {
      log.info("Registering data source {}.", args);

      SolrParams params = SolrParams.toSolrParams(args);
      String name = params.get("name");
      String poolClassName = params.get("class");
      NamedList<?> poolParams = (NamedList<?>) args.get("params");
      // Ignore errors regarding the database connection pool?
      boolean ignore = params.getBool("ignore", true);

      // Blocks other threads that are trying to create a data source with the same name.
      DataSource dataSource =
            dataSources.computeIfAbsent(name, poolName -> createDataSource(poolClassName, poolParams, ignore));

      log.info("Registered data source {}.", dataSource);
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
         throw new IllegalArgumentException("Failed to instantiate database connection pool.", e);
      }

      for (Entry<String, ?> entry : poolParams) {
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
