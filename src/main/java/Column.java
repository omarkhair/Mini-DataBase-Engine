import java.io.Serializable;

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

    public String getMin() {
        return min;
    }

    public String getMax() {
        return max;
    }

}
