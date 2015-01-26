package com.s24.search.solr.analysis;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

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
   
   private TokenizerChain analyzer;
   
   @Mock
   private Factory tokenFilterFactory;
   
   @Before
   public void setUp() {
      Map<String, FieldType> fieldTypes = Maps.newHashMap();
      fieldTypes.put("test", fieldType);
      
      when(searcher.getSchema()).thenReturn(schema);
      when(schema.getFieldTypes()).thenReturn(fieldTypes);

      analyzer = new TokenizerChain(
            new WhitespaceTokenizerFactory(Maps.<String, String>newHashMap()), 
            new TokenFilterFactory[]{ tokenFilterFactory });
      when(fieldType.getIndexAnalyzer()).thenReturn(analyzer);
      when(fieldType.getQueryAnalyzer()).thenReturn(analyzer);
   }
   
   /**
    * Test for {@link SearcherAwareReloader#newSearcher(SolrIndexSearcher, SolrIndexSearcher)}.
    */
   @Test
   public void newSearcher() throws Exception {
      SearcherAwareReloader reloader = new SearcherAwareReloader(null);
      
      reloader.newSearcher(searcher, currentSearcher);
      
      verify(tokenFilterFactory).inform(searcher);
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
