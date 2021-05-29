
public interface TableObserver {

	public void notifyInsert(int pageId, Tuple t);
	public void notifyDelete(int pageId, Tuple t);
}
