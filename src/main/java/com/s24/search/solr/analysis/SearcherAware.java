package com.s24.search.solr.analysis;

import java.io.IOException;

import org.apache.solr.search.SolrIndexSearcher;

/**
 * Interface for event notification, when a new searcher has been created.
 */
public interface SearcherAware {
   /**
    * Notification that a new searcher has been created.
    * 
    * @param searcher
    *           The new searcher.
    * @throws IOException
    *            In case of any problems.
    */
   void inform(SolrIndexSearcher searcher) throws IOException;
}
