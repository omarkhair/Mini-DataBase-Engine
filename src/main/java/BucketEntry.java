import java.io.Serializable;
import java.util.Vector;

@SuppressWarnings("serial")
public class BucketEntry implements Serializable {
	// data contains values of columns of the gridIndex
	private Vector<Object> data;
	private int pageId;
	private Object clusteringKeyValue;

	public BucketEntry(Vector<Object> data, int pageId, Object clusteringKeyValue) {
		this.setData(data);
		this.setPageId(pageId);
		this.clusteringKeyValue = clusteringKeyValue;
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

	public Object getClusteringKeyValue() {
		return clusteringKeyValue;
	}

	public void setClusteringKeyValue(Object clusteringKeyValue) {
		this.clusteringKeyValue = clusteringKeyValue;
	}

	public String toString() {
		return data.toString() + " in page with ID " + pageId + " clustering key is " + clusteringKeyValue;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof BucketEntry))
			return false;
		return this.clusteringKeyValue.equals(((BucketEntry) o).clusteringKeyValue);
	}

	public boolean checkMatching(Vector<SQLTerm> sqlTerms,Vector<String> columnNames) throws DBAppException {
		boolean flag = true ; 
		loop:for(SQLTerm st : sqlTerms) {
			if(!flag) {
				break loop ; 
			}
			int idxOfCol = columnNames.indexOf(st.get_strColumnName()); 
			Comparable colDataInBucketEntry = (Comparable) this.data.get(idxOfCol);
			flag = st.checkSameValue(colDataInBucketEntry);
		}
		return flag ; 
	
}
}