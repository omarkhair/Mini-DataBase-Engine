import java.math.BigInteger;
import java.net.DatagramPacket;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Vector;

public class Utilities {
	public static Comparable parseType(String val, String dataType) throws DBAppException {
		try {
			if (dataType.equals("java.lang.Integer")) {
				return Integer.parseInt(val);
			}
			if (dataType.equals("java.lang.Double")) {
				return Double.parseDouble(val);
			}
			if (dataType.equals("java.util.Date")) {
				return new SimpleDateFormat("yyyy-MM-dd").parse(val);
			}
			return val;
		} catch (ParseException i) {
			throw new DBAppException("Cannot parse value to passed type");
		}
	}

	public static long dateToDays(String date) {
		String[] arr = date.split("-");
		long ans = Long.parseLong(arr[0]) * 365;
		ans += Long.parseLong(arr[1]) * 30;
		ans += Long.parseLong(arr[2]);
		return ans;
	}

	public static BigInteger stringToInteger(String s) {
		BigInteger base = new BigInteger("1");
		BigInteger res = new BigInteger("0");
		for (int i = s.length() - 1; i >= 0; i--) {
			int unicode = (int) s.charAt(i);
			res = res.add(base.multiply(BigInteger.valueOf(unicode)));
			base = base.multiply(BigInteger.valueOf(256));

		}
		return res;
	}

	public static String BigIntegerToString(BigInteger b) {
		StringBuilder sb = new StringBuilder();
		BigInteger base = BigInteger.valueOf(256);
		while (!b.equals(BigInteger.valueOf(0))) {
			int unicode = b.mod(base).intValue();
			char c = (char) unicode;
			sb.append(c);
			b = b.divide(base);
		}
		sb.reverse();
		return sb.toString();
	}

	public static String padding(String s, int newlength) {
		StringBuilder sb = new StringBuilder(s);
		while (newlength - s.length() != 0) {
			sb.append((char) 0);
			newlength--;
		}
		return sb.toString();
	}

	public static int getIndexOf(String key, Vector<Column> columns) {
		for (int i = 0; i < columns.size(); i++) {
			Column c = columns.get(i);
			if (c.getName().equals(key))
				return i;
		}
		return -1;
	}

	public static Vector<Vector<Integer>> permutations(int dim, int min, int max) {
		Vector<Vector<Integer>> res = new Vector<Vector<Integer>>();
		if (dim == 0) {
			res.add(new Vector<Integer>());
			return res;
		}
		Vector<Vector<Integer>> prev = permutations(dim - 1, min, max);
		for (Vector<Integer> vec : prev)
			for (int i = min; i <= max; i++) {
				Vector<Integer> k = ((Vector<Integer>) vec.clone());
				k.add(i);
				res.add(k);
			}
		return res;
	}

	public static void main(String[] args) {
		String s = "ahdhgds12134877sjkf.nxdknzjksdj%^%5212323";
		BigInteger res = stringToInteger(s);
		String b = BigIntegerToString(res);
		String newstring = padding("Omar", 6);
		System.out.println(newstring);
	}

}
