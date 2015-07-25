package com.s24.search.solr.analysis.jdbc;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource;

import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.BeanUtilsBean2;
import org.apache.commons.lang.WordUtils;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.SortField;
import org.apache.lucene.uninverting.UninvertingReader.Type;
import org.apache.solr.response.TextResponseWriter;
import org.apache.solr.schema.FieldType;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.SchemaField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dummy field type which just creates a data source.
 */
@SuppressWarnings("unused") // API
public class JdbcDataSourceFactory extends FieldType {
   /**
    * Logger.
    */
   private static final Logger log = LoggerFactory.getLogger(JdbcDataSourceFactory.class);

   /**
    * Prefix for all database connection pool related properties.
    */
   private  static final String POOL = "pool";

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

   @Override
   protected void setArgs(IndexSchema schema, Map<String, String> args) {
      String name = args.remove(JdbcReaderFactoryParams.DATASOURCE.toString());
      log.info("Registering data source {} for schema {}.", name, schema.getSchemaName());
      // Ignore errors regarding the database connection pool?
      boolean ignore = !"false".equals(args.remove(JdbcReaderFactoryParams.IGNORE.toString()));

      // Blocks other threads that are trying to create a data source with the same name.
      DataSource dataSource =
            dataSources.computeIfAbsent(name, poolName -> createDataSource(args, ignore));

      if (dataSource != null) {
         log.info("Successfully registered data source {} for schema {}.", name, schema.getSchemaName());
      }
   }

   /**
    * Create database connection pool.
    *
    * @param args Configuration.
    * @param ignore Ignore any errors?.
    */
   private DataSource createDataSource(Map<String, String> args, boolean ignore) {
      String poolClassName = args.remove("poolClassName");
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

      for (Iterator<Entry<String, String>> iter = args.entrySet().iterator(); iter.hasNext(); ) {
         Entry<String, String> entry = iter.next();

         String propertyName = entry.getKey();
         if (propertyName.startsWith(POOL)) {
            propertyName = WordUtils.uncapitalize(propertyName.substring(POOL.length()));
            String propertyValue = entry.getValue();
            iter.remove();

            try {
               utils.setProperty(pool, propertyName, propertyValue);

            } catch (InvocationTargetException | IllegalAccessException e) {
               log.error("Failed to configure database connection pool {}#{}: {}.",
                     poolClassName, propertyName, e.getMessage());
               if (ignore) {
                  return null;
               }
               throw new IllegalArgumentException("Failed to configure database connection pool.", e);
            }
         }
      }

      return pool;
   }

   //
   // Unused field type interface
   //

   @Override
   public Type getUninversionType(SchemaField sf) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + " just defines a data source");
   }

   @Override
   public void write(TextResponseWriter writer, String name, IndexableField f) throws IOException {
      throw new UnsupportedOperationException(getClass().getSimpleName() + " just defines a data source");
   }

   @Override
   public SortField getSortField(SchemaField field, boolean top) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + " just defines a data source");
   }
}
