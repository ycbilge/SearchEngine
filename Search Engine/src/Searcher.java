import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.util.Version;

class ValueComparator implements Comparator<String> {

	Map<String, Float> base;

	public ValueComparator(Map<String, Float> base) {
		this.base = base;
	}

	// Note: this comparator imposes orderings that are inconsistent with
	// equals.
	public int compare(String a, String b) {
		if (base.get(a) >= base.get(b)) {
			return -1;
		} else {
			return 1;
		} // returning 0 would merge keys
	}
}

public class Searcher {

	private String fieldName;

	public Searcher() {
		fieldName = "SchemaIndependentIndex";
	}

	private Map<String, String> mapToFile(String[] parts) {
		int id = 1;
		Map<String, String> fileMap = new HashMap<String, String>();
		for (String part : parts) {
			if (!part.matches("\\s+"))
				fileMap.put(id + ".txt", part);
			id++;
		}
		return fileMap;
	}

	/*
	 * maps string into files
	 */
	private String[] splitFile(String result) {
		String[] parts = result.split("###");
		return parts;

	}

	private void printResultsMap(Map<String, Float> resultMap) {
		// List list = new LinkedList(resultMap.entrySet());
		// Collections.sort(list, new Comparator() {
		//
		// public int compare(Object o1, Object o2) {
		// Float f1 = (Float) ((Map.Entry) o1).getValue();
		// Float f2 = (Float) ((Map.Entry) o1).getValue();
		// return f1.compareTo(f2);
		// }
		// });
		// for (Map.Entry<String, Float> entry : resultMap.entrySet()) {
		// if (entry.getValue() != 0.0F) {
		// System.out.println(entry.getKey() + " " + entry.getValue());
		// }
		//
		// }
		ValueComparator bvc = new ValueComparator(resultMap);
		TreeMap<String, Float> sorted_resultMap = new TreeMap<String, Float>(
				bvc);
		sorted_resultMap.putAll(resultMap);
		for (Map.Entry<String, Float> entry : sorted_resultMap.entrySet()) {
			if (entry.getValue() != 0.0F) {
				System.out.println(entry.getKey() + " " + entry.getValue());
			}
		}

	}

	private Map<String, Float> calculateScoreFromIndexVals(
			HashMap<String, ArrayList<IndexTuple>> map) {
		Map<String, Float> lastScoresMap = new HashMap<String, Float>();
		for (Map.Entry<String, ArrayList<IndexTuple>> entry : map.entrySet()) {
			ArrayList<IndexTuple> list = entry.getValue();
			float sum = 0.0F;
			if (list != null)
				sum = formulaForAnd(list);
			lastScoresMap.put(entry.getKey(), sum);
		}

		return lastScoresMap;

	}

	private float formulaForAnd(ArrayList<IndexTuple> tupleArr) {
		float sum = 0.0F;
		for (IndexTuple indexTuple : tupleArr) {
			int val1 = indexTuple.getLeft();
			int val2 = indexTuple.getRight();
			int diff = val1 - val2;
			if (diff < 0) {
				diff *= -1;
			}
			diff++;
			// System.out.println("Diff = " + diff);
			float val = 1 / (float) diff;

			sum += val;
		}
		return sum;
	}

	private ArrayList<IndexTuple> findCoversForTwoIndexes(
			ArrayList<Integer> index1, ArrayList<Integer> index2) {
		ArrayList<IndexTuple> indexTupleList = new ArrayList<IndexTuple>();
		for (int i = 0; i < index1.size(); i++) {
			int min = 99999;
			int valIndex1 = 0;
			int valIndex2 = 0;
			for (int i2 = 0; i2 < index2.size(); i2++) {
				if (index2.get(i2) < index1.get(i)) {
					continue;
				} else {
					int value1 = index1.get(i);
					int value2 = index2.get(i2);
					int diff = value1 - value2;
					if (diff < 0) {
						diff *= -1;
					}
					if (diff < min) {
						min = diff;
						valIndex1 = value1;
						valIndex2 = value2;
					}
				}
			}
			if (valIndex1 != 0 && valIndex2 != 0) {
				IndexTuple indexTuple = new IndexTuple(valIndex1, valIndex2);
				indexTupleList.add(indexTuple);
			}

		}
		for (int i = 0; i < index2.size(); i++) {
			int min = 99999;
			int valIndex1 = 0;
			int valIndex2 = 0;
			for (int i2 = 0; i2 < index1.size(); i2++) {
				if (index2.get(i) > index1.get(i2)) {
					continue;
				} else {
					int value1 = index2.get(i);
					int value2 = index1.get(i2);
					int diff = value1 - value2;
					if (diff < 0) {
						diff *= -1;
					}
					if (diff < min) {
						min = diff;
						valIndex1 = value1;
						valIndex2 = value2;
					}
				}
			}
			if (valIndex1 != 0 && valIndex2 != 0) {
				IndexTuple indexTuple = new IndexTuple(valIndex1, valIndex2);
				indexTupleList.add(indexTuple);
			}
		}
		return indexTupleList;
	}

	private ArrayList<Integer> findIndexOfQueryInFile(
			ArrayList<String> fileTextList, String queryWord) {
		ArrayList<Integer> indexArr = new ArrayList<Integer>();
		for (int index = 0; index < fileTextList.size(); index++) {
			if (fileTextList.get(index).equals(queryWord)) {
				// System.out.println("bu indexi buldu = " +
				// fileTextList.get(index));
				int addingIndex = index + 1;
				indexArr.add(addingIndex);
			}
		}
		return indexArr;
	}

	private Map<String, String> readIndex(String indexPath, String queryString)
			throws IOException, ParseException {
		String resultString = "";
		Analyzer analyzer = new WhitespaceAnalyzer(Version.LUCENE_44);
		QueryParser parser = new QueryParser(Version.LUCENE_44, fieldName,
				analyzer);
		Query query = parser.parse(queryString);
		IndexReader reader = IndexReader.open(NIOFSDirectory.open(new File(
				indexPath)));
		IndexSearcher searcher = new IndexSearcher(reader);
		TopScoreDocCollector collector = TopScoreDocCollector.create(10, true);
		searcher.search(query, collector);
		ScoreDoc[] hits = collector.topDocs().scoreDocs;

		for (int i = 0; i < hits.length; i++) {
			int docId = hits[i].doc;
			Document d = searcher.doc(docId);
			resultString = d.get(fieldName);
		}

		reader.close();
		// System.out.println("result String : ");
		// System.out.println(resultString);
		String[] parts = splitFile(resultString);
		Map<String, String> fileMap = mapToFile(parts);

		return fileMap;

	}

	private int findMin(int a, int b, int c) {
		if (a <= b && a <= c) {
			return a;
		} else if (b <= c && b <= a) {
			return b;
		} else {
			return c;
		}
	}

	private int findMax(int a, int b, int c) {
		if (a >= b && a >= c) {
			return a;
		} else if (b >= c && b >= a) {
			return b;
		} else {
			return c;
		}
	}

	// private ArrayList<IndexTuple> findCoversFor3Query(
	// ArrayList<IndexTuple> indexTuples, ArrayList<Integer> index3) {
	// ArrayList<IndexTuple> listToBeReturned = new ArrayList<IndexTuple>();
	//
	// for (Integer index : index3) {
	// int min = 9999;
	// int valToBeSubmitted = 0;
	// int leftVal = 0;
	// int rightVal = 0;
	//
	// for (IndexTuple indexTuple : indexTuples) {
	//
	// if (indexTuple.getRight() < index) {
	// continue;
	// }
	// int diff1 = indexTuple.getLeft() - index;
	// if (diff1 < 0) {
	// diff1 *= -1;
	// }
	// int diff2 = indexTuple.getRight() - index;
	//
	// if (diff2 < 0) {
	// diff2 *= -1;
	// }
	// int allDiff = diff1 + diff2;
	// if (allDiff < 0) {
	// allDiff *= -1;
	// }
	// if (allDiff <= min) {
	// min = allDiff;
	// valToBeSubmitted = index;
	// leftVal = indexTuple.getLeft();
	// rightVal = indexTuple.getRight();
	// }
	// }
	// if (leftVal != 0 || rightVal != 0 || valToBeSubmitted != 0) {
	// int minVal = findMin(valToBeSubmitted, leftVal, rightVal);
	// int maxVal = findMax(valToBeSubmitted, leftVal, rightVal);
	// IndexTuple newIndexTuple = new IndexTuple(minVal, maxVal);
	// listToBeReturned.add(newIndexTuple);
	// }
	// }
	//
	// return listToBeReturned;
	//
	// }

	private ArrayList<IndexTuple> findCoversFor3Query(
			ArrayList<IndexTuple> indexTuples, ArrayList<Integer> index3) {
		ArrayList<IndexTuple> listToBeReturned = new ArrayList<IndexTuple>();
		// System.out.println(index3);
		for (IndexTuple indexTuple : indexTuples) {
			int min = 99999;
			int valToBeSubmitted = 0;

			for (Integer index : index3) {
				// if(index > indexTuple.getLeft() && index >
				// indexTuple.getRight()) {
				// break;
				// }

				int diff1 = indexTuple.getLeft() - index;
				if (diff1 < 0) {
					diff1 *= -1;
				}
				int diff2 = indexTuple.getRight() - index;

				if (diff2 < 0) {
					diff2 *= -1;
				}
				int allDiff = diff1 + diff2;
				if (allDiff < 0) {
					allDiff *= -1;
				}
				if (allDiff < min) {
					min = allDiff;
					valToBeSubmitted = index;
				}
			}
			int minVal = findMin(valToBeSubmitted, indexTuple.getLeft(),
					indexTuple.getRight());
			int maxVal = findMax(valToBeSubmitted, indexTuple.getLeft(),
					indexTuple.getRight());
			// System.out.println("valToBe = " + valToBeSubmitted + "left = "
			// + indexTuple.getLeft() + " - " + indexTuple.getRight());
			IndexTuple newIndexTuple = new IndexTuple(minVal, maxVal);
			listToBeReturned.add(newIndexTuple);

		}

		return listToBeReturned;
	}

	public void findScoresAlg(String indexPath, String query, String operation)
			throws IOException, ParseException {
		HashMap<String, ArrayList<IndexTuple>> indexHashMap = new HashMap<String, ArrayList<IndexTuple>>();

		HashMap<String, String> fileContentMap = (HashMap<String, String>) readIndex(
				indexPath, query);
		String[] wordsOfQuery = query.trim().split("\\s+");
		if (operation.equals("-and")) {
			ArrayList<String> fileTextList = null;
			for (Map.Entry<String, String> entry : fileContentMap.entrySet()) {
				fileTextList = new ArrayList<String>();
				String textFromFile = entry.getValue();
				String[] textWords = textFromFile.trim().split("\\s+");
				for (String string : textWords) {
					fileTextList.add(string);
				}
				if (wordsOfQuery.length == 3) {
					// TODO ONCE BU - BASLAMADAN ONCE KAGIDA YAZ
					// (wordsOfQuery(3).indexes = a, indexArr1, indexArr2)
					// her a icin indexArr1.left cikar sonra right indan cikar
					// toplamlari min olacak
					// ona gore her min i bul
					// TODO check et ucunden biri bossa ikiliye gonder
					// System.out.println("file = " + entry.getKey() +
					// " size = " + fileTextList.size());
//					System.out.println("File name = " + entry.getKey());
//					System.out.println("query = " + wordsOfQuery[0]);
					ArrayList<Integer> indexArray1 = findIndexOfQueryInFile(
							fileTextList, wordsOfQuery[0]);
//					System.out.println("indexArr1 : ");
//					for (Integer integer : indexArray1) {
//						System.out.println(integer);
//					}
					ArrayList<Integer> indexArray2 = findIndexOfQueryInFile(
							fileTextList, wordsOfQuery[1]);
//					System.out.println("query2 = " + wordsOfQuery[1]);
//					System.out.println("indexArr2 : ");
//					for (Integer integer : indexArray2) {
//						System.out.println(integer);
//					}
					ArrayList<Integer> indexArray3 = findIndexOfQueryInFile(
							fileTextList, wordsOfQuery[2]);
//					System.out.println("query3 = " + wordsOfQuery[2]);
//					System.out.println("indexArr3 : ");
//					for (Integer integer : indexArray3) {
//						System.out.println(integer);
//					}
					if (indexArray1.size() != 0 && indexArray2.size() != 0
							&& indexArray3.size() != 0) {
						ArrayList<IndexTuple> indexTuples = findCoversForTwoIndexes(
								indexArray1, indexArray2);
//						System.out.println("index tuple for arr1 arr2 : ");
//						for (IndexTuple indexTuple : indexTuples) {
//							System.out.println(indexTuple.getLeft() + " - "
//									+ indexTuple.getRight());
//						}
						ArrayList<IndexTuple> indexTuples2 = findCoversFor3Query(
								indexTuples, indexArray3);
						
						HashSet<IndexTuple> hs = new HashSet<IndexTuple>();
						hs.addAll(indexTuples2);
						indexTuples2.clear();
						indexTuples2.addAll(hs);
//						System.out.println("index tuple for arr1 arr2 ar3 : ");
//						for (IndexTuple indexTuple : indexTuples2) {
//							System.out.println(indexTuple.getLeft() + " - "
//									+ indexTuple.getRight());
//						}
						if (indexTuples2.size() != 0) {
							indexHashMap.put(entry.getKey(), indexTuples2);
						} else {
							indexHashMap.put(entry.getKey(), null);
						}
					}
					// } else if (indexArray1.size() == 0) {
					// // TODO handle
					// } else if (indexArray2.size() == 0) {
					// // TODO handle
					// } else {
					//
					// // TODO handle
					// // indexArry3.size() == 0
					//
					// }

				} else if (wordsOfQuery.length == 2) {
					ArrayList<Integer> indexArray1 = findIndexOfQueryInFile(
							fileTextList, wordsOfQuery[0]);
					ArrayList<Integer> indexArray2 = findIndexOfQueryInFile(
							fileTextList, wordsOfQuery[1]);
					ArrayList<IndexTuple> indexTuples = findCoversForTwoIndexes(
							indexArray1, indexArray2);
					if (indexTuples.size() != 0) {
						indexHashMap.put(entry.getKey(), indexTuples);
					} else {
						indexHashMap.put(entry.getKey(), null);
					}
				}
			}
			Map<String, Float> resultMap = calculateScoreFromIndexVals(indexHashMap);
			printResultsMap(resultMap);
		} else if (operation.equals("-or")) {
			Map<String, Float> txtOccurranceMap = new HashMap<String, Float>();
			ArrayList<String> fileTextList = null;
			for (Map.Entry<String, String> entry : fileContentMap.entrySet()) {
				fileTextList = new ArrayList<String>();
				String textFromFile = entry.getValue();
				String[] textWords = textFromFile.trim().split("\\s+");
				for (String string : textWords) {
					fileTextList.add(string);
				}
				if (wordsOfQuery.length == 3 ) {
					ArrayList<Integer> indexArray1 = findIndexOfQueryInFile(
							fileTextList, wordsOfQuery[0]);
					ArrayList<Integer> indexArray2 = findIndexOfQueryInFile(
							fileTextList, wordsOfQuery[1]);
					ArrayList<Integer> indexArray3 = findIndexOfQueryInFile(
							fileTextList, wordsOfQuery[2]);
					if(indexArray1.size() == 0 && indexArray2.size() != 0 && indexArray3.size() != 0) {
						ArrayList<Integer> indexArrayy1 = findIndexOfQueryInFile(
								fileTextList, wordsOfQuery[1]);
						ArrayList<Integer> indexArrayy2 = findIndexOfQueryInFile(
								fileTextList, wordsOfQuery[2]);
						ArrayList<IndexTuple> indexTuples = findCoversForTwoIndexes(
								indexArrayy1, indexArrayy2);
						Float occurenceNum  = 0.666666667F;
						if (indexTuples.size() != 0) {
							indexHashMap.put(entry.getKey(), indexTuples);
							txtOccurranceMap.put(entry.getKey(), occurenceNum);
						} else {
							indexHashMap.put(entry.getKey(), null);
							txtOccurranceMap.put(entry.getKey(), 0.0F);
						}
					}else if(indexArray1.size() != 0 && indexArray2.size() == 0 && indexArray3.size() != 0) {
						ArrayList<Integer> indexArrayy1 = findIndexOfQueryInFile(
								fileTextList, wordsOfQuery[0]);
						ArrayList<Integer> indexArrayy2 = findIndexOfQueryInFile(
								fileTextList, wordsOfQuery[2]);
						ArrayList<IndexTuple> indexTuples = findCoversForTwoIndexes(
								indexArrayy1, indexArrayy2);
						Float occurenceNum  = 0.666666667F;
						if (indexTuples.size() != 0) {
							indexHashMap.put(entry.getKey(), indexTuples);
							txtOccurranceMap.put(entry.getKey(), occurenceNum);
						} else {
							indexHashMap.put(entry.getKey(), null);
							txtOccurranceMap.put(entry.getKey(), 0.0F);
						}
					}else if(indexArray1.size() != 0 && indexArray2.size() != 0 && indexArray3.size() == 0) {
						ArrayList<Integer> indexArrayy1 = findIndexOfQueryInFile(
								fileTextList, wordsOfQuery[0]);
						ArrayList<Integer> indexArrayy2 = findIndexOfQueryInFile(
								fileTextList, wordsOfQuery[1]);
						ArrayList<IndexTuple> indexTuples = findCoversForTwoIndexes(
								indexArrayy1, indexArrayy2);
						Float occurenceNum  = 0.666666667F;
						if (indexTuples.size() != 0) {
							indexHashMap.put(entry.getKey(), indexTuples);
							txtOccurranceMap.put(entry.getKey(), occurenceNum);
						} else {
							indexHashMap.put(entry.getKey(), null);
							txtOccurranceMap.put(entry.getKey(), 0.0F);
						}
					}else if(indexArray1.size() != 0 && indexArray2.size() == 0 && indexArray3.size() == 0) {
						ArrayList<Integer> indexArrayy1 = findIndexOfQueryInFile(
								fileTextList, wordsOfQuery[0]);
						Float occurenceNum  = 0.333333333F;
						ArrayList<IndexTuple> indexTuples = new ArrayList<IndexTuple>();
						for (Integer integer : indexArrayy1) {
							IndexTuple indexTuple = new IndexTuple(0, integer);
							indexTuples.add(indexTuple);
							
						}
						if (indexTuples.size() != 0) {
							indexHashMap.put(entry.getKey(), indexTuples);
							txtOccurranceMap.put(entry.getKey(), occurenceNum);
						} else {
							indexHashMap.put(entry.getKey(), null);
							txtOccurranceMap.put(entry.getKey(), 0.0F);
						}
					}else if(indexArray1.size() == 0 && indexArray2.size() != 0 && indexArray3.size() == 0) {
						ArrayList<Integer> indexArrayy1 = findIndexOfQueryInFile(
								fileTextList, wordsOfQuery[1]);
						Float occurenceNum  = 0.333333333F;
						ArrayList<IndexTuple> indexTuples = new ArrayList<IndexTuple>();
						for (Integer integer : indexArrayy1) {
							IndexTuple indexTuple = new IndexTuple(0, integer);
							indexTuples.add(indexTuple);
							
						}
						if (indexTuples.size() != 0) {
							indexHashMap.put(entry.getKey(), indexTuples);
							txtOccurranceMap.put(entry.getKey(), occurenceNum);
						} else {
							indexHashMap.put(entry.getKey(), null);
							txtOccurranceMap.put(entry.getKey(), 0.0F);
						}
					}else if(indexArray1.size() == 0 && indexArray2.size() == 0 && indexArray3.size() != 0) {
						ArrayList<Integer> indexArrayy1 = findIndexOfQueryInFile(
								fileTextList, wordsOfQuery[2]);
						Float occurenceNum  = 0.333333333F;
						ArrayList<IndexTuple> indexTuples = new ArrayList<IndexTuple>();
						for (Integer integer : indexArrayy1) {
							IndexTuple indexTuple = new IndexTuple(0, integer);
							indexTuples.add(indexTuple);
							
						}
						if (indexTuples.size() != 0) {
							indexHashMap.put(entry.getKey(), indexTuples);
							txtOccurranceMap.put(entry.getKey(), occurenceNum);
						} else {
							indexHashMap.put(entry.getKey(), null);
							txtOccurranceMap.put(entry.getKey(), 0.0F);
						}
					}else {
						if (indexArray1.size() != 0 && indexArray2.size() != 0
								&& indexArray3.size() != 0) {
							ArrayList<IndexTuple> indexTuples = findCoversForTwoIndexes(
									indexArray1, indexArray2);
							ArrayList<IndexTuple> indexTuples2 = findCoversFor3Query(
									indexTuples, indexArray3);
							
							HashSet<IndexTuple> hs = new HashSet<IndexTuple>();
							hs.addAll(indexTuples2);
							indexTuples2.clear();
							indexTuples2.addAll(hs);
							if (indexTuples2.size() != 0) {
								indexHashMap.put(entry.getKey(), indexTuples2);
								txtOccurranceMap.put(entry.getKey(), 1.0F);
							} else {
								indexHashMap.put(entry.getKey(), null);
								txtOccurranceMap.put(entry.getKey(), 0.0F);
							}
						}

					}					
				} else if (wordsOfQuery.length == 2) {//OR DA IKILI OLMIR HERALDE
					ArrayList<Integer> indexArray1 = findIndexOfQueryInFile(
							fileTextList, wordsOfQuery[0]);
					ArrayList<Integer> indexArray2 = findIndexOfQueryInFile(
							fileTextList, wordsOfQuery[1]);
					ArrayList<IndexTuple> indexTuples = findCoversForTwoIndexes(
							indexArray1, indexArray2);
					Float occurenceNum  = 0.0F;
					if(indexArray1.size() != 0 && indexArray2.size() != 0) {
						occurenceNum = 1.0F;
					}else if((indexArray1.size() > 0 && indexArray2.size() == 0) || (indexArray2.size() > 0 && indexArray1.size() == 0)) {
						occurenceNum = 0.5F;
					}
					
					if (indexTuples.size() != 0) {
						indexHashMap.put(entry.getKey(), indexTuples);
						txtOccurranceMap.put(entry.getKey(), occurenceNum);
					} else {
						indexHashMap.put(entry.getKey(), null);
						txtOccurranceMap.put(entry.getKey(), 0.0F);
					}
					
				}
			}
			Map<String, Float> resultMap = calculateScoreFromIndexValsForOr(indexHashMap, (HashMap<String, Float>) txtOccurranceMap);
			printResultsMap(resultMap);

		}

	}
	private Float formulaForOr(ArrayList<IndexTuple> tupleArr, Float oVal) {
		float sum = 0.0F;
		for (IndexTuple indexTuple : tupleArr) {
			int val1 = indexTuple.getLeft();
			int val2 = indexTuple.getRight();
			int diff = val1 - val2;
			if (diff < 0) {
				diff *= -1;
			}
			diff++;
			// System.out.println("Diff = " + diff);
			float val = 1 / (float) diff;

			sum += val;
		}
		sum *= oVal;
		sum *= oVal;
		return sum;
	}
	private Map<String, Float> calculateScoreFromIndexValsForOr(
			HashMap<String, ArrayList<IndexTuple>> map, HashMap<String, Float> occurranceMap) {
		Map<String, Float> lastScoresMap = new HashMap<String, Float>();
		for (Map.Entry<String, ArrayList<IndexTuple>> entry : map.entrySet()) {
			ArrayList<IndexTuple> list = entry.getValue();
			float sum = 0.0F;
			if (list != null) {
				Float val = occurranceMap.get(entry.getKey());
				sum = formulaForOr(list, val);
			}
			lastScoresMap.put(entry.getKey(), sum);
		}
		return lastScoresMap;
	}

	public static void main(String[] args) throws IOException, ParseException {
		// TODO usage goster girilmezse
		String indexPath = args[0];
		String OP = args[1]; // AND OR
		String query = "";
		for (int index = 2; index < args.length; index++) {
			query += args[index];
			query += " ";
		}
		Searcher searcher = new Searcher();
		searcher.findScoresAlg(indexPath, query, OP);
	}

}