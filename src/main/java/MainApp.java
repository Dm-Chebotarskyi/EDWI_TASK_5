
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Scanner;

import static java.lang.System.exit;

/**
 * Created by dima on 11/1/17.
 */
public class MainApp {

    static private String START = "START OF THIS PROJECT GUTENBERG EBOOK";
    static private String END = "END OF THIS PROJECT GUTENBERG EBOOK";

    static public void main(String[] args) throws IOException {

        String path = args[0];

        File directory = new File(path);

        Collection<File> zips = FileUtils.listFiles(
                directory,
                new RegexFileFilter(".+.[ZIP|zip]"),
                DirectoryFileFilter.DIRECTORY
        );

        int count = zips.size();

        System.out.println("Starting indexing " + zips.size());

        ZipUtils zipUtils = new ZipUtils();

        String temporaryDirectory = "/home/dima/Загрузки/PG/tmp";

        StandardAnalyzer analyzer = new StandardAnalyzer();
        FSDirectory index = FSDirectory.open(Paths.get("/home/dima/index.lucene"));
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
                String body = findBody(file);
                if (title != null) {
                    addDoc(w, title, body, zip.getPath());
                    System.out.println("Indexed: " + file.getPath());
                }
            }

            try {
                FileUtils.deleteDirectory(new File(temporaryDirectory));
            } catch (IOException e) {
            }
        }

        w.close();

        System.out.println("Totaly: " + count);
        System.out.println("DONE.");

    }

    private static void addDoc(IndexWriter w, String title, String body, String path) throws IOException {
        Document doc = new Document();
        doc.add(new TextField("title", title, Field.Store.YES));
        doc.add(new TextField("body", body, Field.Store.YES));
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

    private static String findBody(File file) throws IOException {

        InputStream stream = new FileInputStream(file);


        final Scanner scanner = new Scanner(file);
        StringBuilder body = new StringBuilder();
        boolean toWrite = false;
        while (scanner.hasNextLine()) {
            final String lineFromFile = scanner.nextLine();

            if (toWrite)
                body.append(lineFromFile);
                body.append("\n");

            if (lineFromFile.contains(START)) {
                toWrite = true;
            } else if (lineFromFile.contains(END)) {
                break;
            }
        }
        return body.toString();
    }
}
