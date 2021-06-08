import java.io.File;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.Vector;

@SuppressWarnings("serial")
public class GridIndex implements Serializable {
	private String tableName;
	private int id;
	private int cell_id;
	private int dim;
	private Object[] arr;
	private int maximumNoEntries;
	private Vector<Column> columns;
	private Object[][] range;

	public GridIndex(String tableName, int id, int maximumNoEntries, Vector<Column> columns) throws DBAppException {
		this.tableName = tableName;
		this.id = id;
		this.dim = columns.size();
		this.maximumNoEntries = maximumNoEntries;
		arr = createArray(dim);
		cell_id = 0;
		this.columns = columns;
		String dir = "src/main/resources/data/" + tableName + "/index" + id;
		File file = new File(dir);
		file.mkdir();
		// create division ranges for columns
		createRangeArray();
	}

	public Vector<Column> getColumns() {
		return columns;
	}

	public void setColumns(Vector<Column> columns) {
		this.columns = columns;
	}

	private void createRangeArray() throws DBAppException {
		range = new Object[dim][11];
		int i = 0;
		for (Column col : columns) {
			if (col.getDataType().equals("java.lang.Integer")) {
				int min = (Integer) col.getMin();
				int max = (Integer) col.getMax();
				int divSize =(max-min+9)/ 10;
				range[i][0] = min;
				for (int j = 1; j < 11; j++) {
					range[i][j] = (Integer) range[i][j - 1] + divSize;
					range[i][j] = Math.min((Integer) range[i][j], max);
				}
				range[i][10] = max;
			} else if (col.getDataType().equals("java.lang.Double")) {
				double min = (Double) col.getMin();
				double max = (Double) col.getMax();
				double divSize = (max - min) / 10;
				range[i][0] = min;
				for (int j = 1; j < 11; j++) {
					range[i][j] = (Double) range[i][j - 1] + divSize;
					range[i][j] = Math.min((Double) range[i][j], max);
				}
				range[i][10] = max;
			} else if (col.getDataType().equals("java.util.Date")) {
				long min = Utilities.dateToDays(col.getMinString());
				long max = Utilities.dateToDays(col.getMaxString());
				long divSize = (max - min + 9) / 10;
				range[i][0] = min;
				for (int j = 1; j < 11; j++) {
					range[i][j] = (Long) range[i][j - 1] + divSize;
					range[i][j] = Math.min((Long) range[i][j], max);
				}
				range[i][10] = max;
			} else {
				String min = col.getMinString();
				String max = col.getMaxString();
				if (min.length() > max.length()) {
					max = Utilities.padding(max, min.length());
				} else {
					min = Utilities.padding(min, max.length());
				}

				BigInteger minValue = Utilities.stringToInteger(min);
				BigInteger maxValue = Utilities.stringToInteger(max);
				BigInteger divSize = maxValue.subtract(minValue).add(BigInteger.valueOf(9))
						.divide(BigInteger.valueOf(10));
				range[i][0] = min;
				for (int j = 1; j < 11; j++) {
					minValue = minValue.add(divSize);
					if (minValue.compareTo(maxValue) > 0) {
						minValue = maxValue.add(BigInteger.ZERO);
					}
					range[i][j] = Utilities.BigIntegerToString(minValue);
				}
				range[i][10] = max;

			}
			i++;
		}
	}

	private Object[] createArray(int dim) {
		if (dim == 1) {
			Cell[] res = new Cell[10];
			for (int i = 0; i < 10; i++) {
				res[i] = new Cell(maximumNoEntries, cell_id++, tableName, id);
			}
			return res;
		} else {
			Object[] res = new Object[10];
			for (int i = 0; i < 10; i++) {
				res[i] = createArray(dim - 1);
			}
			return res;
		}

	}

	@SuppressWarnings({ "unused" })
	public Cell access(int[] indecies) {
		if (indecies.length != dim) {
			
			return null;
		}
		Object res = arr;
		for (int i : indecies) {
			res = ((Object[]) res)[i];
		}
		return (Cell) res;
	}

	@SuppressWarnings("unchecked")
	private int[] getIdxOfEntry(BucketEntry be) {
		int[] cellIdx = new int[dim];
		for (int i = 0; i < dim; i++) {
			@SuppressWarnings("rawtypes")
			Comparable key = (Comparable) be.getData().get(i);
			if (columns.get(i).getDataType().equals("java.util.Date")) {
				SimpleDateFormat s = new SimpleDateFormat("yyyy-MM-dd");
				key = Utilities.dateToDays(s.format((Date) key));
			}
			Object[] limits = range[i];
			if (key.equals(limits[10])) {
				cellIdx[i] = 9;
			}
			for (int j = 1; j < 11; j++) {
				if (key.compareTo(limits[j]) < 0) {
					cellIdx[i] = j - 1;
					break;
				}
			}
		}
		return cellIdx;
	}

	public void insertEntry(BucketEntry be) throws DBAppException {
		int[] cellIdx = getIdxOfEntry(be);
		Cell targetCell = access(cellIdx);
		targetCell.insertEntry(be);
	}

	public void deleteEntry(BucketEntry be) throws DBAppException {
		int[] cellIdx = getIdxOfEntry(be);
		Cell targetCell = access(cellIdx);
		targetCell.deleteEntry(be);
	}

	private boolean increment(int[] tmp, int i) {
		if (i == tmp.length + 1)
			return false;
		tmp[tmp.length - i]++;
		if (tmp[tmp.length - i] == 10) {
			tmp[tmp.length - i] = 0;
			return increment(tmp, i + 1);
		}
		return true;
	}

	private String getRanges(int[] tmp) {
		String res = "";
		for (int i = 0; i < dim; i++) {
			res += columns.get(i).getName() + " has range of: ";
			res += range[i][tmp[i]] + "->" + range[i][tmp[i] + 1] + "\n";
		}
		return res + "\n";
	}

	public String toString() {
		String res = "This is Grid Index with id " + id + " on table " + tableName + "\n";
		int[] tmp = new int[dim];
		do {
			res += "--------------------------------------------------------------------------------------\n";
			res += "Cell ranges are: \n";
			res += getRanges(tmp);
			res += access(tmp).toString();
		} while (increment(tmp, 1));
		return res;
	}

	public TreeSet<Integer> getPagesIds(Vector<SQLTerm> sqlTerms) throws DBAppException {
		Vector<String> columnNames = new Vector<String>();
		for (Column c : columns) {
			columnNames.add(c.getName());
		}
		Vector<Vector<Integer>> cellIndecies = getCellIndecies(dim, sqlTerms);
		TreeSet<Integer> pages = new TreeSet<Integer>();
		for (Vector<Integer> dimensions : cellIndecies) {
			int[] dims = Utilities.fromVectortoArr(dimensions);
			Cell target = access(dims);
			pages.addAll(target.getPagesOfCell(sqlTerms, columnNames));
		}

		return pages;
	}

	private Vector<Vector<Integer>> getCellIndecies(int i, Vector<SQLTerm> sqlTerms) {
		Vector<Vector<Integer>> res = new Vector<>();
		if (i == 0) {
			res.add(new Vector<>());
			return res;
		}
		Vector<Vector<Integer>> prev = getCellIndecies(i - 1, sqlTerms);
		Vector<Integer> validCellsIdx = getValidCells(i - 1, sqlTerms);
		for (Vector<Integer> prevIdx : prev) {
			for (int cellIdx : validCellsIdx) {
				Vector<Integer> newIdx = (Vector<Integer>) prevIdx.clone();
				newIdx.add(cellIdx);
				res.add(newIdx);
			}
		}
		return res;
	}

	private Vector<Integer> getValidCells(int i, Vector<SQLTerm> sqlTerms) {
		Vector<Integer> res = new Vector<>();
		for (int cell = 0; cell < 9; cell++) {
			if (cellSatisfiesSQLTerms(i, cell, sqlTerms))
				res.add(cell);
		}
		return res;
	}

	private boolean cellSatisfiesSQLTerms(int colIdx, int cellIdx, Vector<SQLTerm> sqlTerms) {
		Object l = range[colIdx][cellIdx];
		Object r = range[colIdx][cellIdx + 1];
		for (SQLTerm term : sqlTerms) {
			if (term.get_strColumnName().equals(columns.get(colIdx).getName())) {
				Comparable val = (Comparable) term.get_objValue();
				if (columns.get(colIdx).getDataType().equals("java.util.Date"))
					val = Utilities.dateObjectToDays((Date) val);
				boolean matches = true;
				if (term.get_strOperator().equals("=")) {
					if (cellIdx == 9)
						matches = val.compareTo(l) >= 0 && val.compareTo(r) <= 0;
					else
						matches = val.compareTo(l) >= 0 && val.compareTo(r) < 0;
				} else if (term.get_strOperator().equals(">") || term.get_strOperator().equals(">=")) {
					if (cellIdx == 9)
						matches = val.compareTo(r) <= 0;
					else
						matches = val.compareTo(r) < 0;
				} else if (term.get_strOperator().equals("<") || term.get_strOperator().equals("<=")) {
					matches = val.compareTo(l) >= 0;
				}
				if (!matches)
					return false;
			}
		}
		return true;
	}
	
	public int getPageWithClustering(Object clusteringKey) throws DBAppException {
		int[] cellIdx = new int[dim];
		for (int i = 0; i < dim; i++) {
			@SuppressWarnings("rawtypes")
			Comparable key = (Comparable) clusteringKey;
			if (columns.get(i).getDataType().equals("java.util.Date")) {
				SimpleDateFormat s = new SimpleDateFormat("yyyy-MM-dd");
				key = Utilities.dateToDays(s.format((Date) key));
			}
			Object[] limits = range[i];
			if (key.equals(limits[10])) {
				cellIdx[i] = 9;
			}
			for (int j = 1; j < 11; j++) {
				if (key.compareTo(limits[j]) < 0) {
					cellIdx[i] = j - 1;
					break;
				}
			}
		}
		Cell target = access(cellIdx); 
		return target.getPageFromCell(clusteringKey);
	}
	
	public int getDim() {
		return dim;
	}

	public void setDim(int dim) {
		this.dim = dim;
	}
	
	public int[] getCellIdUsingClustKey(Object clusteringKey) {
		int[] cellIdx = new int[dim];
		for (int i = 0; i < dim; i++) {
			@SuppressWarnings("rawtypes")
			Comparable key = (Comparable) clusteringKey;
			if (columns.get(i).getDataType().equals("java.util.Date")) {
				SimpleDateFormat s = new SimpleDateFormat("yyyy-MM-dd");
				key = Utilities.dateToDays(s.format((Date) key));
			}
			Object[] limits = range[i];
			if (key.equals(limits[10])) {
				cellIdx[i] = 9;
			}
			for (int j = 1; j < 11; j++) {
				if (key.compareTo(limits[j]) < 0) {
					cellIdx[i] = j - 1;
					break;
				}
			}
		}
		return cellIdx;
	}
	
	public int getPageforInsert(Object clusteringKey) throws DBAppException {
		int[] dim = getCellIdUsingClustKey(clusteringKey);
		Cell target = access(dim);
		if (target.getBuckets().size() == 0) {
			return -1;
		} else {
			Comparable maxSofare = (Comparable) range[0][dim[0]];
			int pageId = -1;
			for (Bucket b : target.getBuckets()) {
				b.readData();
				for (BucketEntry be : b.getEntries()) {
					Comparable key = (Comparable) be.getClusteringKeyValue();
					Comparable clust = (Comparable) clusteringKey;
					if (key.compareTo(clust) < 0 && key.compareTo(maxSofare) >= 0) {
						maxSofare = key;
						pageId = be.getPageId();
					}
				}
			}
			return pageId;
		}

	}
	
	public int getPageWithClustering(String clusteringKey) throws DBAppException {
		Cell target = access(getCellIdUsingClustKey(clusteringKey));
		return target.getPageFromCell(clusteringKey);
	}

}
