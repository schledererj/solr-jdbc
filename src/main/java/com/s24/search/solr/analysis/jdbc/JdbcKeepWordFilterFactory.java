package com.s24.search.solr.analysis.jdbc;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.apache.lucene.analysis.miscellaneous.KeepWordFilterFactory;
import org.apache.lucene.analysis.util.ResourceLoader;

/**
 * A jdbc based keep word filter
 * 
 * @author Shopping24 GmbH, Torsten Bøgh Köster (@tboeghk)
 */
public class JdbcKeepWordFilterFactory extends KeepWordFilterFactory {

   /**
    * Database based reader.
    */
   private final JdbcReader reader;

   public JdbcKeepWordFilterFactory(Map<String, String> args) {
      this(args, JdbcReaderFactory.createFromSolrParams(args, "words"));
   }

   /**
    * Constructor
    */
   public JdbcKeepWordFilterFactory(Map<String, String> args, JdbcReader reader) {
      super(args);

      this.reader = reader;
   }

   /**
    * Load words from jdbc
    */
   @Override
   public void inform(ResourceLoader loader) throws IOException {
      super.inform(new JdbcResourceLoader(loader, reader, StandardCharsets.UTF_8));
   }

}
