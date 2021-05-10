import java.io.Serializable;
import java.util.Vector;

@SuppressWarnings("serial")
public class Cell implements Serializable{
	private int id;
	private String tableName;
	private int index_id;
	private int lastBucket_id;
	private Vector<Bucket> buckets;
	private int maximumNoEntries;

	public Cell(int maximumNoEntries,int id,String tableName,int index_id){
		this.maximumNoEntries = maximumNoEntries;
		this.id = id;
		this.tableName = tableName;
		this.index_id = index_id;
		buckets = new Vector<>();
		lastBucket_id = 0;
	}
	public Vector<Bucket> getBuckets() {
		return buckets;
	}

	public void setBuckets(Vector<Bucket> buckets) {
		this.buckets = buckets;
	}

	public void insertEntry(BucketEntry entry) throws DBAppException {
		if(buckets.size() == 0){
			buckets.add(new Bucket(tableName,lastBucket_id++,index_id,id));
			buckets.get(0).insertBucketEntry(entry);
			return;
		}
		Bucket lastBucket = buckets.get(buckets.size()-1);
		if(lastBucket.getEntries().size() == maximumNoEntries){
			lastBucket = new Bucket(tableName,lastBucket_id++,index_id,id);
			buckets.add(lastBucket);
		}
		lastBucket.insertBucketEntry(entry);
	}


}
