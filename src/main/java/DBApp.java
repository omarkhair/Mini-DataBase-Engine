import java.io.*;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

public class DBApp implements DBAppInterface {
	public int MaximumRowsCountinPage;
	public int MaximumKeysCountinIndexBucket;
	
	public DBApp() {
		init();
	}

	@Override
	public void init() {
		this.readConfig();
	}

	@Override
	public void createTable(String tableName, String clusteringKey, Hashtable<String, String> colNameType,
			Hashtable<String, String> colNameMin, Hashtable<String, String> colNameMax) throws DBAppException {
		validateEntries(tableName, clusteringKey, colNameType, colNameMin, colNameMax);

		// create metadata file
		readConfig();
		Table table = new Table(tableName, clusteringKey, colNameType, colNameMin, colNameMax, MaximumRowsCountinPage);
		String path = "src/tables/" + tableName;
		File file = new File(path);
		file.mkdir();
		file = new File(path + "/pages");
		file.mkdir();
		Serializer.serialize(path + "/" + tableName + ".ser", table);
	}

	private void validateEntries(String tableName, String clusteringKey, Hashtable<String, String> colNameType,
			Hashtable<String, String> colNameMin, Hashtable<String, String> colNameMax) throws DBAppException {
		boolean flag = false;
		for (Map.Entry<String, String> ent : colNameType.entrySet()) {
			if (ent.getKey().equals(clusteringKey))
				flag = true;
			if (!(ent.getValue().equals("java.lang.Integer") || ent.getValue().equals("java.lang.String")
					|| ent.getValue().equals("java.lang.Double") || ent.getValue().equals("java.lang.Date")))
				throw new DBAppException();
			if (!colNameMin.containsKey(ent.getKey()) || !colNameMax.containsKey(ent.getKey())) {
				throw new DBAppException();
			}

		}
		if (!flag)
			throw new DBAppException();
	}

	@Override
	public void createIndex(String tableName, String[] columnNames) throws DBAppException {

	}

	@Override
	public void insertIntoTable(String tableName, Hashtable<String, Object> colNameValue) throws DBAppException {
		String path = "src/tables/" + tableName + "/" + tableName + ".ser";
		Table table = (Table) Serializer.deserilize(path);
		table.verifyInsertion(colNameValue);
		// verify the types of the inserted values
		table.insertRecord(colNameValue);
		Serializer.serialize(path, table);
	}

	@Override
	public void updateTable(String tableName, String clusteringKeyValue, Hashtable<String, Object> columnNameValue)
			throws DBAppException {

	}

	@Override
	public void deleteFromTable(String tableName, Hashtable<String, Object> columnNameValue) throws DBAppException {
		String path = "src/tables/" + tableName + "/" + tableName + ".ser";
		Table table = (Table) Serializer.deserilize(path);
		// false return means there is a condition beyond min and max of some column
		if (!table.verifyDeletion(columnNameValue))
			return;
		table.deleteRecords(columnNameValue);
		Serializer.serialize(path, table);
	}

	@Override
	public Iterator selectFromTable(SQLTerm[] sqlTerms, String[] arrayOperators) throws DBAppException {
		return null;
	}

	public void readConfig() {
		String path = "src/main/resources/DBApp.config";

		try {
			BufferedReader br = new BufferedReader(new FileReader(path));
			StringTokenizer st = new StringTokenizer(br.readLine());
			st.nextToken();
			st.nextToken();
			MaximumRowsCountinPage = Integer.parseInt(st.nextToken());
			st = new StringTokenizer(br.readLine());
			st.nextToken();
			st.nextToken();
			MaximumKeysCountinIndexBucket = Integer.parseInt(st.nextToken());
//                System.out.println(MaximumRowsCountinPage + " "+ MaximumKeysCountinIndexBucket);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.out.println("Config file not found");
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static void main(String[] args) throws DBAppException {
	DBApp dbApp = new DBApp();
        Hashtable htblColNameType = new Hashtable( );
        htblColNameType.put("id", "java.lang.Integer");
        htblColNameType.put("name", "java.lang.String");
        htblColNameType.put("gpa", "java.lang.Double");
        Hashtable htblColNameMin = new Hashtable( );
        htblColNameMin.put("id", "0");
        htblColNameMin.put("name", "a");
        htblColNameMin.put("gpa", "1");
        Hashtable htblColNameMax = new Hashtable( );
        htblColNameMax.put("id", "1000");
        htblColNameMax.put("name", "zzzzzzzzzzzzzzz");
        htblColNameMax.put("gpa", "4");
        dbApp.createTable( "Student", "id", htblColNameType ,htblColNameMin,htblColNameMax);
		Table table = (Table) Serializer.deserilize("src/tables/Student/Student.ser");
		for (int i = 0; i < 20; i+=2) {
			Hashtable record1 = new Hashtable();
			record1.put("id", i);
			record1.put("name", "hesham");
			record1.put("gpa", 2.0);
			dbApp.insertIntoTable("Student", record1);
		}
		for(int i = 25 ; i<40;i+=1) {
		Hashtable record1 = new Hashtable();
		record1.put("id",i);
		record1.put("name", "hesham");
		record1.put("gpa", 2.0);
		dbApp.insertIntoTable("Student", record1);}
//		for(int i=6;i<12;i++) {
//			Hashtable<String, Object> ht = new Hashtable<>();
//			ht.put("name", "hesham");
//			dbApp.deleteFromTable("Student", ht);
//		}
		table = (Table) Serializer.deserilize("src/tables/Student/Student.ser");
		System.out.println(table);
		
		// dbApp.createTable("Employee", "id", colNameType, colNameMin, colNameMax);

	}
}
