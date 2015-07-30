package com.s24.search.solr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.PluginInfo;
import org.apache.solr.schema.IndexSchema;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Test for {@link ConfiguringHttpShardHandlerFactory}.
 */
@RunWith(MockitoJUnitRunner.class)
public class ConfiguringHttpShardHandlerFactoryTest {
   @Mock
   private IndexSchema indexSchema;

   @Before
   @After
   public void cleanUp() {
      ConfiguringHttpShardHandlerFactory.clear();
   }

   /**
    * Test for {@link ConfiguringHttpShardHandlerFactory#init(PluginInfo)}.
    */
   @Test
   public void init() {
      NamedList<Object> poolConfig = new NamedList<>();
      poolConfig.add("class", JdbcDataSource.class.getName());
      poolConfig.add("url", "url");
      poolConfig.add("user", "user");
      poolConfig.add("password", "password");
      poolConfig.add("loginTimeout", 100);

      Map<String, Object> shardHandlerConfig = new HashMap<>();
      NamedList<Object> beans = new NamedList<>();
      beans.add("dataSource", poolConfig);
      shardHandlerConfig.put("beans", beans);
      PluginInfo info = new PluginInfo("shardHandler", shardHandlerConfig);
      ConfiguringHttpShardHandlerFactory factory = new ConfiguringHttpShardHandlerFactory();
      factory.init(info);

      DataSource dataSource = (DataSource) ConfiguringHttpShardHandlerFactory.lookUp("dataSource");
      assertTrue(dataSource instanceof JdbcDataSource);
      JdbcDataSource jdbcDataSource = (JdbcDataSource) dataSource;
      assertEquals("url", jdbcDataSource.getUrl());
      assertEquals("user", jdbcDataSource.getUser());
      assertEquals("password", jdbcDataSource.getPassword());
      assertEquals(100, jdbcDataSource.getLoginTimeout());
   }
}