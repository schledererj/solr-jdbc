package com.s24.search.solr.analysis;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceTokenizerFactory;
import org.apache.lucene.analysis.util.TokenFilterFactory;
import org.apache.solr.analysis.TokenizerChain;
import org.apache.solr.schema.FieldType;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.search.SolrIndexSearcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.Maps;

/**
 * Test for {@link SearcherAwareReloader}.
 */
@RunWith(MockitoJUnitRunner.class)
public class SearcherAwareReloaderTest {
   @Mock
   private SolrIndexSearcher searcher;
   
   @Mock
   private SolrIndexSearcher currentSearcher;
   
   @Mock
   private IndexSchema schema;
   
   @Mock
   private FieldType fieldType;

   @Mock
   private Factory indexTokenFilterFactory;

   private TokenizerChain indexAnalyzer;

   @Mock
   private Factory queryTokenFilterFactory;

   private TokenizerChain queryAnalyzer;

   @Mock
   private Analyzer simpleAnalyzer;

   /**
    * Reloader under test.
    */
   private SearcherAwareReloader reloader;

   @Before
   public void setUp() {
      Map<String, FieldType> fieldTypes = Maps.newHashMap();
      fieldTypes.put("test", fieldType);
      
      when(searcher.getSchema()).thenReturn(schema);
      when(schema.getFieldTypes()).thenReturn(fieldTypes);

      indexAnalyzer = new TokenizerChain(
            new WhitespaceTokenizerFactory(Maps.<String, String>newHashMap()), 
            new TokenFilterFactory[]{ indexTokenFilterFactory });
      queryAnalyzer = new TokenizerChain(
            new WhitespaceTokenizerFactory(Maps.<String, String>newHashMap()),
            new TokenFilterFactory[]{ queryTokenFilterFactory });

      reloader = new SearcherAwareReloader(null);
   }
   
   /**
    * Test for {@link SearcherAwareReloader#newSearcher(SolrIndexSearcher, SolrIndexSearcher)}.
    */
   @Test
   public void newSearcher_differentAnalyzers() throws Exception {
      when(fieldType.getIndexAnalyzer()).thenReturn(indexAnalyzer);
      when(fieldType.getQueryAnalyzer()).thenReturn(queryAnalyzer);

      reloader.newSearcher(searcher, currentSearcher);

      // Inform twice if index and query analyzer not the same.
      verify(indexTokenFilterFactory, times(1)).inform(searcher);
      verify(queryTokenFilterFactory, times(1)).inform(searcher);
   }

   /**
    * Test for {@link SearcherAwareReloader#newSearcher(SolrIndexSearcher, SolrIndexSearcher)}.
    */
   @Test
   public void newSearcher_sameAnalyzer() throws Exception {
      when(fieldType.getIndexAnalyzer()).thenReturn(indexAnalyzer);
      when(fieldType.getQueryAnalyzer()).thenReturn(indexAnalyzer);

      reloader.newSearcher(searcher, currentSearcher);

      // Do not inform twice if index and query analyzer are the same.
      verify(indexTokenFilterFactory, times(1)).inform(searcher);
      verify(queryTokenFilterFactory, never()).inform(searcher);
   }

   /**
    * Test for {@link SearcherAwareReloader#newSearcher(SolrIndexSearcher, SolrIndexSearcher)}.
    */
   @Test
   public void newSearcher_simpleAnalyzer() throws Exception {
      when(fieldType.getIndexAnalyzer()).thenReturn(simpleAnalyzer);
      when(fieldType.getQueryAnalyzer()).thenReturn(simpleAnalyzer);

      reloader.newSearcher(searcher, currentSearcher);

      // Only inform if analyzers is instance of TokenizerChain.
      verify(indexTokenFilterFactory, never()).inform(searcher);
      verify(queryTokenFilterFactory, never()).inform(searcher);
   }

   /**
    * Dummy {@link SearcherAware} token filter factory for testing.
    */
   private static abstract class Factory extends TokenFilterFactory implements SearcherAware {
      public Factory(Map<String, String> args) {
         super(args);
      }
   }
}
