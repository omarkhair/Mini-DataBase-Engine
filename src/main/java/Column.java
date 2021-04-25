import java.io.Serializable;
import java.util.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class Column implements Serializable {
    private String name;
    private String dataType;
    private boolean isPrimary;
    private boolean isIndexed;
    private String min;
    private String max;

    public Column(String name, String dataType, boolean isPrimary, boolean isIndexed, String min, String max){
        this.name = name;
        this.dataType = dataType;
        this.isPrimary = isPrimary;
        this.isIndexed = isIndexed;
        this.min = min;
        this.max = max;
    }
    public String getName() {
        return name;
    }

    public String getDataType() {
        return dataType;
    }

    public boolean isPrimary() {
        return isPrimary;
    }

    public boolean isIndexed() {
        return isIndexed;
    }

    public void setIndexed(boolean indexed) {
        isIndexed = indexed;
    }

    @SuppressWarnings("rawtypes")
	public Comparable getMin() throws ParseException {
        return parseType(min);
    }

    @SuppressWarnings("rawtypes")
    public Comparable getMax() throws ParseException {
        return parseType(max);
    }
    
    public Comparable parseType(String val) throws ParseException {
    	if(dataType.equals("java.lang.Integer")) {
    		return Integer.parseInt(val);
    	}
    	if(dataType.equals("java.lang.Double")) {
    		return Double.parseDouble(val);
    	}
    	if(dataType.equals("java.lang.Date")) {
    		return new SimpleDateFormat("YYYY-MM-DD").parse(val);  
    	}
    	return val;
    }

}
