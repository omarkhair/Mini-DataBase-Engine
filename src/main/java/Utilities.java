import java.net.DatagramPacket;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class Utilities {
    public static Comparable parseType(String val, String dataType) throws DBAppException {
        try {
            if (dataType.equals("java.lang.Integer")) {
                return Integer.parseInt(val);
            }
            if (dataType.equals("java.lang.Double")) {
                return Double.parseDouble(val);
            }
            if (dataType.equals("java.lang.Date")) {
                return new SimpleDateFormat("YYYY-MM-DD").parse(val);
            }
            return val;
        } catch (ParseException i) {
            throw new DBAppException("");
        }
    }
}
