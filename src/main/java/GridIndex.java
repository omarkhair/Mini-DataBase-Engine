import java.io.File;
import java.io.Serializable;
import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Hashtable;
import java.util.Scanner;
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
	private String path;
	private Object[][] range;

	public GridIndex(String tableName, int id, int maximumNoEntries, Vector<Column> columns) throws DBAppException {
		this.tableName = tableName;
		this.id = id;
		this.dim = columns.size();
		this.maximumNoEntries = maximumNoEntries;
		arr = createArray(dim);
		cell_id = 0;
		this.columns = columns;
		path = "src/main/resources/data/" + tableName + "/index" + id + "/index" + id + ".ser";
		String dir = "src/main/resources/data/" + tableName + "/index" + id;
		File file = new File(dir);
		file.mkdir();
		// create division ranges for columns
		createRangeArray();
		Serializer.serialize(path, this);
		for(int i = 0;i<dim;i++) {
			for(int j = 0 ; j<11;j++) {
				System.out.print(range[i][j]+" ");
			}
			System.out.println();
		}
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
				int divSize = (max - min + 9) / 10;
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

	@SuppressWarnings({ "unchecked", "unused" })
	public Cell access(int[] indecies) {
		if (indecies.length != dim)
			return null;
		Object res = arr;
		for (int i : indecies) {
			res = ((Object[]) res)[i];
		}
		return (Cell) res;
	}

	public void insertEntry(BucketEntry be) throws DBAppException {
		int[] cellIdx = new int[dim];
		for (int i = 0; i < dim; i++) {
			Comparable key = (Comparable) be.getData().get(i);
			if (columns.get(i).getDataType().equals("java.util.Date")) {
				SimpleDateFormat s = new SimpleDateFormat("yyyy-MM-dd");
				key = Utilities.dateToDays(s.format((Date) key));
			}
			Object[] limits = range[i];
			for (int j = 1; j < 11; j++) {
				if (key.compareTo(limits[j]) <= 0) {
					cellIdx[i] = j - 1;
					break;
				}
			}
		}

		Cell targetCell = access(cellIdx);
		targetCell.insertEntry(be);
	}
}
