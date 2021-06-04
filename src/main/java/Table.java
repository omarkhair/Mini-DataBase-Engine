import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.text.ParseException;
import java.util.*;

public class Table implements Serializable, TableObserver {
	private String tableName;
	private String clusteringKey;
	private String clusteringKeyType;
	private Vector<Column> columns;
	private Vector<Page> pages;
	private int pageMaxRows;
	private int lastPageId;
	private int lastIndexId;
	private Vector<GridIndex> indecies;

	public Table(String tableName, String clusteringKey, Hashtable<String, String> colNameType,
			Hashtable<String, String> colNameMin, Hashtable<String, String> colNameMax, int pageMaxRows) {
		this.tableName = tableName;
		this.clusteringKey = clusteringKey;
		this.clusteringKeyType = colNameType.get(clusteringKey);
		columns = new Vector<>();
		// first column is the primary key
		for (Map.Entry<String, String> ent : colNameType.entrySet()) {
			Column col = new Column(ent.getKey(), ent.getValue(), clusteringKey.equals(ent.getKey()), false,
					colNameMin.get(ent.getKey()), colNameMax.get(ent.getKey()));
			if (ent.getKey().equals(clusteringKey))
				columns.add(0, col);
			else
				columns.add(col);
		}
		create_metadata();
		pages = new Vector<>();
		this.pageMaxRows = pageMaxRows;
		lastPageId = 0;
		lastIndexId = 0;
		indecies = new Vector<>();
	}

	public Vector<GridIndex> getIndecies() {
		return indecies;
	}

	public void setIndecies(Vector<GridIndex> indecies) {
		this.indecies = indecies;
	}

	public int getLastIndexId() {
		return lastIndexId;
	}

	public void setLastIndexId(int lastIndexId) {
		this.lastIndexId = lastIndexId;
	}

	public String getTableName() {
		return tableName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public String getClusteringKey() {
		return clusteringKey;
	}

	public void setClusteringKey(String clusteringKey) {
		this.clusteringKey = clusteringKey;
	}

	public Vector<Column> getColumns() {
		return columns;
	}

	public void setColumns(Vector<Column> columns) {
		this.columns = columns;
	}

	public Vector<Page> getPages() {
		return pages;
	}

	public void setPages(Vector<Page> pages) {
		this.pages = pages;
	}

	public void insertRecord(Hashtable<String, Object> colNameValue) throws DBAppException {
		Vector<Object> data = new Vector<>();
		for (Column c : columns) {
			data.add(colNameValue.getOrDefault(c.getName(), null));
		}
		Tuple t = new Tuple(data);
		insertRecordOverflow(t);

//the commented method shifts all full pages to insert instead of creating extra pages
//		insertRecordHelper(t);
	}

	public void insertRecordOverflow(Tuple t) throws DBAppException {
		if (pages.size() == 0) {
			Page page = new Page(tableName, clusteringKeyType, lastPageId++, this);
			pages.add(page);
			page.insertRecord(t);
		} else {
			int target = binarySearch(t.getData().get(0));
			Page currentPage = pages.get(target);
			if (currentPage.getNumberOfTuples() < pageMaxRows) {
				currentPage.insertRecord(t);
				return;
			} else {
				if (target == pages.size() - 1) {
					Page nextPage = new Page(tableName, clusteringKeyType, lastPageId++, this);
					currentPage.insertRecord(t);
					Tuple lastRecord = currentPage.removeLastRecord();
					nextPage.insertRecord(lastRecord);
					pages.add(nextPage);
					return;
				}
				Page nextPage = pages.get(target + 1);
				if (nextPage.getNumberOfTuples() < pageMaxRows) {
					currentPage.insertRecord(t);
					Tuple lastRecord = currentPage.removeLastRecord();
					nextPage.insertRecord(lastRecord);
					return;
				}
				// current and next pages are both full, and an overflow page is needed
				Page overflowPage = new Page(tableName, clusteringKeyType, lastPageId++, this);
				currentPage.insertRecordInMemory(t);
				for (int i = pageMaxRows; i > pageMaxRows / 2; i--) {
					overflowPage.insertRecordInMemory(currentPage.removeLastRecordInMemory());
				}
				pages.add(target + 1, overflowPage);
				currentPage.writeData();
				currentPage.setData(null);
				overflowPage.writeData();
				overflowPage.setData(null);
			}
		}
	}

	public void insertRecordHelper(Tuple t) throws DBAppException {
		if (pages.size() == 0) {
			Page page = new Page(tableName, clusteringKeyType, lastPageId++, this);
			pages.add(page);
			// Collections.sort(pages);
			page.insertRecord(t);
		} else {
			// search for target page
			int target = binarySearch(t.getData().get(0));
			for (int i = target; i < pages.size(); i++) {
				Page currentPage = pages.get(i);
				if (currentPage.getNumberOfTuples() < pageMaxRows) {
					currentPage.insertRecord(t);
					return;
				} else {
					currentPage.insertRecord(t);
					t = currentPage.removeLastRecord();
				}
			}
			Page page = new Page(tableName, clusteringKeyType, lastPageId++, this);
			pages.add(page);
			// Collections.sort(pages);
			page.insertRecord(t);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private int binarySearch(Object key) {
		int lo = 0, hi = pages.size() - 1;

		while (lo <= hi) {
			int mid = (lo + hi) / 2;
			Page p = pages.get(mid);
			if ((((Comparable) key).compareTo(p.getMaxValue()) <= 0
					&& ((Comparable) key).compareTo(p.getMinValue()) >= 0)
					|| ((((Comparable) key).compareTo(p.getMaxValue()) >= 0) && mid + 1 < pages.size()
							&& ((Comparable) key).compareTo(pages.get(mid + 1).getMinValue()) < 0)
					|| ((((Comparable) key).compareTo(p.getMaxValue()) >= 0) && mid + 1 == pages.size())
					|| ((((Comparable) key).compareTo(p.getMinValue()) <= 0) && mid == 0)) {
				return mid;
			} else {
				if (((Comparable) key).compareTo(p.getMaxValue()) >= 0) {
					lo = mid + 1;
				} else
					hi = mid - 1;
			}
		}
		return -1;
	}

	public void updateRecord(String clusteringKey, Hashtable<String, Object> columnNameValue) throws DBAppException {
		Comparable newClusteringKey = Utilities.parseType(clusteringKey, clusteringKeyType);
		int target = binarySearch(newClusteringKey);
		Page p = pages.get(target);
		if (p.updateRecord(clusteringKey, columnNameValue, columns)) {
			if (p.getNumberOfTuples() == 0) {
				pages.remove(p);
			}
			Tuple t = p.getUpdatedClustering();
			insertRecordOverflow(t);
		}
	}

	public void deleteRecords(Hashtable<String, Object> colNameValue) throws DBAppException {
		if (!colNameValue.containsKey(clusteringKey)) {
			Vector<Page> toBeRemoved = new Vector<>();
			for (Page p : pages) {
				p.deleteRecords(colNameValue, columns);
				if (p.getNumberOfTuples() == 0) {
					toBeRemoved.add(p);
					p.deletePageFromDisk();
				}
			}
			pages.removeAll(toBeRemoved);
		} else {
			Object primaryKeyVal = colNameValue.get(clusteringKey);
			int target = binarySearch(primaryKeyVal);
//			System.out.println(target);
			Page p = pages.get(target);
			p.deleteRecords(colNameValue, columns);
			if (p.getNumberOfTuples() == 0) {
				pages.remove(p);
				p.deletePageFromDisk();
			}

		}
	}

	public void verifyInsertion(Hashtable<String, Object> colNameValue) throws DBAppException {
		// if meta dara is invalid create a new one
		try {
			readMetaData();
		} catch (DBAppException e) {
			create_metadata();
		}
		if (!colNameValue.containsKey(clusteringKey))
			throw new DBAppException("inserted record does not have a clustering key");
		for (Map.Entry<String, Object> ent : colNameValue.entrySet()) {
			Column c = getColumn(ent.getKey());
			if (c == null)
				throw new DBAppException("there is no column named " + ent.getKey());
			Object val = ent.getValue();
			if (val instanceof String) {
				if (!c.getDataType().equals("java.lang.String"))
					throw new DBAppException("mismatching data type of column " + ent.getKey());
				String val2 = (String) val;
				if (val2.compareTo(((String) c.getMax())) > 0 || val2.compareTo(((String) c.getMin())) < 0) {
					throw new DBAppException("out of bounds value for column " + ent.getKey());
				}
			} else if (val instanceof Integer) {
				if (!c.getDataType().equals("java.lang.Integer"))
					throw new DBAppException("mismatching data type of column " + ent.getKey());
				Integer val2 = (Integer) val;
				if (val2.compareTo(((Integer) c.getMax())) > 0 || val2.compareTo(((Integer) c.getMin())) < 0) {
					throw new DBAppException("out of bounds value for column " + ent.getKey());
				}
			} else if (val instanceof Double) {
				if (!c.getDataType().equals("java.lang.Double"))
					throw new DBAppException("mismatching data type of column " + ent.getKey());
				Double val2 = (Double) val;
				if (val2.compareTo(((Double) c.getMax())) > 0 || val2.compareTo(((Double) c.getMin())) < 0) {
					throw new DBAppException("out of bounds value for column " + ent.getKey());
				}
			} else if (val instanceof Date) {
				if (!c.getDataType().equals("java.util.Date"))
					throw new DBAppException("mismatching data type of column " + ent.getKey());
				Date val2 = (Date) val;
				if (val2.compareTo(((Date) c.getMax())) > 0 || val2.compareTo(((Date) c.getMin())) < 0) {
					throw new DBAppException(
							"out of bounds value for column " + ent.getKey() + " value: " + val2.toString());
				}
			} else
				throw new DBAppException("unsupported data type for column " + ent.getKey());
		}
	}

	public Column getColumn(String columnName) {
		for (Column c : columns) {
			if (c.getName().equals(columnName))
				return c;
		}
		return null;
	}

	public boolean verifyDeletion(Hashtable<String, Object> colNameValue) throws DBAppException {
		// if meta dara is invalid create a new one
		try {
			readMetaData();
		} catch (DBAppException e) {
			create_metadata();
		}
		for (Map.Entry<String, Object> ent : colNameValue.entrySet()) {
			Column c = getColumn(ent.getKey());
			if (c == null)
				throw new DBAppException("there is no column named " + ent.getKey());
			Object val = ent.getValue();
			if (val instanceof String) {
				if (!c.getDataType().equals("java.lang.String"))
					throw new DBAppException("mismatching data type of column " + ent.getKey());
				String val2 = (String) val;
				if (val2.compareTo(((String) c.getMax())) > 0 || val2.compareTo(((String) c.getMin())) < 0) {
					return false;
				}
			} else if (val instanceof Integer) {
				if (!c.getDataType().equals("java.lang.Integer"))
					throw new DBAppException("mismatching data type of column " + ent.getKey());
				Integer val2 = (Integer) val;
				if (val2.compareTo(((Integer) c.getMax())) > 0 || val2.compareTo(((Integer) c.getMin())) < 0) {
					return false;
				}
			} else if (val instanceof Double) {
				if (!c.getDataType().equals("java.lang.Double"))
					throw new DBAppException("mismatching data type of column " + ent.getKey());
				Double val2 = (Double) val;
				if (val2.compareTo(((Double) c.getMax())) > 0 || val2.compareTo(((Double) c.getMin())) < 0) {
					return false;
				}
			} else if (val instanceof Date) {
				if (!c.getDataType().equals("java.util.Date"))
					throw new DBAppException("mismatching data type of column " + ent.getKey());
				Date val2 = (Date) val;
				if (val2.compareTo(((Date) c.getMax())) > 0 || val2.compareTo(((Date) c.getMin())) < 0) {
					return false;
				}
			} else
				throw new DBAppException("unsupported data type for column " + ent.getKey());
		}
		return true;
	}

	public void verifyUpdate(String clusteringKeyValue, Hashtable<String, Object> columnNameValue)
			throws DBAppException {
		// if meta dara is invalid create a new one
		try {
			readMetaData();
		} catch (DBAppException e) {
			create_metadata();
		}
		Comparable newClusteringKey = Utilities.parseType(clusteringKeyValue, clusteringKeyType);
		loop: for (Column c : columns) {
			if (c.getName().equals(clusteringKey)) {
				if (newClusteringKey.compareTo(c.getMax()) > 0 && newClusteringKey.compareTo(c.getMin()) <= 0) {
					throw new DBAppException("The clustering Key is out of bounds");
				}
				break loop;
			}
		}

		for (Map.Entry<String, Object> ent : columnNameValue.entrySet()) {
			Column c = getColumn(ent.getKey());
			if (c == null)
				throw new DBAppException("there is no column named " + ent.getKey());
			Object val = ent.getValue();
			if (val instanceof String) {
				if (!c.getDataType().equals("java.lang.String"))
					throw new DBAppException("mismatching data type of column " + ent.getKey());
				String val2 = (String) val;
				if (val2.compareTo(((String) c.getMax())) > 0 || val2.compareTo(((String) c.getMin())) < 0) {
					throw new DBAppException("out of bounds value for column " + ent.getKey());
				}
			} else if (val instanceof Integer) {
				if (!c.getDataType().equals("java.lang.Integer"))
					throw new DBAppException("mismatching data type of column " + ent.getKey());
				Integer val2 = (Integer) val;
				if (val2.compareTo(((Integer) c.getMax())) > 0 || val2.compareTo(((Integer) c.getMin())) < 0) {
					throw new DBAppException("out of bounds value for column " + ent.getKey());
				}
			} else if (val instanceof Double) {
				if (!c.getDataType().equals("java.lang.Double"))
					throw new DBAppException("mismatching data type of column " + ent.getKey());
				Double val2 = (Double) val;
				if (val2.compareTo(((Double) c.getMax())) > 0 || val2.compareTo(((Double) c.getMin())) < 0) {
					throw new DBAppException("out of bounds value for column " + ent.getKey());
				}
			} else if (val instanceof Date) {
				if (!c.getDataType().equals("java.util.Date"))
					throw new DBAppException("mismatching data type of column " + ent.getKey());
				Date val2 = (Date) val;
				if (val2.compareTo(((Date) c.getMax())) > 0 || val2.compareTo(((Date) c.getMin())) < 0) {
					throw new DBAppException("out of bounds value for column " + ent.getKey());
				}
			} else
				throw new DBAppException("unsupported data type for column " + ent.getKey());
		}
	}

	public String toString() {
		String result = "";
		for (Column c : columns)
			result += c.toString() + "  ++  ";
		result = result.substring(0, result.length() - 4);
		result += "\n";
		for (Page p : pages) {
			result += "Page Number:  " + pages.indexOf(p) + "\n";
			result += p.toString();
			for (int i = p.getNumberOfTuples(); i < pageMaxRows; i++)
				result += "empty space \n";
			result += "\n";
		}
		return result;
	}

	public void readMetaData() throws DBAppException {
		Scanner sc;
		try {
			sc = new Scanner(new File("src/main/resources/metadata.csv"));
		} catch (Exception e) {
			throw new DBAppException("meta data file not found for table " + tableName);
		}
		sc.nextLine();
		for (Column c : columns) {
			if (!sc.hasNextLine())
				throw new DBAppException("invalid meta data file for table " + tableName);
			String[] line = sc.nextLine().split(",");
			// System.out.println(Arrays.toString(line));
			if (line.length != 7 || !line[0].equals(tableName) || !line[1].equals(c.getName())
					|| !line[2].equals(c.getDataType()) || !line[5].equals(c.getMinString())
					|| !line[6].equals(c.getMaxString())
					|| (c.getName().equals(clusteringKey) != line[3].equals("True")))
				throw new DBAppException("invalid meta data file for table " + tableName);
		}
		if (sc.hasNextLine())
			throw new DBAppException("invalid meta data file for table " + tableName);
	}

	public void create_metadata() {
		try {
			PrintWriter pw = new PrintWriter("src/main/resources/metadata.csv");
			pw.println("Table Name,Column Name,Column Type,ClusteringKey,Indexed,min,max");
			for (Column c : columns) {
				pw.println(tableName + "," + c.getName() + "," + c.getDataType() + ","
						+ (c.getName().equals(clusteringKey) ? "True" : "False") + ",False," + c.getMinString() + ","
						+ c.getMaxString());
			}
			pw.flush();
			pw.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

	}

	public Vector<Column> getCols(String[] columnNames) throws DBAppException {
		for (int i = 0; i < columnNames.length; i++)
			for (int j = i + 1; j < columnNames.length; j++) {
				if (columnNames[i].equals(columnNames[j]))
					throw new DBAppException("There are 2 columns with name " + columnNames[i]);
			}
		Vector<Column> cols = new Vector<>();
		for (String col : columnNames) {
			if (getColumn(col) == null)
				throw new DBAppException("There is no column named " + col + " in table " + tableName);
			cols.add(getColumn(col));
		}
		return cols;
	}

	// this method fills the given grid index with all the entries found in the
	// table
	public void populateIndex(GridIndex index) throws DBAppException {
		for (Page page : pages) {
			page.readData();
			Vector<Tuple> data = page.getData();
			for (Tuple t : data) {
				BucketEntry be = createBucketEntry(t, page.getId(), index);
				index.insertEntry(be);
			}

			// free the page data that we are done working with from memory
			page.setData(null);
		}
	}

	public BucketEntry createBucketEntry(Tuple t, int pageId, GridIndex index) {
		Vector<Object> entryData = new Vector<>();
		for (Column col : index.getColumns()) {
			int idxInTuple = Utilities.getIndexOf(col.getName(), columns);
			entryData.add(t.getIthVal(idxInTuple));
		}
		BucketEntry be = new BucketEntry(entryData, pageId, t.getIthVal(0));
		return be;
	}

	public void deleteUsingIndex(Hashtable<String, Object> colNameValue) throws DBAppException {
		SQLTerm[] sqlTerms = createSqlTerms(colNameValue);
		Vector<SQLTerm> selectTerms = new Vector<SQLTerm>();
		for (SQLTerm st : sqlTerms) {
			selectTerms.add(st);
		}
		GridIndex g = getBestIndex(selectTerms);
		if (g == null) {
			deleteRecords(colNameValue);
			return;
		} else {
			selectTerms = Utilities.getAndedTermsInIndex(g, (Vector<SQLTerm>) selectTerms.clone());
			TreeSet<Integer> pagesId = g.getPagesIds(selectTerms);
			for (Integer pageId : pagesId) {
				Page target = this.getPageWithId(pageId);
				target.deleteRecords(colNameValue, columns);
			}
		}

	}

	public void notifyInsert(int pageId, Tuple t) throws DBAppException {
		for (GridIndex index : indecies) {
			BucketEntry be = createBucketEntry(t, pageId, index);
			index.insertEntry(be);
		}

	}

	@Override
	public void notifyDelete(int pageId, Tuple t) throws DBAppException {
		for (GridIndex index : indecies) {
			BucketEntry be = createBucketEntry(t, pageId, index);
			index.deleteEntry(be);
		}

	}

	public GridIndex getBestIndex(Vector<SQLTerm> selectTerms) {
		int maxSoFar = 0;
		GridIndex resIndex = null;
		for (GridIndex g : indecies) {
			int columns = getNoMatchingColumns(g, selectTerms);
			if (columns > maxSoFar) {
				maxSoFar = columns;
				resIndex = g;
			} else if (columns == maxSoFar && resIndex != null) {
				if (g.getColumns().size() < resIndex.getColumns().size()) {
					resIndex = g;
				}
			}
		}
		return resIndex;
	}

	private int getNoMatchingColumns(GridIndex g, Vector<SQLTerm> selectTerms) {
		int res = 0;
		for (SQLTerm t : selectTerms)
			for (Column c : g.getColumns())
				if (c.getName().equals(t.get_strColumnName()))
					res++;
		return res;
	}

	public HashSet<Integer> getAllPagesIds() {
		HashSet<Integer> res = new HashSet<Integer>();
		for (Page p : pages) {
			res.add(p.getId());
		}
		return res;
	}

	public Iterator evaluateSelect(HashSet<Integer> pagesIds, SQLTerm[] sqlTerms, String[] arrayOperators)
			throws DBAppException {
		Vector<Tuple> res = new Vector<Tuple>();
		for (Integer i : pagesIds) {
			Page p = pages.get(i);
			p.readData();
			for (Tuple t : p.getData()) {
				if (evaluateSelectOnTuple(t, sqlTerms, arrayOperators))
					res.add(t);
			}
			p.setData(null);
		}
		return res.iterator();
	}

	private boolean evaluateSelectOnTuple(Tuple t, SQLTerm[] sqlTerms, String[] arrayOperators) throws DBAppException {
		boolean[] evals = new boolean[sqlTerms.length];
		for (int i = 0; i < sqlTerms.length; i++)
			evals[i] = isTupleInTerm(t, sqlTerms[i]);
		return evaluateExpression(evals, arrayOperators);
	}

	public static boolean evaluateExpression(boolean[] val, String[] arrayOperators) {
		ArrayList<Boolean> oldVal = new ArrayList<>();
		ArrayList<String> oldOps = new ArrayList<>();
		for (boolean x : val)
			oldVal.add(x);
		for (String s : arrayOperators)
			oldOps.add(s);

		ArrayList<Boolean> newVal = new ArrayList<>();
		ArrayList<String> newOps = new ArrayList<>();
		boolean prev = oldVal.get(0);
		for (int i = 0; i < oldOps.size(); i++) {
			if (oldOps.get(i).equals("AND")) {
				prev = prev & oldVal.get(i + 1);
			} else {
				newVal.add(prev);
				prev = oldVal.get(i + 1);
				newOps.add(oldOps.get(i));
			}
		}
		newVal.add(prev);

		oldVal = newVal;
		oldOps = newOps;
		newVal = new ArrayList<>();
		newOps = new ArrayList<>();

		prev = oldVal.get(0);
		for (int i = 0; i < oldOps.size(); i++) {
			if (oldOps.get(i).equals("OR")) {
				prev = prev | oldVal.get(i + 1);
			} else {
				newVal.add(prev);
				prev = oldVal.get(i + 1);
				newOps.add(oldOps.get(i));
			}
		}
		newVal.add(prev);

		oldVal = newVal;
		oldOps = newOps;
		newVal = new ArrayList<>();
		newOps = new ArrayList<>();

		prev = oldVal.get(0);
		for (int i = 0; i < oldOps.size(); i++) {
			if (oldOps.get(i).equals("XOR")) {
				prev = prev ^ oldVal.get(i + 1);
			} else {
				newVal.add(prev);
				prev = oldVal.get(i + 1);
				newOps.add(oldOps.get(i));
			}
		}

		return prev;
	}

	private boolean evaluateSelectOnTupleNoPrescedence(Tuple t, SQLTerm[] sqlTerms, String[] arrayOperators, int i)
			throws DBAppException {
		if (i == arrayOperators.length) {
			return isTupleInTerm(t, sqlTerms[i]);
		} else {
			switch (arrayOperators[i]) {
			case "AND":
				return isTupleInTerm(t, sqlTerms[i])
						&& evaluateSelectOnTupleNoPrescedence(t, sqlTerms, arrayOperators, i + 1);
			case "OR":
				return isTupleInTerm(t, sqlTerms[i])
						|| evaluateSelectOnTupleNoPrescedence(t, sqlTerms, arrayOperators, i + 1);
			case "XOR":
				return isTupleInTerm(t, sqlTerms[i])
						^ evaluateSelectOnTupleNoPrescedence(t, sqlTerms, arrayOperators, i + 1);
			default:
				throw new DBAppException("wrong passed sql operator");
			}
		}
	}

	private boolean isTupleInTerm(Tuple t, SQLTerm sqlTerm) throws DBAppException {
		int index = 0;
		for (int i = 0; i < columns.size(); i++) {
			if (columns.get(i).sameColumn(sqlTerm))
				index = i;
		}
		Comparable tupleValue = (Comparable) t.getData().get(index);
		return sqlTerm.checkSameValue(tupleValue);
	}

	public SQLTerm[] createSqlTerms(Hashtable<String, Object> colNameValue) {
		SQLTerm sqlTerms[] = new SQLTerm[colNameValue.size()];
		int i = 0;
		for (Map.Entry<String, Object> val : colNameValue.entrySet()) {
			SQLTerm st = new SQLTerm(this.tableName, val.getKey(), "=", val.getValue());
			sqlTerms[i] = st;
			i++;
		}
		return sqlTerms;
	}

	private Page getPageWithId(Integer i) {
		for (Page p : pages)
			if (p.getId() == i)
				return p;
		return null;
	}

}
