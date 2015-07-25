package com.s24.search.solr.analysis.jdbc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.solr.schema.IndexSchema;
import org.h2.jdbcx.JdbcDataSource;
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

   /**
    * Test for {@link JdbcDataSourceFactory#init(IndexSchema, Map)}.
    */
   @Test
   public void init() {
      JdbcDataSourceFactory factory = new JdbcDataSourceFactory();

      Map<String, String> poolArgs = new HashMap<>();
      poolArgs.put("dataSource", "test");
      poolArgs.put("poolClassName", JdbcDataSource.class.getName());
      poolArgs.put("poolUrl", "url");
      poolArgs.put("poolUser", "user");
      poolArgs.put("poolPassword", "password");
      poolArgs.put("poolLoginTimeout", "100");
      factory.setArgs(indexSchema, poolArgs);

      DataSource dataSource = JdbcDataSourceFactory.lookUp("test");
      assertTrue(dataSource instanceof JdbcDataSource);
      JdbcDataSource jdbcDataSource = (JdbcDataSource) dataSource;
      assertEquals("url", jdbcDataSource.getUrl());
      assertEquals("user", jdbcDataSource.getUser());
      assertEquals("password", jdbcDataSource.getPassword());
      assertEquals(100, jdbcDataSource.getLoginTimeout());
   }
}