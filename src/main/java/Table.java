import java.io.Serializable;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;

public class Table implements Serializable {
    private String tableName;
    private String clusteringKey;
    private ArrayList<Column> columns;
    private ArrayList<Page> pages;

    public Table (String tableName, String clusteringKey, Hashtable<String, String> colNameType,
                  Hashtable<String, String> colNameMin, Hashtable<String, String> colNameMax){
        this.tableName = tableName;
        this.clusteringKey = clusteringKey;
        columns = new ArrayList<>();
        for(Map.Entry<String, String> ent: colNameType.entrySet()){
            Column col = new Column(ent.getKey(), ent.getValue(), clusteringKey.equals(ent.getKey()),
                    false, colNameMin.get(ent.getKey()), colNameMax.get(ent.getKey()));
            columns.add(col);
        }
    }

}
