package com.s24.search.solr.analysis.jdbc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.sql.DataSource;

import org.apache.solr.common.util.NamedList;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.Test;

/**
 * Test for {@link JdbcDataSourceFactory}.
 */
public class JdbcDataSourceFactoryTest {
   /**
    * Test for {@link JdbcDataSourceFactory#init(NamedList)}.
    */
   @Test
   public void init() {
      JdbcDataSourceFactory factory = new JdbcDataSourceFactory();

      NamedList<Object> args = new NamedList<>();
      args.add("name", "test");
      args.add("class", JdbcDataSource.class.getName());
      NamedList<Object> poolParams = new NamedList<>();
      args.add("params", poolParams);
      poolParams.add("url", "url");
      poolParams.add("user", "user");
      poolParams.add("password", "password");
      poolParams.add("loginTimeout", 100);
      factory.init(args);

      DataSource dataSource = JdbcDataSourceFactory.lookUp("test");
      assertTrue(dataSource instanceof JdbcDataSource);
      JdbcDataSource jdbcDataSource = (JdbcDataSource) dataSource;
      assertEquals("url", jdbcDataSource.getUrl());
      assertEquals("user", jdbcDataSource.getUser());
      assertEquals("password", jdbcDataSource.getPassword());
      assertEquals(100, jdbcDataSource.getLoginTimeout());
   }
}