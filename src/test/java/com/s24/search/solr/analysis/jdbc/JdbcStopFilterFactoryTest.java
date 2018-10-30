package com.s24.search.solr.analysis.jdbc;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.log4j.BasicConfigurator;
import org.apache.lucene.analysis.MockTokenizer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.AbstractAnalysisFactory;
import org.apache.lucene.analysis.util.ClasspathResourceLoader;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.lucene.util.TestRuleLimitSysouts.Limit;
import org.apache.lucene.util.Version;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.mock.jndi.SimpleNamingContextBuilder;

/**
 * Test for {@link JdbcSynonymFilterFactory}.
 */
@Limit(bytes = 16384)
public class JdbcStopFilterFactoryTest extends LuceneTestCase {
   /**
    * Embedded database. Implements {@link DataSource}.
    */
   private EmbeddedDatabase database;

   @Before
   public void setUpDatabase() throws Exception {
      BasicConfigurator.resetConfiguration();
      BasicConfigurator.configure();

      // Create H2 database instance
      database = new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2).build();

      // Add synonym table with some content
      JdbcTemplate template = new JdbcTemplate(database);
      template.execute("create table stopwords(stopword varchar(255))");
      template.execute("insert into stopwords values ('somestring')");
      template.execute("insert into stopwords values ('anotherstring')");

      // Register data source with JNDI
      SimpleNamingContextBuilder builder = SimpleNamingContextBuilder.emptyActivatedContextBuilder();
      builder.bind("java:comp/env/dataSource", database);
   }

   /**
    * Test for {@link JdbcSynonymFilterFactory#create(TokenStream)}.
    */
   @Test
   public void create() throws Exception {
      Map<String, String> args = new HashMap<>();
      args.put(AbstractAnalysisFactory.LUCENE_MATCH_VERSION_PARAM, Version.LUCENE_5_0_0.toString());
      args.put(JdbcReaderFactoryParams.DATASOURCE, "java:comp/env/dataSource");
      args.put(JdbcReaderFactoryParams.SQL, "select stopword from stopwords");

      // White space tokenizer, to lower case tokenizer.
      MockTokenizer tokenizer = new MockTokenizer();
      tokenizer.setReader(new StringReader("test1 somestring test2 anotherstring"));

      JdbcStopFilterFactory factory = new JdbcStopFilterFactory(args);
      factory.inform(new ClasspathResourceLoader());

      try (TokenStream stream = factory.create(tokenizer)) {
         CharTermAttribute attribute = stream.addAttribute(CharTermAttribute.class);
         stream.reset();
         assertTrue(stream.incrementToken());
         assertEquals("test1", attribute.toString());
         assertTrue(stream.incrementToken());
         assertEquals("test2", attribute.toString());
         assertFalse(stream.incrementToken());
         stream.end();
      }
   }

   @After
   public void tearDownDatabase() throws Exception {
      database.shutdown();
   }
}
