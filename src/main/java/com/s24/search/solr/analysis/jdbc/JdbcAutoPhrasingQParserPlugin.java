package com.s24.search.solr.analysis.jdbc;

import com.lucidworks.analysis.AutoPhrasingParameters;
import com.lucidworks.analysis.AutoPhrasingTokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.lucene.analysis.util.ResourceLoaderAware;
import org.apache.lucene.analysis.util.WordlistLoader;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.QParser;
import org.apache.solr.search.QParserPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;


public class JdbcAutoPhrasingQParserPlugin extends QParserPlugin implements ResourceLoaderAware {

    private static final Logger Log = LoggerFactory.getLogger(JdbcAutoPhrasingQParserPlugin.class);
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private CharArraySet phraseSets;

    private AutoPhrasingParameters autoPhrasingParameters;
    private JdbcReader reader;


    @Override
    public void init(NamedList initArgs) {
        Log.debug("init JdbcAutoPhrasingQParserPlugin...");
        SolrParams solrParams = SolrParams.toSolrParams(initArgs);
        autoPhrasingParameters = new AutoPhrasingParameters(solrParams);

        reader = JdbcReaderFactory.createFromSolrParams(SolrParams.toMap(initArgs), null);
    }

    @Override
    public QParser createParser(String qStr, SolrParams localParams, SolrParams params,
                                SolrQueryRequest req) {
        Log.debug("createParser JdbcAutoPhrasingQParserPlugin...");
        ModifiableSolrParams modifiableSolrParams = new ModifiableSolrParams(params);

        String modQ = qStr;
        if (qStr != null) {
            modQ = filter(qStr);
            modifiableSolrParams.set("q", modQ);
        }
        return req.getCore().getQueryPlugin(autoPhrasingParameters.getDownstreamParser())
                .createParser(modQ, localParams, modifiableSolrParams, req);
    }

    private String filter(String qStr) {

        String query = qStr;

        // filter : for field names
        while (query.contains(" :"))
            query = query.replaceAll("\\s:", ": ");

        // mandatory and optional clauses
        query = query.replaceAll("\\+", "+ ");
        query = query.replaceAll("\\-", "- ");

        // logical operators
        if (autoPhrasingParameters.getIgnoreCase()) {
            query = query.replaceAll(" AND ", " && ");
            query = query.replaceAll(" OR ", " || ");
            query = query.replaceAll(" TO ", " `to` ");
            query = query.replaceAll("NOW", "`now`");
        }

        // grouping with parenthesis
        query = query.replaceAll("\\(", "( ");
        query = query.replaceAll("\\)", " )");

        query = String.format(" %s ", query);

        // unit of measure queries
        query = query.replaceAll("(?i)(\\d+)\\s?(pound[s]?|lb[s]?)\\b", "$1lb");
        query = query.replaceAll("(?i)(\\d+)\\s?(inch(es)?)", "$1in ");
        query = query.replaceAll("(?i)\\b(\\d+)\\s?(inche?s?|i+n?|[\"]+)\\s?(w|l|h|d)?(x+|\\s+|\\b+|\\s?x+|$)", "$1in ");
        query = query.replaceAll("(?i)(\\d+)\\s?(ounce[s]?|oz)\\b", "$1oz");
        query = query.replaceAll("(?i)(\\d+)\\s?(quart[s]?|qt[s]?)\\b", "$1 qt");
        query = query.replaceAll("(?i)(\\d+)\\s?(gallon[s]?|gal?)\\b", "$1gal");
        query = query.replaceAll("(?i)(\\d+)\\s?(yd[s]?|yard[s]?)\\b", "$1yd");
        query = query.replaceAll("(?i)(\\d+)\\s?(liter[s]?|l)\\b", "$1l");
        query = query.replaceAll("(?i)(\\d+)\\s?(mm|cc|ml)\\b", "$1$2");

        // phrases with quotes
        query = query.replaceAll("(^|\\s)\"", " open_quote` ");
        query = query.replaceAll("\"(\\s|$)", " close_quote` ");

        // autophrase the query
        try {
            query = autophrase(query);
        } catch (IOException ioe) {
            Log.error(ioe.toString());
        }

        // restore mandatory and optional
        query = query.replaceAll("\\+ ", "+");
        query = query.replaceAll("\\- ", "-");

        // restore logical operators
        if (autoPhrasingParameters.getIgnoreCase()) {
            query = query.replaceAll(" && ", " AND ");
            query = query.replaceAll(" \\|\\| ", " OR ");
            query = query.replaceAll(" `to` ", " TO ");
            query = query.replaceAll("`now`", "NOW");
        }

        // restore grouping with parenthesis
        query = query.replaceAll( "\\( ", "(" );
        query = query.replaceAll( " \\)", ")" );

        // restore quotes
        query = query.replaceAll("open_quote`\\s", "\"");
        query = query.replaceAll("\\sclose_quote`", "\"");

        return query;
    }

    private String autophrase(String input) throws IOException {
        WhitespaceTokenizer wt = new WhitespaceTokenizer();
        wt.setReader(new StringReader(input));
        TokenStream ts = wt;
        if (autoPhrasingParameters.getIgnoreCase()) {
            ts = new LowerCaseFilter(wt);
        }
        AutoPhrasingTokenFilter autoPhrasingTokenFilter =
                new AutoPhrasingTokenFilter(ts, phraseSets);
        autoPhrasingTokenFilter.setReplaceWhitespaceWith(autoPhrasingParameters.getReplaceWhitespaceWith());
        CharTermAttribute term = autoPhrasingTokenFilter.addAttribute(CharTermAttribute.class);
        autoPhrasingTokenFilter.reset();

        StringBuilder stringBuilder = new StringBuilder();
        while (autoPhrasingTokenFilter.incrementToken()) {
            stringBuilder.append(term.toString()).append(" ");
        }

        return stringBuilder.toString().trim();
    }

    @Override
    public void inform(ResourceLoader loader) throws IOException {
        JdbcResourceLoader jdbcLoader = new JdbcResourceLoader(loader, reader, UTF8);
        phraseSets = getWordSet(jdbcLoader, true);
    }

    private CharArraySet getWordSet(ResourceLoader loader, boolean ignoreCase)
            throws IOException {

        CharArraySet words = new CharArraySet(500, ignoreCase);
        List<String> stopWords = getLines(loader);
        words.addAll(StopFilter.makeStopSet(stopWords, ignoreCase));
        return words;
    }

    private List<String> getLines(ResourceLoader loader) throws IOException {
        return WordlistLoader.getLines(loader.openResource("database"), StandardCharsets.UTF_8);
    }
}
