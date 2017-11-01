import com.sun.deploy.util.StringUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;
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

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Scanner;

import static java.lang.System.exit;

/**
 * Created by dima on 11/1/17.
 */
public class MainApp {

    static public void main(String[] args) throws IOException {

        String path = args[0];

        System.out.println(path);
        File directory = new File(path);

        Collection<File> zips = FileUtils.listFiles(
                directory,
                new RegexFileFilter(".+.[ZIP|zip]"),
                DirectoryFileFilter.DIRECTORY
        );


        System.out.println("Starting indexing " + zips.size());

        ZipUtils zipUtils = new ZipUtils();

        String temporaryDirectory = "/home/dima/Загрузки/PG/tmp";

        StandardAnalyzer analyzer = new StandardAnalyzer();
        Directory index = new RAMDirectory();
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        IndexWriter w = new IndexWriter(index, config);

        for (File zip : zips) {
            try {
                zipUtils.unzip(zip, temporaryDirectory);
            } catch (IOException e) {
            }

            Collection<File> files = FileUtils.listFiles(
                    new File(temporaryDirectory),
                    new RegexFileFilter(".+.txt"),
                    DirectoryFileFilter.DIRECTORY
            );

            for (File file : files) {
                String title = findTitle(file);
                if (title != null) {
                    addDoc(w, title, zip.getPath());
                    System.out.println("Indexed: " + file.getPath());
                }
            }

            try {
                FileUtils.deleteDirectory(new File(temporaryDirectory));
            } catch (IOException e) {
            }
        }

        w.close();

        System.out.println("DONE. \n Query mode started. ");

        while (true) {
            String querystr = askForTitle();
            if (querystr.equals("-1")) {
                System.out.println("Bye!");
                exit(0);
            }
            Query q = null;
            try {
                q = new QueryParser("title", analyzer).parse(querystr);
            } catch (ParseException e) {
                System.out.println("Error parsing query");
            }

            processQuery(index, q);
        }

    }

    private static String askForTitle() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("\n\nPlease enter title to search: ");
        String title = scanner.nextLine();
        return title;
    }

    private static void processQuery(Directory index, Query q) throws IOException {
        int hitsPerPage = 10;
        IndexReader reader = DirectoryReader.open(index);
        IndexSearcher searcher = new IndexSearcher(reader);
        TopDocs docs = searcher.search(q, hitsPerPage);
        ScoreDoc[] hits = docs.scoreDocs;

        System.out.println("Found " + hits.length + " hits.");
        for (int i = 0; i < hits.length; ++i) {
            int docId = hits[i].doc;
            Document d = searcher.doc(docId);
            System.out.println((i + 1) + ". " + d.get("title") + "\t" + d.get("path"));
        }
    }

    private static void addDoc(IndexWriter w, String title, String path) throws IOException {
        Document doc = new Document();
        doc.add(new TextField("title", title, Field.Store.YES));
        doc.add(new StringField("path", path, Field.Store.YES));
        w.addDocument(doc);
    }

    private static String findTitle(File file) throws IOException {
        Scanner scanner = new Scanner(file);

        String title = "";
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (line.contains("Title:")) {
                String[] parts = line.split(":");
                title += parts[1].trim();
                if (scanner.hasNextLine()) {
                    String second = scanner.nextLine();
                    if (second != null && !second.isEmpty()) {
                        title += "\n" + second.trim();
                    }
                }
                return title;
            }
        }
        return null;
    }
}
