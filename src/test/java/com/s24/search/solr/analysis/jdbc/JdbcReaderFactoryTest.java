package com.s24.search.solr.analysis.jdbc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.BasicConfigurator;
import org.apache.solr.schema.IndexSchema;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.mock.jndi.SimpleNamingContextBuilder;

/**
 * Test for {@link JdbcReaderFactory}.
 */
@RunWith(MockitoJUnitRunner.class)
public class JdbcReaderFactoryTest {
   @Mock
   private IndexSchema indexSchema;

   @Before
   public void setUp() throws Exception {
      BasicConfigurator.resetConfiguration();
      BasicConfigurator.configure();
   }

   @Test
   public void createFromSolrParams_solr() {
      // Configure data source via JdbcDataSourceFactory.
      Map<String, String> poolArgs = new HashMap<>();
      poolArgs.put("dataSource", "dataSource");
      poolArgs.put("poolClassName", JdbcDataSource.class.getName());
      poolArgs.put("poolUrl", "jdbc:h2:mem:testdb");
      poolArgs.put("poolUser", "sa");
      poolArgs.put("poolPassword", "");
      new JdbcDataSourceFactory().init(indexSchema, poolArgs);

      // Configure JdbcReaderFactory.
      Map<String, String> args = new HashMap<>();
      args.put(JdbcReaderFactoryParams.DATASOURCE.toString(), "dataSource");
      args.put(JdbcReaderFactoryParams.SQL.toString(), "sql");
      args.put(JdbcReaderFactoryParams.IGNORE.toString(), "false");

      JdbcReader reader = JdbcReaderFactory.createFromSolrParams(args, "dummy");
      assertNotNull(reader);
      assertEquals("sql", reader.getSql());
   }

   @Test
   public void createFromSolrParams_jndi() throws Exception {
      // Register data source with JNDI.
      SimpleNamingContextBuilder builder = SimpleNamingContextBuilder.emptyActivatedContextBuilder();
      builder.bind("java:comp/env/dataSource", new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2).build());

      // Configure JdbcReaderFactory.
      Map<String, String> args = new HashMap<>();
      args.put(JdbcReaderFactoryParams.DATASOURCE.toString(), "dataSource");
      args.put(JdbcReaderFactoryParams.SQL.toString(), "sql");
      args.put(JdbcReaderFactoryParams.IGNORE.toString(), "false");

      JdbcReader reader = JdbcReaderFactory.createFromSolrParams(args, "dummy");
      assertNotNull(reader);
      assertEquals("sql", reader.getSql());
   }
}