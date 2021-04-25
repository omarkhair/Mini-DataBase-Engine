import java.io.File;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Map;
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
        writeData();
        numberOfTuples = 0;
    }

    public void writeData(){
        Serializer.serialize(path, data);
    }
    // important note: whenever you use readData(), set data Vector to null to
    // free it from memory
    public void readData() throws DBAppException{
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

    public void insertRecord(Tuple t) throws DBAppException{
    	//check if it is the first entry
        readData();
        numberOfTuples++;
        data.add(t);
        Collections.sort(data);
        updateMinMax();
        writeData();
        data = null;
    }
    
    public Tuple removeLastRecord() throws DBAppException{
    	readData();
    	numberOfTuples--;
    	Tuple t = data.remove(data.size()-1);
    	updateMinMax();
    	writeData();
    	data = null;
    	return t;
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


    public int getNumberOfTuples() {
        return numberOfTuples;
    }

    public void setNumberOfTuples(int numberOfTuples) {
        this.numberOfTuples = numberOfTuples;
    }
    
    public void deleteRecords(Hashtable<String, Object> colNameValue, Vector<Column> columns) throws DBAppException{
    	readData();
    	Vector<Tuple> toBeRemoved = new Vector<>();
    	loop: for(Tuple t: data) {
    		for(Map.Entry<String, Object> val: colNameValue.entrySet()) {
    			int i = columns.indexOf(val.getKey());
    			if(!val.getValue().equals(t.getIthVal(i))) {
    				continue loop;
    			}
    		}
    		toBeRemoved.add(t);
    		numberOfTuples--;
    	}
    	data.removeAll(toBeRemoved);
    	updateMinMax();
    	writeData();
    	data = null;
    }

	public void deletePageFromDisk() {
		File f = new File(path);
		f.delete();
	}
}
