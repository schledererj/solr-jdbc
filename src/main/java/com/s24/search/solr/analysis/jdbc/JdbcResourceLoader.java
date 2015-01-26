package com.s24.search.solr.analysis.jdbc;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import org.apache.commons.io.input.ReaderInputStream;
import org.apache.lucene.analysis.util.ResourceLoader;

/**
 * {@link ResourceLoader} which loads a resource from a {@link JdbcReader}.
 * All other operations are delegated to the parent resource loader.
 */
class JdbcResourceLoader implements ResourceLoader {
   /**
    * Name of database resource.
    */
   static final String DATABASE = "database";

   /**
    * {@link ResourceLoader} to delegate class loading to.
    */
   private ResourceLoader parent;

   /**
    * Database based reader.
    */
   private final JdbcReader reader;

   /**
    * Encoding of database resource.
    */
   private final Charset charset;

   /**
    * Constructor.
    *
    * @param parent
    *           Parent resource loader.
    * @param reader
    *           Database based reader.
    * @param charset
    *           {@link Charset} to encode database resource with, because resources are always input streams.
    */
   public JdbcResourceLoader(ResourceLoader parent, JdbcReader reader, Charset charset) {
      this.parent = checkNotNull(parent);
      this.reader = checkNotNull(reader);
      this.charset = checkNotNull(charset);
   }

   @Override
   public InputStream openResource(String resource) throws IOException {
      if (DATABASE.equals(resource)) {
         return new ReaderInputStream(reader.getReader(), charset);
      }

      return parent.openResource(resource);
   }

   @Override
   public <T> Class<? extends T> findClass(String cname, Class<T> expectedType) {
      return parent.findClass(cname, expectedType);
   }

   @Override
   public <T> T newInstance(String cname, Class<T> expectedType) {
      return parent.newInstance(cname, expectedType);
   }
}
