import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Comparator;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
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

import org.apache.lucene.search.TopScoreDocCollector;

import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.store.SimpleFSDirectory;

import org.apache.lucene.util.Version;

//TODO find better place for file
//TODO delete file after indexing

public class Indexer {
	private String tempFileName;
	private String fieldName;

	public Indexer() {
		fieldName = "SchemaIndependentIndex";
		tempFileName = "ycbilge3439349TempFile.txt";
	}

	public void readFromFile(String filePath) throws IOException {
		File folder = new File(filePath);
		File[] listOfFiles = folder.listFiles();
		Arrays.sort(listOfFiles, new Comparator<File>() {
			@Override
			public int compare(File o1, File o2) {
				int n1 = extractNumber(o1.getName());
				int n2 = extractNumber(o2.getName());
				return n1 -n2;
			}
			private int extractNumber(String name) {
                int i = 0;
                try {
                    int e = name.lastIndexOf('.');
                    String number = name.substring(0, e);
                    i = Integer.parseInt(number);
                } catch(Exception e) {
                    i = 0; // if filename does not match the format
                           // then default to 0
                }
                return i;
            }
		});
		
		
		/*
		 * Arrays.sort(files, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                int n1 = extractNumber(o1.getName());
                int n2 = extractNumber(o2.getName());
                return n1 - n2;
            }

            private int extractNumber(String name) {
                int i = 0;
                try {
                    int s = name.indexOf('_')+1;
                    int e = name.lastIndexOf('.');
                    String number = name.substring(s, e);
                    i = Integer.parseInt(number);
                } catch(Exception e) {
                    i = 0; // if filename does not match the format
                           // then default to 0
                }
                return i;
            }
        });
		 */
		
		
		
		
		
		
		
		
		
		
		
		StringBuilder sb = new StringBuilder();
		// String modifiedLine = "";
		for (int i = 0; i < listOfFiles.length; i++) {
			File file = listOfFiles[i];
			//fSystem.out.println("File name = " + file.getName());
			if (file.isFile() && file.getName().endsWith(".txt")) {
				FileInputStream fstream = new FileInputStream(file);
				DataInputStream in = new DataInputStream(fstream);
				BufferedReader br = new BufferedReader(
						new InputStreamReader(in));
				String modifiedLine;
				String line;
				while ((line = br.readLine()) != null) {
					// System.out.print(line);
					modifiedLine = line.replaceAll("[^a-zA-Z]", " ");
					modifiedLine = modifiedLine.toLowerCase();
					sb.append(modifiedLine);
					// sb.append(line);
				}
				sb.append(" ### ");
				// System.out.println(" ###");
				br.close();
			}
		}
		// System.out.println("String = " + sb.toString());
		createTempFile(sb);
	}

	public void createTempFile(StringBuilder sb) throws IOException {

		File file = new File(tempFileName);
		BufferedWriter output = new BufferedWriter(new FileWriter(file));
		output.write(sb.toString());
		output.close();
		// file.delete();
	}

	public void startIndexingDocument(String indexPath) throws IOException {
		Analyzer analyzer = new WhitespaceAnalyzer(Version.LUCENE_44);
		SimpleFSDirectory directory = new SimpleFSDirectory(new File(indexPath));
		IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_44,
				analyzer);
		IndexWriter writer = new IndexWriter(directory, config);
		indexDocs(writer);
		writer.close();
	}

	private void indexDocs(IndexWriter w) throws IOException {
		Document doc = new Document();
		File file = new File(tempFileName);

		BufferedReader br = new BufferedReader(new FileReader(tempFileName));
		// System.out.println(br.readLine());
		// Field field = new StringField(fieldName, file.getPath(),
		// Field.Store.YES);
		// Field field = new StringField(fieldName, br.readLine().toString(),
		// Field.Store.YES);
		Field field = new TextField(fieldName, br.readLine().toString(),
				Field.Store.YES);

		doc.add(field);
		w.addDocument(doc);
		file.delete();
	}

	public void printNumberOfIndexedDocs(String indexPath)
			throws ParseException, IOException {
		File indexDirectory = new File(indexPath);
		IndexReader reader = IndexReader.open(FSDirectory.open(indexDirectory));
		//System.out.println("total docs " + reader.maxDoc()); // return total
																// docs in index
		// System.out.println();
	}

	public static void main(String[] args) throws IOException, ParseException {

		if (args[0] == null || args[1] == null) {
			System.out.println("Usage Error : provide two arguments");
		} else {
			String dirPath = args[0];
			String indexPath = args[1];
			Indexer indexer = new Indexer();
			indexer.readFromFile(dirPath);
			indexer.startIndexingDocument(indexPath);
			//indexer.printNumberOfIndexedDocs(indexPath);
			// indexer.readFromIndex(indexPath);
		}

	}

}