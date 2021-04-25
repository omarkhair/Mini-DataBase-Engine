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
		pages = new Vector<>();
		this.pageMaxRows = pageMaxRows;
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
			data.add(colNameValue.getOrDefault(c.getName(),null));
		}
		Tuple t = new Tuple(data);
		insertRecordHelper(t);
	}

	@SuppressWarnings("unchecked")
	public void insertRecordHelper(Tuple t) throws DBAppException{
		if (pages.size() == 0) {
			Page page = new Page(tableName, clusteringKeyType);
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
			Page page = new Page(tableName, clusteringKeyType);
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
							&& ((Comparable) key).compareTo(pages.get(mid + 1).getMinValue()) <= 0)
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

	public void updateRecord() {

	}

	public void deleteRecords(Hashtable<String, Object> colNameValue) throws DBAppException {
		Vector<Page> toBeRemoved = new Vector<>();
		for (Page p : pages) {
			p.deleteRecords(colNameValue, columns);
			if(p.getNumberOfTuples()==0) {
				toBeRemoved.add(p);
				p.deletePageFromDisk();
			}
		}
		pages.removeAll(toBeRemoved);
	}

	public void verifyInsertion(Hashtable<String, Object> colNameValue) throws DBAppException{
		if(colNameValue.containsKey(clusteringKey))
			throw new DBAppException("inserted record does not have a clustering key");
		for(Map.Entry<String, Object> ent: colNameValue.entrySet()) {
			Column c = getColumn(ent.getKey());
			if(c==null) 
				throw new DBAppException("there is no column named " + ent.getKey());
			Object val = ent.getValue();
			if(val instanceof String) {
				if(!c.getDataType().equals("java.lang.String"))
					throw new DBAppException("mismatching data type of column "+ent.getKey());
				String val2 = (String) val;
				try {
					if(val2.compareTo(((String)c.getMax()))>0 || val2.compareTo(((String)c.getMin()))<0) {
						throw new DBAppException("out of bounds value for column "+ent.getKey());
					}
				} catch (ParseException e) {
					throw new DBAppException();
				}
			}
			else if(val instanceof Integer) {
				if(!c.getDataType().equals("java.lang.Integer"))
					throw new DBAppException("mismatching data type of column "+ent.getKey());
				Integer val2 = (Integer) val;
				try {
					if(val2.compareTo(((Integer)c.getMax()))>0 || val2.compareTo(((Integer)c.getMin()))<0) {
						throw new DBAppException("out of bounds value for column "+ent.getKey());
					}
				} catch (ParseException e) {
					throw new DBAppException();
				}
			}
			else if(val instanceof Double) {
				if(!c.getDataType().equals("java.lang.Double"))
					throw new DBAppException("mismatching data type of column "+ent.getKey());
				Double val2 = (Double) val;
				try {
					if(val2.compareTo(((Double)c.getMax()))>0 || val2.compareTo(((Double)c.getMin()))<0) {
						throw new DBAppException("out of bounds value for column "+ent.getKey());
					}
				} catch (ParseException e) {
					throw new DBAppException();
				}
			}
			else if(val instanceof Date) {
				if(!c.getDataType().equals("java.lang.Date"))
					throw new DBAppException("mismatching data type of column "+ent.getKey());
				Date val2 = (Date) val;
				try {
					if(val2.compareTo(((Date)c.getMax()))>0 || val2.compareTo(((Date)c.getMin()))<0) {
						throw new DBAppException("out of bounds value for column "+ent.getKey());
					}
				} catch (ParseException e) {
					throw new DBAppException("invalid date format");
				}
			}
			else 
				throw new DBAppException("unsupported data type for column "+ent.getKey());
		}
	}
	
	public Column getColumn(String columnName) {
		for(Column c: columns) {
			if(c.getName().equals(columnName))
				return c;
		}
		return null;
	}

	public boolean verifyDeletion(Hashtable<String, Object> colNameValue) throws DBAppException{
		for(Map.Entry<String, Object> ent: colNameValue.entrySet()) {
			Column c = getColumn(ent.getKey());
			if(c==null) 
				throw new DBAppException("there is no column named " + ent.getKey());
			Object val = ent.getValue();
			if(val instanceof String) {
				if(!c.getDataType().equals("java.lang.String"))
					throw new DBAppException("mismatching data type of column "+ent.getKey());
				String val2 = (String) val;
				try {
					if(val2.compareTo(((String)c.getMax()))>0 || val2.compareTo(((String)c.getMin()))<0) {
						return false;
					}
				} catch (ParseException e) {
					throw new DBAppException();
				}
			}
			else if(val instanceof Integer) {
				if(!c.getDataType().equals("java.lang.Integer"))
					throw new DBAppException("mismatching data type of column "+ent.getKey());
				Integer val2 = (Integer) val;
				try {
					if(val2.compareTo(((Integer)c.getMax()))>0 || val2.compareTo(((Integer)c.getMin()))<0) {
						return false;
					}
				} catch (ParseException e) {
					throw new DBAppException();
				}
			}
			else if(val instanceof Double) {
				if(!c.getDataType().equals("java.lang.Double"))
					throw new DBAppException("mismatching data type of column "+ent.getKey());
				Double val2 = (Double) val;
				try {
					if(val2.compareTo(((Double)c.getMax()))>0 || val2.compareTo(((Double)c.getMin()))<0) {
						return false;
					}
				} catch (ParseException e) {
					throw new DBAppException();
				}
			}
			else if(val instanceof Date) {
				if(!c.getDataType().equals("java.lang.Date"))
					throw new DBAppException("mismatching data type of column "+ent.getKey());
				Date val2 = (Date) val;
				try {
					if(val2.compareTo(((Date)c.getMax()))>0 || val2.compareTo(((Date)c.getMin()))<0) {
						return false;
					}
				} catch (ParseException e) {
					throw new DBAppException("invalid date format");
				}
			}
			else 
				throw new DBAppException("unsupported data type for column "+ent.getKey());
		}
		return true;
	}
}
