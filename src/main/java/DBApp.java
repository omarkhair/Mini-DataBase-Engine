import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

public class DBApp implements DBAppInterface {
    @Override
    public void init() {

    }

    @Override
    public void createTable(String tableName, String clusteringKey, Hashtable<String, String> colNameType,
                            Hashtable<String, String> colNameMin, Hashtable<String, String> colNameMax)
            throws DBAppException {
        validateEntries(tableName, clusteringKey,colNameType, colNameMin, colNameMax);

        // create metadata file
        create_metadata(tableName, clusteringKey, colNameType, colNameMin, colNameMax);
    }

    private void create_metadata(String tableName, String clusteringKey, Hashtable<String, String> colNameType,
                                 Hashtable<String, String> colNameMin, Hashtable<String, String> colNameMax) {
        try {
            PrintWriter pw = new PrintWriter("src/main/resources/metadata_"+tableName+".csv");
            pw.println("Table Name, Column Name, Column Type, ClusteringKey, Indexed, min, max");
            for (Map.Entry<String, String> ent: colNameType.entrySet()) {
                pw.println(tableName+", "+ ent.getKey()+", "+ent.getValue()+", "
                        +(ent.getKey().equals(clusteringKey)?"True":"False") + ", False, "
                        + colNameMin.get(ent.getKey())+", "+ colNameMax.get(ent.getKey()));
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }


    }
    private void validateEntries(String tableName,
                                String clusteringKey,
                                Hashtable<String, String> colNameType,
                                Hashtable<String, String> colNameMin,
                                Hashtable<String, String> colNameMax) throws DBAppException{
        boolean flag = false;
        for(Map.Entry<String, String> ent:colNameType.entrySet()) {
            if(ent.getKey().equals(clusteringKey))
                flag = true;
            if(!(ent.getValue().equals("java.lang.Integer")||
                    ent.getValue().equals("java.lang.String")||
                    ent.getValue().equals("java.lang.Double")||
                    ent.getValue().equals("java.lang.Date")))
                throw new DBAppException();

            if(!colNameMin.contains(ent.getKey())||!colNameMax.contains(ent.getKey()))
                throw new DBAppException();
        }
        if(!flag)
            throw new DBAppException();
    }

    @Override
    public void createIndex(String tableName, String[] columnNames) throws DBAppException {

    }

    @Override
    public void insertIntoTable(String tableName, Hashtable<String, Object> colNameValue) throws DBAppException {

    }

    @Override
    public void updateTable(String tableName, String clusteringKeyValue, Hashtable<String, Object> columnNameValue) throws DBAppException {

    }

    @Override
    public void deleteFromTable(String tableName, Hashtable<String, Object> columnNameValue) throws DBAppException {

    }

    @Override
    public Iterator selectFromTable(SQLTerm[] sqlTerms, String[] arrayOperators) throws DBAppException {
        return null;
    }
}
