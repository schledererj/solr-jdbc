package com.s24.search.solr.analysis.jdbc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Map;

import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.PluginInfo;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.mock.jndi.SimpleNamingContextBuilder;

import com.s24.search.solr.ConfiguringHttpShardHandlerFactory;

/**
 * Test for {@link JdbcReaderFactory}.
 */
@RunWith(MockitoJUnitRunner.class)
public class JdbcReaderFactoryTest {
   @Before
   @After
   public void cleanUp() {
      ConfiguringHttpShardHandlerFactory.clear();
   }

   /**
    * Test for {@link JdbcReaderFactory#createFromSolrParams(Map, String)}.
    */
   @Test
   public void createFromSolrParams_solr() {
      // Configure data source via JdbcDataSourceFactory.
      NamedList<Object> poolConfig = new NamedList<>();
      poolConfig.add("class", JdbcDataSource.class.getName());
      poolConfig.add("url", "jdbc:h2:mem:testdb");
      poolConfig.add("user", "sa");
      poolConfig.add("password", "");

      Map<String, Object> shardHandlerConfig = new HashMap<>();
      NamedList<Object> pools = new NamedList<>();
      pools.add("dataSource", poolConfig);
      shardHandlerConfig.put("pools", pools);
      PluginInfo info = new PluginInfo("shardHandler", shardHandlerConfig);
      ConfiguringHttpShardHandlerFactory factory = new ConfiguringHttpShardHandlerFactory();
      factory.init(info);

      // Configure JdbcReaderFactory.
      Map<String, String> readerDefinition = new HashMap<>();
      readerDefinition.put(JdbcReaderFactoryParams.DATASOURCE, "dataSource");
      readerDefinition.put(JdbcReaderFactoryParams.SQL, "sql");
      readerDefinition.put(JdbcReaderFactoryParams.IGNORE, "false");

      JdbcReader reader = JdbcReaderFactory.createFromSolrParams(readerDefinition, "dummy");
      assertNotNull(reader);
      assertEquals("sql", reader.getSql());
   }

   /**
    * Test for {@link JdbcReaderFactory#createFromSolrParams(Map, String)}.
    */
   @Test
   public void createReader_jndi() throws Exception {
      // Register data source with JNDI.
      SimpleNamingContextBuilder builder = SimpleNamingContextBuilder.emptyActivatedContextBuilder();
      builder.bind("java:comp/env/dataSource", new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2).build());

      // Configure JdbcReaderFactory.
      Map<String, String> readerDefinition = new HashMap<>();
      readerDefinition.put(JdbcReaderFactoryParams.DATASOURCE, "dataSource");
      readerDefinition.put(JdbcReaderFactoryParams.SQL, "sql");
      readerDefinition.put(JdbcReaderFactoryParams.IGNORE, "false");

      JdbcReader reader = JdbcReaderFactory.createFromSolrParams(readerDefinition, "dummy");
      assertNotNull(reader);
      assertEquals("sql", reader.getSql());
   }
}