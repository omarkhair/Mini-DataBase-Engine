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
            throw new DBAppException("");
        }
    }
    public static long dateToDays(String date){
        String[] arr = date.split("-");
        long ans = Long.parseLong(arr[0]) * 365;
        ans += Long.parseLong(arr[1]) * 30;
        ans += Long.parseLong(arr[2]);
        return ans;
    }
    public static int getIndexOf(String key, Vector<Column> columns) {
        for(int i = 0; i<columns.size();i++) {
            Column c = columns.get(i);
            if(c.getName().equals(key))
                return i;
        }
        return -1;
    }

}
