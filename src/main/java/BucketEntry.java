import java.io.Serializable;
import java.util.Vector;

@SuppressWarnings("serial")
public class BucketEntry implements Serializable {

	private Vector<Object> data;
	private int pageId;

	public BucketEntry(Vector<Object> data, int pageId, int entryIndex) {
		this.setData(data);
		this.setPageId(pageId);
	}

	public Vector<Object> getData() {
		return data;
	}

	public void setData(Vector<Object> data) {
		this.data = data;
	}

	public int getPageId() {
		return pageId;
	}

	public void setPageId(int pageId) {
		this.pageId = pageId;
	}

	public String toString(){
		return data.toString() + " in page with ID " + pageId;
	}
	
}
