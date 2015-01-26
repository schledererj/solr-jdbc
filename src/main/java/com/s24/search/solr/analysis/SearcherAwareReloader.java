package com.s24.search.solr.analysis;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.util.Map.Entry;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.util.TokenFilterFactory;
import org.apache.solr.analysis.TokenizerChain;
import org.apache.solr.core.AbstractSolrEventListener;
import org.apache.solr.core.SolrCore;
import org.apache.solr.schema.FieldType;
import org.apache.solr.search.SolrIndexSearcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Notifies all {@link SearcherAware}s about new searchers.
 */
public class SearcherAwareReloader extends AbstractSolrEventListener {
   /**
    * Logger.
    */
   private static final Logger logger = LoggerFactory.getLogger(SearcherAwareReloader.class);

   /**
    * Constructor.
    * 
    * @param core
    *           Solr core.
    */
   public SearcherAwareReloader(SolrCore core) {
      super(core);
   }

   @Override
   public void newSearcher(SolrIndexSearcher searcher, SolrIndexSearcher currentSearcher) {
      checkNotNull(searcher, "Pre-condition violated: searcher must not be null.");

      logger.info("Informing searcher awares.");

      for (Entry<String, FieldType> entry : searcher.getSchema().getFieldTypes().entrySet()) {
         String name = entry.getKey();
         FieldType fieldType = entry.getValue();
         
         inform("field type", name, fieldType, searcher);

         Analyzer indexAnalyzer = fieldType.getQueryAnalyzer();
         Analyzer queryAnalyzer = fieldType.getQueryAnalyzer();
         if (indexAnalyzer instanceof TokenizerChain) {
            inform(name, (TokenizerChain) indexAnalyzer, searcher);
         }
         if (indexAnalyzer != queryAnalyzer && queryAnalyzer instanceof TokenizerChain) {
            inform(name, (TokenizerChain) queryAnalyzer, searcher);
         }
      }

      logger.info("All searcher awares have been informed.");
   }

   /**
    * Inform {@link SearcherAware} about a new searcher.
    * 
    * @param type
    *           Description of concrete type of the {@link SearcherAware}.
    * @param name
    *           Name of the field type.
    * @param o
    *           The {@link SearcherAware}.
    * @param searcher
    *           The new searcher.
    */
   private void inform(String type, String name, Object o, SolrIndexSearcher searcher) {
      if (o instanceof SearcherAware) {
         logger.info("Informing searcher aware {} ({}) of field type {} about a new searcher.", 
               type, o.getClass().getName(), name);
         try {
            ((SearcherAware) o).inform(searcher);
         } catch (IOException e) {
            logger.error("Failed to inform {} ({}) of field type {} about a new searcher.", 
                  type, o.getClass().getName(), name, e);
            throw new IllegalArgumentException("Failed to inform about a new searcher.", e);
         }
      }
   }
   
   /**
    * Inform {@link SearcherAware} filter factories in a {@link TokenizerChain} about a new searcher.
    * 
    * @param name
    *           Name of the field type.
    * @param tokenizers
    *           The {@link TokenizerChain}.
    * @param searcher
    *           The new searcher.
    */
   private void inform(String name, TokenizerChain tokenizers, SolrIndexSearcher searcher) {
      for (TokenFilterFactory factory : tokenizers.getTokenFilterFactories()) {
         inform("token filter factory", name, factory, searcher);
      }
   }
}
