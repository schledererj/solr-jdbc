package com.s24.search.solr.analysis.jdbc;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.core.StopFilterFactory;
import org.apache.lucene.analysis.miscellaneous.StemmerOverrideFilterFactory;
import org.apache.lucene.analysis.util.ResourceLoader;

import com.google.common.collect.ImmutableList;

/**
 * Factory for {@code StemmerOverrideFilter} which loads the dictionary from a database.
 */
public class JdbcStemmerOverrideFilterFactory extends StemmerOverrideFilterFactory {

   /**
    * {@link Charset} to encode synonym database with. Has to be the same as in the {@link StopFilterFactory}.
    */
   private static final Charset UTF8 = Charset.forName("UTF-8");

   /**
    * Database based reader.
    */
   private final JdbcReader reader;

   /**
    * Creates a new JdbcStemmerOverrideFilterFactory.
    *
    * @param args
    *           Configuration.
    */
   public JdbcStemmerOverrideFilterFactory(Map<String, String> args) {
      super(superclassArgs(args));
      this.reader = JdbcReaderFactory.createFromSolrParams(args, "dictionary");
   }

   /**
    * Returns a map which contains only those arguments that the superclass understands.
    */
   private static Map<String, String> superclassArgs(Map<String, String> args) {
      Map<String, String> result = new HashMap<>();
      for (String arg : ImmutableList.of("dictionary", "ignoreCase")) {
         String value = args.get(arg);
         if (value != null) {
            result.put(arg, value);
         }
      }
      return result;
   }

   @Override
   public void inform(ResourceLoader loader) throws IOException {
      super.inform(new JdbcResourceLoader(loader, reader, UTF8));
   }
}
