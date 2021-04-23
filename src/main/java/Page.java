import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Vector;

public class Page implements Serializable, Comparable {
    static int pageCounter = 0;

    private int id;
    private String path; // src/pages/page1.ser
    private String keyType;
    private Object minValue;
    private Object maxValue;
    private String tableName;
    private int numberOfTuples;
    transient private Vector<Tuple> data;
    public Page(String tableName, String keyType){
        this.tableName = tableName;
        this.keyType = keyType;
        id = pageCounter++;
        path = "src/tables/"+ tableName +"/page"+id+".ser";
        data = new Vector<Tuple>();
        numberOfTuples = 0;
    }

    public void writeData(){
        Serializer.serialize(path, data);
    }
    // important note: whenever you use readData(), set data Vector to null to
    // free it from memory
    public void readData(){
        data = (Vector<Tuple>) Serializer.deserilize(path);
    }


    @Override
    public int compareTo(Object o) {
        Page other = (Page) o;
        return this.id - other.id;
    }

    public static void main(String[] args) {
        Vector<Integer> v = new Vector<>();
        Collections.sort(v);
    }

    public void insertRecord(Tuple t) {
        readData();
        numberOfTuples++;
        data.add(t);
        Collections.sort(data);
        updateMinMax();
        writeData();

    }

    public void updateMinMax() {
        minValue = data.get(0).getData().get(0);
        maxValue = data.get(data.size()-1).getData().get(0);

    }

    public Object getMinValue(){
        return minValue;
    }

    public Object getMaxValue(){
        return maxValue;
    }
}
