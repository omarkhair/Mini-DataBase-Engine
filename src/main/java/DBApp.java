import java.io.*;
import java.sql.Date;
import java.util.*;

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
		String path = "src/main/resources/data/" + tableName;
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
					|| ent.getValue().equals("java.lang.Double") || ent.getValue().equals("java.util.Date")))
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
		String path = "src/main/resources/data/" + tableName + "/" + tableName + ".ser";
		Table table = (Table) Serializer.deserilize(path);
		Vector<Column> cols = table.getCols(columnNames);
		// edit metadata file
		for(Column col: cols)
			col.setIndexed(true);
		table.create_metadata();
		
		GridIndex index = new GridIndex(tableName, table.getLastIndexId(), MaximumKeysCountinIndexBucket, cols);
		table.getIndecies().add(index);
		table.setLastIndexId(table.getLastIndexId() + 1);
		// insert all rows of the table in the newly created index
		table.populateIndex(index);
		Serializer.serialize(path, table);
		//System.out.println(index);
	}


	@Override
	public void insertIntoTable(String tableName, Hashtable<String, Object> colNameValue) throws DBAppException {
		String path = "src/main/resources/data/" + tableName + "/" + tableName + ".ser";
		Table table = (Table) Serializer.deserilize(path);
		table.verifyInsertion(colNameValue);
		// verify the types of the inserted values
		table.insertRecord(colNameValue);
		//table.updatePagesRecord();
		Serializer.serialize(path, table);
	}

	@Override
	public void updateTable(String tableName, String clusteringKeyValue, Hashtable<String, Object> columnNameValue) throws DBAppException {
    	String path = "src/main/resources/data/"+tableName+"/"+tableName+".ser";
        Table table = (Table) Serializer.deserilize(path);
        table.verifyUpdate(clusteringKeyValue,columnNameValue);
        table.updateRecord(clusteringKeyValue,columnNameValue);
        //table.updatePagesRecord();
        Serializer.serialize(path, table);
    }

	@Override
	public void deleteFromTable(String tableName, Hashtable<String, Object> columnNameValue) throws DBAppException {
		String path = "src/main/resources/data/" + tableName + "/" + tableName + ".ser";
		Table table = (Table) Serializer.deserilize(path);
		// false return means there is a condition beyond min and max of some column
		if (!table.verifyDeletion(columnNameValue))
			return;
		table.deleteRecords(columnNameValue);
		//table.updatePagesRecord();
		Serializer.serialize(path, table);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Iterator selectFromTable(SQLTerm[] sqlTerms, String[] arrayOperators) throws DBAppException {
		Table table = (Table) Serializer.deserilize("src/main/resources/data/"+sqlTerms[0].get_strTableName()+"/"+sqlTerms[0].get_strTableName()+".ser");
		Vector<Column> selectColumns = validateSelect(sqlTerms,arrayOperators,table);
		Vector<SQLTerm> andedTerms = new Vector<SQLTerm>();
		andedTerms.add(sqlTerms[0]);
		int i = 1;
		HashSet<Integer> pagesIds = new HashSet<Integer>();
		for(String s : arrayOperators) {
			if(s.equals("AND")) {
				andedTerms.add(sqlTerms[i]);
			}
			else {
				try {
					pagesIds.addAll(computeAND(andedTerms, table));
				}
				catch(NoIndexFoundException e) {
					pagesIds = table.getAllPagesIds();
					break;
				}
				andedTerms.removeAllElements();
			}
			i++;
		}
		
		return table.evaluateSelect(pagesIds,sqlTerms,arrayOperators);

	}

	private HashSet<Integer> computeAND(Vector<SQLTerm> andedTerms, Table table) throws NoIndexFoundException{
		GridIndex g = table.getBestIndex(andedTerms);
		if(g==null) throw new NoIndexFoundException();
		Vector<SQLTerm> andedTermsInIndex = getAndedTermsInIndex(g,andedTerms);
		return g.getPagesIds(andedTermsInIndex);
	}

	private Vector<SQLTerm> getAndedTermsInIndex(GridIndex g, Vector<SQLTerm> andedTerms) {
		Vector<SQLTerm> res = new Vector<SQLTerm>();
		for(Column c : g.getColumns())
			for(SQLTerm t : andedTerms)
				if(c.sameColumn(t))
					res.add(t);
		return res;
	}

	private Vector<Column> validateSelect(SQLTerm[] sqlTerms, String[] arrayOperators, Table table) throws DBAppException {
		Vector<Column> res = new Vector<Column>();
		for(SQLTerm t: sqlTerms) {
			Column flag = null;
			if(!t.get_strTableName().equals(table.getTableName()))
				throw new DBAppException("Passed SQL Terms select from different tables "+t.get_strTableName()+" and "+table.getTableName());
			for(Column c : table.getColumns()) {
				if(c.getName().equals(t.get_strColumnName()))
					flag = c;
			}
			if(flag==null)
				throw new DBAppException("Column "+t.get_strColumnName()+" does not exist in table "+table.getTableName());
			else
				res.add(flag);
			if(t.get_strOperator()!=">"&&t.get_strOperator()!=">="&&t.get_strOperator()!="<"&&
					t.get_strOperator()!="<="&&t.get_strOperator()!="!="&&t.get_strOperator()!="=")
				throw new DBAppException("undefined operator "+t.get_strOperator());
			//Utilities.parseType(flag.getDataType(), dataType);
			if(flag.getDataType().equals("java.lang.Integer")&&!(t.get_objValue() instanceof Integer))
				throw new DBAppException("Select term data type mismatch");
			if(flag.getDataType().equals("java.lang.Double")&&!(t.get_objValue() instanceof Double))
				throw new DBAppException("Select term data type mismatch");
			if(flag.getDataType().equals("java.lang.String")&&!(t.get_objValue() instanceof String))
				throw new DBAppException("Select term data type mismatch");
			if(flag.getDataType().equals("java.util.Date")&&!(t.get_objValue() instanceof Date))
				throw new DBAppException("Select term data type mismatch");
		}
		for(String o:arrayOperators) {
			if(!o.equals("AND")&&!o.equals("OR")&&!o.equals("XOR"))
				throw new DBAppException("undefined operator "+o);
		}
		if(arrayOperators.length != sqlTerms.length-1)
			throw new DBAppException("Number of operators has to match the number of the terms");
		return res;
	}

	public void readConfig() {
		String path = "src/main/resources/DBApp.config";

		try {
			@SuppressWarnings("resource")
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

	public static void main(String[] args) throws DBAppException, InterruptedException {
	DBApp dbApp = new DBApp();
//        Hashtable htblColNameType = new Hashtable( );
//        htblColNameType.put("id", "java.lang.Integer");
//        htblColNameType.put("name", "java.lang.String");
//        htblColNameType.put("gpa", "java.lang.Double");
//        Hashtable htblColNameMin = new Hashtable( );
//        htblColNameMin.put("id", "0");
//        htblColNameMin.put("name", "a");
//        htblColNameMin.put("gpa", "1");
//        Hashtable htblColNameMax = new Hashtable( );
//        htblColNameMax.put("id", "1000");
//        htblColNameMax.put("name", "z");
//        htblColNameMax.put("gpa", "4");
//        dbApp.createTable( "test", "id", htblColNameType ,htblColNameMin,htblColNameMax);
//        char x = 'a';
//        boolean t = true;
//		for (int i = 0; i <= 22; i++) {
//		Hashtable record1 = new Hashtable();
//		record1.put("id", i);
//		record1.put("name", ""+x);
//		if(t)
//			x++;
//		t = !t;
//		record1.put("gpa", 2.0);
//		dbApp.insertIntoTable("test", record1);
//		}
		
		Table table = (Table) Serializer.deserilize("src/main/resources/data/test/test.ser");
		//System.out.println(table.getIndecies().size());
        //String[] colnames = {"name","id"};
        //dbApp.createIndex("test", colnames);
        
        System.out.println(table.getIndecies().get(1));
        //System.out.println(table.getIndecies().size());
//		for (int i = 0; i < 30; i+=3) {
//			Hashtable record1 = new Hashtable();
//			record1.put("id", i);
//			record1.put("name", "ahmed");
//			record1.put("gpa", 3.0);
//			dbApp.insertIntoTable("Student", record1);
//		}
//		for (int i = 2; i < 30; i+=3) {
//			Hashtable record1 = new Hashtable();
//			record1.put("id", i);
//			record1.put("name", "omar");
//			record1.put("gpa", 2.0);
//			dbApp.insertIntoTable("Student", record1);
//		}
//		Hashtable record1 = new Hashtable();
//		record1.put("id",7);
//		record1.put("name", "hesham");
//		record1.put("gpa", 2.0);
//		dbApp.insertIntoTable("Student", record1);
//		for(int i=6;i<12;i++) {
//		 Hashtable<String, Object> ht = new Hashtable<>();
//			ht.put("id", 37);
//			ht.put("gpa", 2.6);
//			ht.put("name","mohamed"); 
//			dbApp.updateTable("Student", "9",ht);
////			 
//////		}
//
//		Table table = (Table) Serializer.deserilize("src/main/resources/data/Student/Student.ser");
//		System.out.println(table);
//		
		// dbApp.createTable("Employee", "id", colNameType, colNameMin, colNameMax);

	}
}
