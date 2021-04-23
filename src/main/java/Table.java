import java.io.Serializable;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

public class Table implements Serializable {
    private String tableName;
    private String clusteringKey;
    private String clusteringKeyType;
    private Vector<Column> columns;
    private Vector<Page> pages;
    private int pageMaxRows;

    public Table (String tableName, String clusteringKey, Hashtable<String, String> colNameType,
                  Hashtable<String, String> colNameMin, Hashtable<String, String> colNameMax, int pageMaxRows){
        this.tableName = tableName;
        this.clusteringKey = clusteringKey;
        this.clusteringKeyType = colNameType.get(clusteringKey);
        columns = new Vector<>();
        // first column is the primary key
        for(Map.Entry<String, String> ent: colNameType.entrySet()){
            Column col = new Column(ent.getKey(), ent.getValue(), clusteringKey.equals(ent.getKey()),
                    false, colNameMin.get(ent.getKey()), colNameMax.get(ent.getKey()));
            if(ent.getKey().equals(clusteringKey))
                columns.add(0, col);
            else
                columns.add(col);
        }
        pages = new Vector<>();
        this.pageMaxRows = pageMaxRows;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getClusteringKey() {
        return clusteringKey;
    }

    public void setClusteringKey(String clusteringKey) {
        this.clusteringKey = clusteringKey;
    }

    public Vector<Column> getColumns() {
        return columns;
    }

    public void setColumns(Vector<Column> columns) {
        this.columns = columns;
    }

    public Vector<Page> getPages() {
        return pages;
    }

    public void setPages(Vector<Page> pages) {
        this.pages = pages;
    }

    public void insertRecord(Hashtable<String, Object> colNameValue) {
        Vector<Object> data = new Vector<>();
        for (Column c: columns){
            data.add(colNameValue.get(c.getName()));
        }
        Tuple t = new Tuple(data);
        if(pages.size()==0){
            Page page = new Page(tableName, clusteringKeyType);
            page.insertRecord(t);
        } else{
            // search for target page
        }
    }
}
