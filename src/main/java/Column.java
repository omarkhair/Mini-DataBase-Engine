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
	public Comparable getMin() throws DBAppException {
        return Utilities.parseType(min, dataType);
    }
    
    public String getMinString() {
    	return min;
    }

    @SuppressWarnings("rawtypes")
    public Comparable getMax() throws DBAppException {
        return Utilities.parseType(max, dataType);
    }


    public String getMaxString() {
    	return max;
    }

    public Comparable parseType(String val) throws ParseException {
    	if(dataType.equals("java.lang.Integer")) {
    		return Integer.parseInt(val);
    	}
    	if(dataType.equals("java.lang.Double")) {
    		return Double.parseDouble(val);
    	}
    	if(dataType.equals("java.util.Date")) {
    		return new SimpleDateFormat("yyyy-MM-dd").parse(val);
    	}
    	return val;
    }
    
    public String toString() {
    	String result = name+" : ";
    	switch(dataType) {
    	case "java.lang.Integer": result += "int"; break;
    	case "java.lang.Double": result += "double"; break;
    	case "java.util.Date": result += "date"; break;
    	default: result += "String";
    	}
    	return result;
    }

}
