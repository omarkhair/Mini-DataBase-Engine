import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.text.ParseException;
import java.util.*;

public class Table implements Serializable {
	private String tableName;
	private String clusteringKey;
	private String clusteringKeyType;
	private Vector<Column> columns;
	private Vector<Page> pages;
	private int pageMaxRows;
	private int lastPageId;
	private int lastIndexId;
	private Vector<Integer> indecies;

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

	public Vector<Integer> getIndecies() {
		return indecies;
	}

	public void setIndecies(Vector<Integer> indecies) {
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
			Page page = new Page(tableName, clusteringKeyType, lastPageId++);
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
					Page nextPage = new Page(tableName, clusteringKeyType, lastPageId++);
					currentPage.insertRecord(t);
					Tuple lastRecord = currentPage.removeLastRecord();
					pages.add(nextPage);
					nextPage.insertRecord(lastRecord);
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
				Page overflowPage = new Page(tableName, clusteringKeyType, lastPageId++);
				currentPage.insertRecord(t);
				for (int i = pageMaxRows; i > pageMaxRows / 2; i--) {
					overflowPage.insertRecord(currentPage.removeLastRecord());
				}
				pages.add(target + 1, overflowPage);

			}
		}
	}

	public void insertRecordHelper(Tuple t) throws DBAppException {
		if (pages.size() == 0) {
			Page page = new Page(tableName, clusteringKeyType, lastPageId++);
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
			Page page = new Page(tableName, clusteringKeyType, lastPageId++);
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
					throw new DBAppException("out of bounds value for column " + ent.getKey()+" value: "+val2.toString());
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
		for(int i=0;i<columnNames.length;i++)
			for(int j=i+1;j<columnNames.length;j++){
				if(columnNames[i].equals(columnNames[j]))
					throw new DBAppException("There are 2 columns with name "+ columnNames[i]);
			}
		Vector<Column> cols = new Vector<>();
		for (String col: columnNames){
			if(getColumn(col)==null)
				throw new DBAppException("There is no column named "+ col +" in table "+tableName);
			cols.add(getColumn(col));
		}
		return cols;
	}

	// this method fills the given grid index with all the entries found in the table
	public void populateIndex(GridIndex index) throws DBAppException {
		for(Page page: pages){
			page.readData();
			Vector<Tuple> data = page.getData();
			for(Tuple t: data){
				Vector<Object> entryData = new Vector<>();
				for(Column col: index.getColumns()){
					int idxInTuple = Utilities.getIndexOf(col.getName(), columns);
					entryData.add(t.getIthVal(idxInTuple));
				}
				BucketEntry be = new BucketEntry(entryData, page.getId() , t.getIthVal(0));
				index.insertEntry(be);
			}
			// free the page data that we are done working with from memory
			page.setData(null);
		}
	}
//	public void updatePagesRecord() {
//		for(Page p : pages) {
//			p.setId(pages.indexOf(p)); 
//		}
//	}
}
