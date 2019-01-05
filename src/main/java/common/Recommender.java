package common;

import beans.recommender.Item;
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

import java.io.*;
import java.util.ArrayList;
import java.util.List;

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
     * @param CSVPath path of file
     */
    public Recommender(String CSVPath) {
        List<Item> pois = this.readCSV(CSVPath);

        this.analyzer = new StandardAnalyzer();
        this.index = new RAMDirectory();
        IndexWriterConfig config = new IndexWriterConfig(analyzer);

        IndexWriter w;
        try {
            w  = new IndexWriter(index, config);
            for (Item p : pois) {
                addDoc(w, p.getWebsite(), p.getName(), p.getAddress(), p.getPhone(), p.getTags(), p.getLat(), p.getLng());
            }
            w.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Lucene method for search into Index relevant document to recommend
     * @param query is string to querying the index
     * @return a list of relevant item to recommend
     * @throws IOException for Input/Output exception
     * @throws ParseException for search document exception
     */
    public List<Item> searchItems(String query) throws IOException, ParseException {
        Query q = new QueryParser("tags", this.analyzer).parse(query);

        int hitsPerPage = 10;
        IndexReader reader = DirectoryReader.open(this.index);
        IndexSearcher searcher = new IndexSearcher(reader);
        TopDocs docs = searcher.search(q, hitsPerPage);
        ScoreDoc[] hits = docs.scoreDocs;

        List<Item> items = new ArrayList<Item>();
        for (ScoreDoc hit : hits) {
            int docId = hit.doc;
            Document d = searcher.doc(docId);
            Item i = new Item(d.get("website"),
                    d.get("name"),
                    d.get("address"),
                    d.get("phone"),
                    d.get("tags"),
                    Float.valueOf(d.get("lat")),
                    Float.valueOf(d.get("lng")));
            i.setScore(hit.score);
            items.add(i);
        }
        reader.close();

        return items;
    }

    /**
     * Read items to recommend to user from a CSV file
     * @param csvFile representing the file path of CSV file
     * @return an item list
     */
    private List<Item> readCSV(String csvFile) {
        List<Item> pois = new ArrayList<Item>();
        InputStream in = getClass().getResourceAsStream(csvFile);
        BufferedReader br = null;
        String line;

        try {
            br = new BufferedReader(new InputStreamReader(in));

            while ((line = br.readLine()) != null) {
                // use comma as separator
                String[] items = line.split(HORmessages.CSV_SPLIT);
                pois.add(new Item(items[0], items[1], items[2], items[3], items[4],
                                items[5], Double.valueOf(items[6]), Integer.valueOf(items[7]),
                                Float.valueOf(items[8]), Float.valueOf(items[9])));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return pois;
    }

    /**
     * Lucene method for add a document to index representing the item to indexing
     * @param w is the IndexWriter of Lucene
     * @param website representing the website of item
     * @param name representing the name of item
     * @param address representing the address of item
     * @param phone representing the phone of item
     * @param tags representing the TAGS of item
     * @param lat representing the latitude of item
     * @param lng representing the longitude of item
     * @throws IOException for Input/Output exception
     */
    private static void addDoc(IndexWriter w,
                               String website, String name, String address, String phone, String tags,
                               float lat, float lng) throws IOException {
        Document doc = new Document();
        doc.add(new StringField("name", name, Field.Store.YES));
        doc.add(new StringField("website", website, Field.Store.YES));
        doc.add(new StringField("address", address, Field.Store.YES));
        doc.add(new StringField("phone", phone, Field.Store.YES));
        doc.add(new TextField("tags", tags, Field.Store.YES));
        doc.add(new StringField("lat", String.valueOf(lat), Field.Store.YES));
        doc.add(new StringField("lng", String.valueOf(lng), Field.Store.YES));
        w.addDocument(doc);
    }
}