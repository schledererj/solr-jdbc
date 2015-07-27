package com.s24.search.solr.analysis.jdbc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.sql.DataSource;

import org.apache.solr.common.util.NamedList;
import org.apache.solr.schema.IndexSchema;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Test for {@link JdbcDataSourceFactory}.
 */
@RunWith(MockitoJUnitRunner.class)
public class JdbcDataSourceFactoryTest {
   @Mock
   private IndexSchema indexSchema;

   @Before
   @After
   public void cleanUp() {
      JdbcDataSourceFactory.clear();
   }

   /**
    * Test for {@link JdbcDataSourceFactory#init(NamedList)}.
    */
   @Test
   public void init() {
      JdbcDataSourceFactory factory = new JdbcDataSourceFactory(null);

      NamedList<Object> poolDefinition = new NamedList<>();
      poolDefinition.add("dataSource", "dataSource");
      poolDefinition.add("class", JdbcDataSource.class.getName());
      NamedList<Object> poolConfig = new NamedList<>();
      poolConfig.add("url", "url");
      poolConfig.add("user", "user");
      poolConfig.add("password", "password");
      poolConfig.add("loginTimeout", 100);
      poolDefinition.add("params", poolConfig);
      factory.init(poolDefinition);

      DataSource dataSource = JdbcDataSourceFactory.lookUp("dataSource");
      assertTrue(dataSource instanceof JdbcDataSource);
      JdbcDataSource jdbcDataSource = (JdbcDataSource) dataSource;
      assertEquals("url", jdbcDataSource.getUrl());
      assertEquals("user", jdbcDataSource.getUser());
      assertEquals("password", jdbcDataSource.getPassword());
      assertEquals(100, jdbcDataSource.getLoginTimeout());
   }
}