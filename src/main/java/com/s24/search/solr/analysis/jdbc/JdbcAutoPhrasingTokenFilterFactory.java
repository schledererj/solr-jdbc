package com.s24.search.solr.analysis.jdbc;

import com.lucidworks.analysis.*;
import com.s24.search.solr.analysis.SearcherAware;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.solr.search.SolrIndexSearcher;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;

/**
 * Factory for a {@link AutoPhrasingTokenFilter} which loads autophrases from a database.
 *
 * @author John Schlederer
 */
public class JdbcAutoPhrasingTokenFilterFactory extends AutoPhrasingTokenFilterFactory implements SearcherAware{

    /**
     * {@link Charset} to encode synonym database with. Has to be the same as in
     * the {@link AutoPhrasingTokenFilterFactory}.
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
     * @param reader
     *           Reader for autophrases.
     */
    JdbcAutoPhrasingTokenFilterFactory(Map<String, String> args, JdbcReader reader) {
        super(args);

        this.reader = reader;
    }

    /**
     * Constructor.
     *
     * @param args
     *           Configuration.
     */
    public JdbcAutoPhrasingTokenFilterFactory(Map<String, String> args) {
        this(args, JdbcReaderFactory.createFromSolrParams(args, "phrases"));
    }

    @Override
    public void inform(ResourceLoader loader) throws IOException {
        super.inform(new JdbcResourceLoader(loader, reader, UTF8));
    }

    @Override
    public void inform(SolrIndexSearcher searcher) {
        try {
            inform(searcher.getCore().getResourceLoader());
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to notify about new searcher.", e);
        }
    }
}
