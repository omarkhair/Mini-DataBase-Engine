import java.io.File;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Map;
import java.util.Observer;
import java.util.Vector;

public class Page implements Serializable, Comparable {
    //static int pageCounter = 0;

    private int id;
    private String path; // src/pages/page1.ser
    private String keyType;
    private Object minValue;
    private Object maxValue;
    private String tableName;
    private int numberOfTuples;
    private Tuple updatedClustering ; 
    transient private Vector<Tuple> data;
    private TableObserver observer;
    public Page(String tableName, String keyType,  int id, TableObserver t){
        this.tableName = tableName;
        this.keyType = keyType;
        this.id = id;
        path = "src/main/resources/data/"+ tableName +"/pages/page"+id+".ser";
        data = new Vector<Tuple>();
        observer = t;
        numberOfTuples = 0;
        writeData();
        updatedClustering = null;
        data = null;
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

    public void insertRecord(Tuple t) throws DBAppException{
    	insertRecordInMemory(t);
        writeData();
        data = null;
    }
    // This is a faster version but you need to handle syncing with the data on disk
    @SuppressWarnings("unchecked")
	public void insertRecordInMemory(Tuple t) throws DBAppException{
        if(data == null)
            readData();
        //check if it is the first entry
        if(data.contains(t))
            throw new DBAppException("Primary key already exists in table");
        numberOfTuples++;
        data.add(t);
        Collections.sort(data);
        updateMinMax();
        observer.notifyInsert(id, t);
    }
    
    public Tuple removeLastRecord() throws DBAppException{
    	Tuple t = removeLastRecordInMemory();
    	writeData();
    	data = null;
    	return t;
    }

    // This is a faster version but you need to handle syncing with the data on disk (serialization)
    public Tuple removeLastRecordInMemory() throws DBAppException{
        if(data == null)
            readData();
        if(numberOfTuples == 0)
            return null;
        numberOfTuples--;
        Tuple t = data.remove(data.size()-1);
        updateMinMax();
        observer.notifyDelete(id, t);
        return t;
    }

    public void updateMinMax() {
    	if(numberOfTuples == 0 || data == null)
    		return;
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

    public Vector<Tuple> getData() {
        return data;
    }

    public void setData(Vector<Tuple> data) {
        this.data = data;
    }

    public void deleteRecords(Hashtable<String, Object> colNameValue, Vector<Column> columns) throws DBAppException{
    	readData();
    	Vector<Tuple> toBeRemoved = new Vector<>();
    	loop: for(Tuple t: data) {
    		for(Map.Entry<String, Object> val: colNameValue.entrySet()) {
    			int i = getIndexOf(val.getKey(),columns);
    			if(!val.getValue().equals(t.getIthVal(i))) {
    				continue loop;
    			}
    		}
    		toBeRemoved.add(t);
    		observer.notifyDelete(id, t);
    		numberOfTuples--;
    	}
    	data.removeAll(toBeRemoved);
    	updateMinMax();
    	writeData();
    	data = null;
    }

	private int getIndexOf(String key, Vector<Column> columns) {
		for(int i = 0; i<columns.size();i++) {
			Column c = columns.get(i);
			if(c.getName().equals(key))
				return i;
		}
		return -1;
	}

	public void deletePageFromDisk() {
		File f = new File(path);
		f.delete();
	}
	
	public boolean updateRecord(String clusteringKey , Hashtable<String,Object> columnNameValue,Vector<Column> columns) throws DBAppException {
        readData();
    	boolean flag = false ; 
        loop: for(Tuple t : data){
            if((t.getData().get(0)).toString().equals(clusteringKey)){
            	observer.notifyDelete(id, t);
                for(Map.Entry<String,Object> entry : columnNameValue.entrySet()){
                    int indx = getIndexOf(entry.getKey(), columns); 
                    if(indx==0) {
                    	flag = true ; 
                    }
                    t.getData().set(indx,entry.getValue());
                }
                if(flag) {
                	this.updatedClustering  = t ; 
                	data.remove(t);
                	numberOfTuples--; 
                	updateMinMax();
                	if(numberOfTuples==0){
                	    deletePageFromDisk();
                    }
                }
                else
                	observer.notifyInsert(id, t);
                break loop ;
            }
        }
        writeData();
        data= null ;
        return flag ; 
    }
	
	public Tuple getUpdatedClustering() {
		return this.updatedClustering ; 
	}

	public String toString() {
		String result = ""; //"Page ID:  "+id+"\n";
		try {readData();} catch (DBAppException e) {return e.getMessage()+"\n";}
		for(Tuple t: data) {
			result += t.toString()+"\n";
		}
		data = null;
		return result;
	}
	public int getId() {
		return id; 
	}
	public void setId(int id ) {
		this.id = id ; 
	}


}
