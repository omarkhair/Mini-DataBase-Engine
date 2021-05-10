import java.io.Serializable;
import java.util.Collections;
import java.util.Vector;

@SuppressWarnings("serial")
public class Bucket implements Serializable{

	private int id;
	private String path;
	private String tableName;
	private int index_id;
	private int cell_id;
	private int numberOfEntries;
	private transient Vector<BucketEntry> entries;

	public Bucket(String tableName,int id, int index_id,int cell_id) {
		this.tableName = tableName;
		this.id = id;
		this.index_id = index_id;
		this.cell_id = cell_id;
		path = "src/main/resources/data/"+ tableName +"/index"+index_id+"/cell"+cell_id+"/bucket"+id+".ser";
		numberOfEntries = 0;
		entries = new Vector<BucketEntry>();
		writeData();
		entries = null;
	}
	public void writeData(){
		Serializer.serialize(path, entries);
	}
	// important note: whenever you use readData(), set data Vector to null to
	// free it from memory
	public void readData() throws DBAppException{
		entries = (Vector<BucketEntry>) Serializer.deserilize(path);
	}
	public void insertBucketEntry(BucketEntry b) throws DBAppException{
		readData();
		numberOfEntries++;
		entries.add(b);
		writeData();
		entries = null;
	}

	public Vector<BucketEntry> getEntries() {
		return entries;
	}

	public void setEntries(Vector<BucketEntry> entries) {
		this.entries = entries;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}
}
