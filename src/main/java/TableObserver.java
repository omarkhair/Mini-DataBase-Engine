
public interface TableObserver {

	public void notifyInsert(int pageId, Tuple t) throws DBAppException;
	public void notifyDelete(int pageId, Tuple t) throws DBAppException;
}
