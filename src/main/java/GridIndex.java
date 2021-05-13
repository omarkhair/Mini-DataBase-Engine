import java.io.Serializable;
import java.text.ParseException;
import java.util.Date;
import java.util.Hashtable;
import java.util.Scanner;
import java.util.Vector;

@SuppressWarnings("serial")
public class GridIndex implements Serializable{
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
		path = "src/main/resources/data/"+ tableName +"/index"+id+"/index"+id+".ser";
		// create division ranges for columns
		createRangeArray();
		Serializer.serialize(path, this);
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
		for(Column col: columns){
			if(col.getDataType().equals("java.lang.Integer")){
				int min = (Integer) col.getMin();
				int max = (Integer) col.getMax();
				int divSize = (max - min + 9) / 10;
				range[i][0] = min;
				for(int j = 1;j<11;j++){
					range[i][j] = (Integer) range [i][j-1] + divSize;
					range[i][j] = Math.min((Integer) range[i][j], max);
				}
				range[i][10] = max;
			}
			else if(col.getDataType().equals("java.lang.Double")){
				double min = (Double) col.getMin();
				double max = (Double) col.getMax();
				double divSize = (max - min) / 10;
				range[i][0] = min;
				for(int j = 1;j<11;j++){
					range[i][j] = (Double) range [i][j-1] + divSize;
					range[i][j] = Math.min((Double) range[i][j], max);
				}
				range[i][10] = max;
			}
			else if(col.getDataType().equals("java.util.Date")){
				long min = Utilities.dateToDays(col.getMinString());
				long max = Utilities.dateToDays(col.getMaxString());
				long divSize = (max - min + 9) / 10;
				range[i][0] = min;
				for(int j = 1;j<11;j++){
					range[i][j] = (Long) range [i][j-1] + divSize;
					range[i][j] = Math.min((Long) range[i][j], max);
				}
				range[i][10] = max;
			} else{
				String min = col.getMinString();
				String max = col.getMaxString();
				// TODO: find a way to convert string ranges

			}
			i++;
		}
	}

	private Object[] createArray(int dim) {
		if(dim==1) {
			Cell[] res = new Cell[10];
			for(int i = 0; i < 10; i++) {
				res[i] = new Cell(maximumNoEntries, cell_id++,tableName,id);
			}
			return res;
		}
		else {
			Object[] res = new Object[10];
			for(int i = 0; i<10;i++) {
				res[i] = createArray(dim-1);
			}
			return res;
		}

	}

	@SuppressWarnings({ "unchecked", "unused" })
	public Cell access(int[] indecies) {
		if(indecies.length != dim)
			return null;
		Object res = arr;
		for(int i : indecies) {
			res = ((Object[]) res)[i];
		}
		return (Cell)res;
	}

	public void insertEntry(BucketEntry be) throws DBAppException {
		int[] cellIdx = new int[dim];
		for(int i=0;i<dim;i++){
			Comparable key = (Comparable) be.getData().get(i);
			if(columns.get(i).getDataType().equals("java.util.Date")) {
				key = Utilities.dateToDays(((Date) key).toString());  //TODO: verify string format
			}
			// TODO: handle string comparison in divisions
			Object[] limits = range[i];
			for(int j = 1; j<11;j++){
				if(key.compareTo(limits[j])<=0){
					cellIdx[i] = j - 1;
					break;
				}
			}
		}

		Cell targetCell = access(cellIdx);
		targetCell.insertEntry(be);
	}
	public static void main(String[] args) {
//		Grid_Index a = new Grid_Index(5, 50);
//		Cell b = a.access(new int[]{1,2,3,4,5});
//		Vector<Object> data = new Vector<>();
//		data.add(5);
//		data.add(6);
//		BucketEntry e1 = new BucketEntry(data, 1, 7);
//		b.insertEntry(e1);
//		Cell c = a.access(new int[]{1,2,3,4,5});
//		BucketEntry e2 = c.getBuckets().get(0).getEntries().get(0);
//		System.out.println(e2);
		Scanner sc = new Scanner(System.in);
		String date1 = sc.next();
		String date2 =sc.next();
		System.out.println(Utilities.dateToDays(date2) - Utilities.dateToDays(date1));
	}


}
