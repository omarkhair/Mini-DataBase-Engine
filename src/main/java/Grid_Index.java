import java.io.Serializable;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Vector;

@SuppressWarnings("serial")
public class Grid_Index implements Serializable{
	private String tableName;
	private int id;
	private int cell_id;
	private int dim;
	private Object[] arr;
	private int maximumNoEntries;
	private Vector<Column> columns;
	private String path;

	public Grid_Index(int n, int maximumNoEntries, Vector<Column> columns) {
		this.dim = n;
		this.maximumNoEntries = maximumNoEntries;
		arr = createArray(n);
		cell_id = 0;
		this.columns = columns;
		path = "src/main/resources/data/"+ tableName +"/index"+id+"/index"+id+".ser";
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
	}

}
