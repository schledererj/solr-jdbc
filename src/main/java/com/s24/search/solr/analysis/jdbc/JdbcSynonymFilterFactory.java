package com.s24.search.solr.analysis.jdbc;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;

import org.apache.lucene.analysis.synonym.SynonymFilter;
import org.apache.lucene.analysis.synonym.SynonymFilterFactory;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.solr.search.SolrIndexSearcher;

import com.s24.search.solr.analysis.SearcherAware;

/**
 * Factory for a {@link SynonymFilter} which loads synonyms from a database.
 *
 * @author Shopping24 GmbH, Torsten Bøgh Köster (@tboeghk)
 */
public class JdbcSynonymFilterFactory extends SynonymFilterFactory implements SearcherAware {

   /**
    * {@link Charset} to encode synonym database with. Has to be the same as in
    * the {@link SynonymFilterFactory}.
    */
   private static final Charset UTF8 = Charset.forName("UTF-8");

   /**
    * Database based reader.
    */
   private final JdbcReader reader;

   /**
    * Constructor.
    *
    * @param args
    *           Configuration.
    */
   public JdbcSynonymFilterFactory(Map<String, String> args) {
      this(args, JdbcReaderFactory.createFromSolrParams(args, "synonyms"));
   }

   /**
    * Constructor.
    *
    * @param args
    *           Configuration.
    * @param reader
    *           Reader for synonyms.
    */
   JdbcSynonymFilterFactory(Map<String, String> args, JdbcReader reader) {
      super(args);

      this.reader = reader;
   }

   @Override
   public void inform(SolrIndexSearcher searcher) {
      try {
         inform(searcher.getCore().getResourceLoader());
      } catch (IOException e) {
         throw new IllegalArgumentException("Failed to notify about new searcher.", e);
      }
   }

   @Override
   public void inform(ResourceLoader loader) throws IOException {
      super.inform(new JdbcResourceLoader(loader, reader, UTF8));
   }
}
