package com.s24.search.solr.analysis.jdbc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.BasicConfigurator;
import org.apache.solr.common.util.NamedList;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.mock.jndi.SimpleNamingContextBuilder;

/**
 * Test for {@link JdbcReaderFactory}.
 */
public class JdbcReaderFactoryTest {
   @Before
   public void setUp() throws Exception {
      BasicConfigurator.resetConfiguration();
      BasicConfigurator.configure();
   }

   @Test
   public void createFromSolrParams_solr() {
      // Configure data source via JdbcDataSourceFactory.
      NamedList<Object> params = new NamedList<>();
      params.add("name", "dataSource");
      params.add("class", JdbcDataSource.class.getName());
      NamedList<Object> poolParams = new NamedList<>();
      params.add("params", poolParams);
      poolParams.add("url", "jdbc:h2:mem:testdb");
      poolParams.add("user", "sa");
      poolParams.add("password", "");
      new JdbcDataSourceFactory().init(params);

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