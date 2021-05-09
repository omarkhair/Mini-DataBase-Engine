import java.io.Serializable;
import java.util.Vector;

@SuppressWarnings("serial")
public class BucketEntry implements Serializable {

	private Vector<Object> data;
	private int pageId;
	private int entryIndex;
	
	public BucketEntry(Vector<Object> data, int pageId, int entryIndex) {
		this.setData(data);
		this.setPageId(pageId);
		this.setEntryIndex(entryIndex);
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

	public int getEntryIndex() {
		return entryIndex;
	}

	public void setEntryIndex(int entryIndex) {
		this.entryIndex = entryIndex;
	}
	
}
