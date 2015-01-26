package com.s24.search.solr.analysis.jdbc;

import static org.junit.Assert.assertEquals;

import java.io.Reader;
import java.io.StringWriter;

import javax.sql.DataSource;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.mock.jndi.SimpleNamingContextBuilder;

/**
 * Test for {@link JndiJdbcReader}.
 */
public class JndiJdbcReaderTest {
   /**
    * Embedded database.
    * Implements {@link DataSource}.
    */
   private EmbeddedDatabase database;

   @Before
   public void setUp() throws Exception {
      // Create H2 database instance
      database = new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2).build();

      // Add synonym table with some content
      JdbcTemplate template = new JdbcTemplate(database);
      template.execute("create table synonyms(synonyms varchar(256))");
      template.execute("insert into synonyms(synonyms) values('test1=>testA,testB')");
      template.execute("insert into synonyms(synonyms) values('test2=>testC,testD')");

      // Register data source with JNDI
      SimpleNamingContextBuilder builder = SimpleNamingContextBuilder.emptyActivatedContextBuilder();
      builder.bind("java:comp/env/dataSource", database);
   }

   /**
    * Test for {@link JndiJdbcReader#getReader()}.
    */
   @Test
   public void getReader() throws Exception {
      Reader reader = new JndiJdbcReader("dataSource", "select synonyms from synonyms", false).getReader();
      StringWriter synonyms = new StringWriter();
      IOUtils.copy(reader, synonyms);

      assertEquals("test1=>testA,testB\ntest2=>testC,testD", synonyms.toString());
   }

   /**
    * Test for {@link JndiJdbcReader#JndiJdbcReader(String, String, boolean)}.
    */
   @Test
   public void ignore() {
      // Missing data source will be ignored
      new JndiJdbcReader("dataSource", "sql", true);
   }

   @After
   public void tearDown() throws Exception {
      database.shutdown();
   }
}
