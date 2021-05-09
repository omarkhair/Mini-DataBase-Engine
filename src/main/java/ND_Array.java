import java.io.Serializable;
import java.util.Vector;

@SuppressWarnings("serial")
public class ND_Array implements Serializable{

	private int n;
	private Object[] arr;
	
	public ND_Array(int n) {
		this.n = n;
		arr = createArray(n);
	}

	private Object[] createArray(int dim) {
		if(dim==1) {
			Vector<Bucket>[] res = new Vector[10];
			for(int i = 0; i < 10; i++) {
				res[i] = new Vector<Bucket>();
			}
			return res;
		}
		else {
			Object[] res = new Object[10];
			for(int i = 0; i<10;i++) {
				res[i] = createArray(dim-1);
			}
			return res;
		}
		
	}
	
	@SuppressWarnings({ "unchecked", "unused" })
	public Vector<Bucket> access(int[] indecies) {
		if(indecies.length != n)
			return null;
		Object res = arr;
		for(int i : indecies) {
			res = ((Object[]) res)[i];
		}
		return (Vector<Bucket>)res;
	}
	
	public static void main(String[] args) {
		ND_Array a = new ND_Array(2);
		Vector<Bucket> b = a.access(new int[]{1,2});
		b.add(new Bucket());
	}
	
}
