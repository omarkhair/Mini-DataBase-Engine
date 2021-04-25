import java.io.Serializable;
import java.util.Vector;

public class Tuple implements Serializable, Comparable {
    private Vector<Object> data;
    public Tuple(Vector<Object>data){
        this.data = data;
    }

    public Vector<Object> getData() {
        return data;
    }

    public void setData(Vector<Object> data) {
        this.data = data;
    }
    
    public Object getIthVal(int i) {
    	return data.get(i);
    }

    @Override
    public int compareTo(Object o) {
        return this.compareTo(o);
    }
}
