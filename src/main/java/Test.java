import java.util.Arrays;
import java.util.Vector;

public class Test {
	
	@SuppressWarnings("unchecked")
	public static Vector<Vector<Integer>> permutations(int dim, int min, int max){
		Vector<Vector<Integer>> res = new Vector<Vector<Integer>>();
		if(dim == 0) {
			res.add(new Vector<Integer>());
			return res;
		}
		Vector<Vector<Integer>> prev = permutations(dim - 1, min, max);
		for(Vector<Integer> vec:prev)
			for(int i=min;i<=max;i++) {
				Vector<Integer> k =  ((Vector<Integer>)vec.clone());
				k.add(i);
				res.add(k);
			}
		return res;		
	}
	// Driver code
	public static void main(String args[]) {
		Vector<Vector<Integer>> res = permutations(2, 0, 9);
		for (Vector<Integer> x :res) {
			System.out.println(x.toString());
		}
	}
}
