import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Vector;

public class Page implements Serializable, Comparable {
    static int pageCounter = 0;

    private int id;
    private String path; // src/pages/page1.ser
    private String keyType;
    private String minValue;
    private String maxValue;
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
}
