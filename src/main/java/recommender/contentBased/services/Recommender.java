package recommender.contentBased.services;

import common.Utils;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import recommender.contentBased.beans.Item;
import settings.HORmessages;
import survey.context.beans.Location;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Define Recommender class
 *
 * @author Roberto B. Stanziale
 * @version 1.0
 */
public class Recommender {

    private StandardAnalyzer analyzer;
    private Directory index;

    /**
     * Constructor of Recommender
     * @param pois list of Items
     */
    public Recommender(List<Item> pois) {
        this.analyzer = new StandardAnalyzer();
        this.index = new RAMDirectory();
        IndexWriterConfig config = new IndexWriterConfig(analyzer);

        IndexWriter w;
        try {
            w  = new IndexWriter(index, config);
            for (Item p : pois) {
                addDoc(w,
                        p.getWebsite(),
                        p.getName(),
                        p.getAddress(),
                        p.getPhone(),
                        p.getTags(),
                        p.getRatingAverage(),
                        p.getLat(),
                        p.getLng());
            }
            w.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Lucene method for search into Index relevant document to recommend
     * @param query is string to representing the query for Lucene index
     * @param location is user location
     * @return a list of relevant item to recommend
     * @throws IOException for Input/Output exception
     * @throws ParseException for search document exception
     */
    public List<Item> searchItems(String query, Location location) throws IOException, ParseException {
        Query q = new QueryParser("tags", analyzer).parse(query);

        int hitsPerPage = 50;
        IndexReader reader = DirectoryReader.open(index);
        IndexSearcher searcher = new IndexSearcher(reader);

        TopDocs docs = searcher.search(q, hitsPerPage);
        ScoreDoc[] hits = docs.scoreDocs;

        Set<Item> items = new TreeSet<>();
        for (ScoreDoc hit : hits) {
            int docId = hit.doc;
            Document d = searcher.doc(docId);

            if (this.isNearbyItem(location.getLatitude(), Float.valueOf(d.get("lat")),
                    location.getLongitude(), Float.valueOf(d.get("lng")))) {
                Item i = new Item(d.get("website"),
                        d.get("name"),
                        d.get("address"),
                        d.get("phone"),
                        d.get("tags"),
                        Double.valueOf(d.get("ratingAverage")),
                        Float.valueOf(d.get("lat")),
                        Float.valueOf(d.get("lng")));
                i.setScore(hit.score);
                items.add(i);
            }
        }
        reader.close();

        return new ArrayList<> (items);
    }

    /**
     * Lucene method for add a document to index representing the item to indexing
     * @param w is the IndexWriter of Lucene
     * @param website representing the website of item
     * @param name representing the name of item
     * @param address representing the address of item
     * @param phone representing the phone of item
     * @param tags representing the TAGS of item
     * @param ratingAverage representing rating average
     * @param lat representing the latitude of item
     * @param lng representing the longitude of item
     * @throws IOException for Input/Output exception
     */
    private static void addDoc(IndexWriter w,
                               String website, String name, String address, String phone, String tags,
                               double ratingAverage, float lat, float lng) throws IOException {
        Document doc = new Document();
        doc.add(new StringField("name", name, Field.Store.YES));
        doc.add(new StringField("website", website, Field.Store.YES));
        doc.add(new StringField("address", address, Field.Store.YES));
        doc.add(new StringField("phone", phone, Field.Store.YES));
        doc.add(new TextField("tags", tags, Field.Store.YES));
        doc.add(new StringField("ratingAverage", String.valueOf(ratingAverage), Field.Store.YES));
        doc.add(new StringField("lat", String.valueOf(lat), Field.Store.YES));
        doc.add(new StringField("lng", String.valueOf(lng), Field.Store.YES));
        w.addDocument(doc);
    }

    /**
     * Check if two Items are close
     * @param lat1 representing Item 1 latitude
     * @param lat2 representing Item 2 latitude
     * @param lon1 representing Item 1 longitude
     * @param lon2 representing Item 2 longitude
     * @return boolean flag
     */
    private boolean isNearbyItem(double lat1, double lat2, double lon1, double lon2) {
        return Utils.distance(lat1, lat2, lon1, lon2, 0.0, 0.0) < HORmessages.THRESHOLD;
    }
}