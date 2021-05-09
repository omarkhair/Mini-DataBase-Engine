import java.io.Serializable;
import java.util.Vector;

@SuppressWarnings("serial")
public class Bucket implements Serializable{

	private Vector<BucketEntry> entries;
	
	public Bucket() {
		setEntries(new Vector<>());
	}

	public Vector<BucketEntry> getEntries() {
		return entries;
	}

	public void setEntries(Vector<BucketEntry> entries) {
		this.entries = entries;
	}
	
}
