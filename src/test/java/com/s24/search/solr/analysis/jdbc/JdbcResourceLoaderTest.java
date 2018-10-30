package com.s24.search.solr.analysis.jdbc;

import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.Charset;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.io.IOUtils;
import org.apache.lucene.analysis.util.ClasspathResourceLoader;
import org.junit.Test;

/**
 * Test for {@link JdbcResourceLoader}.
 */
public class JdbcResourceLoaderTest {
   @Test
   public void openResource() throws Exception {
      ClasspathResourceLoader parent = new ClasspathResourceLoader();
      JdbcReader reader = new TestJdbcReader("test=>test1,test2");
      Charset charset = Charset.forName("UTF-8");

      JdbcResourceLoader loader = new JdbcResourceLoader(parent, reader, charset);

      InputStream resource = loader.openResource(JdbcResourceLoader.DATABASE);
      StringWriter writer = new StringWriter();
      IOUtils.copy(resource, writer, charset);

      assertEquals("test=>test1,test2", writer.toString());
   }

   /**
    * {@link JdbcReader} returning a fixed synonym database.
    */
   private static class TestJdbcReader implements JdbcReader {
      /**
       * Synonym database.
       */
      private final String synonyms;

      /**
       * Constructor.
       *
       * @param synonyms
       *           Synonym database.
       */
      public TestJdbcReader(String synonyms) {
         this.synonyms = synonyms;
      }

      @Override
      public Reader getReader() {
         return new StringReader(synonyms);
      }

      @Override
      public QueryRunner getJdbcRunner() {
         return null;
      }

      @Override
      public String getSql() {
         return "SELECT * FROM test;";
      }
   }
}
