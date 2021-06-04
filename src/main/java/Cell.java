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

	// this method assumes that all buckets are full except for the last one, it inserts directly to it
	public void insertEntry(BucketEntry entry) throws DBAppException {
		if(buckets.size() == 0){
			buckets.add(new Bucket(tableName,lastBucket_id++,index_id,id));
			buckets.get(0).insertBucketEntry(entry);
			return;
		}
		Bucket lastBucket = buckets.get(buckets.size()-1);
		if(lastBucket.getEntries().size() == maximumNoEntries){
			lastBucket.setEntries(null);
			lastBucket = new Bucket(tableName,lastBucket_id++,index_id,id);
			buckets.add(lastBucket);
		}
		lastBucket.insertBucketEntry(entry);
	}


	public void deleteEntry(BucketEntry entry) throws DBAppException {
		int n = buckets.size();
		for(int i=0;i<n;i++){
			Bucket bucket = buckets.get(i);
			if(bucket.removeBucketEntry(entry)){
				// the target entry was found and removed
				// We will maintain the property that only the last page can have empty space
				// by taking an element from the last Bucket and putting it in this bucket
				Bucket lastBucket = buckets.get(n-1);
				if(i < n-1){
					bucket.insertBucketEntry(lastBucket.removeLastEntry());
				}
				if(lastBucket.getNumberOfEntries() == 0){
					lastBucket.deleteBucketFromDisk();
					buckets.remove(n-1);
				}
				break;
			}
		}
	}
	public Vector<Integer> getPagesOfCell(Vector<SQLTerm> sqlTerms,Vector<String> columnNames) throws DBAppException {
		Vector<Integer>pages = new Vector<Integer>(); 
		for(Bucket b :buckets ) {
			b.readData();
			Vector<BucketEntry> bucketEntries = b.getEntries(); 
			for(BucketEntry be : bucketEntries) {
				if(be.checkMatching(sqlTerms,columnNames)) {
					pages.add(be.getPageId());
				}
			}
			
		}		
		return pages;
	}
	public String toString() {
		String res = "Cell id: "+id+"; has "+buckets.size()+" buckets \n";
		for(Bucket b:buckets) {
			res+= "--------------------------------------------\n";
			res += b.toString()+"\n";
		}
		if(buckets.size()==0)
			res+="---empty cell---\n\n";
		res += "\n";
		return res;
	}
}
