
import java.io.File;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Vector;

public class MainParser {

	public Iterator parseSQL(String s) throws DBAppException {
		return parseSQL(new StringBuffer(s));
	}

	@SuppressWarnings("rawtypes")
	public Iterator parseSQL(StringBuffer strbufSQL) throws DBAppException {
		StringAnalyzer sa = new StringAnalyzer(strbufSQL.toString());
		if (sa.hasMoreWords()) {
			String command = sa.nextWord();
			if (command.equals("create")) {
				String type = sa.nextWord();
				if (type.equals("table")) {
					createTable(sa);
					return null;
				} else if (type.equals("index")) {
					createIndex(sa);
					return null;
				} else {
					throw new DBAppException("undefined create type. you asked to create a " + type);
				}
			} else if (command.equals("insert")) {
				insert(sa);
				return null;
			} else if (command.equals("update")) {
				update(sa);
				return null;
			} else if (command.equals("delete")) {
				delete(sa);
				return null;
			} else if (command.equals("select")) {
				return select(sa);
			} else {
				throw new DBAppException("undefined instruction " + command);
			}
		} else
			return null;
	}

	@SuppressWarnings("rawtypes")
	private Iterator select(StringAnalyzer sa) throws DBAppException {
		if (!sa.nextWord().equals("*"))
			throw new DBAppException("you have to add * after select, for we don't support projection");
		if (!sa.nextWord().equals("from"))
			throw new DBAppException("you have to add FROM to specify the selection table");
		String tableName = sa.nextWord();
		String path = "src/main/resources/data/" + tableName + "/" + tableName + ".ser";
		Table table = (Table) Serializer.deserilize(path);
		Vector<String> operators = new Vector<String>();
		Vector<SQLTerm> terms = new Vector<SQLTerm>();
		if (!sa.hasMoreWords() || !sa.nextWord().equals("where"))
			throw new DBAppException("you have to add WHERE to specify the selection terms");
		while (sa.hasMoreWords()) {
			String columnName = sa.nextWord();
			String operator = sa.nextWord();
			String value = sa.nextWord();
			String type = table.getColumn(columnName).getDataType();
			if (type.equals("java.lang.String") || type.equals("java.util.Date"))
				if (value.charAt(0) != '\'' || value.charAt(value.length() - 1) != '\'')
					throw new DBAppException("put varchar and date in single quotes");
				else
					value = value.substring(1, value.length() - 1);
			Object val = Utilities.parseType(value, type);
			SQLTerm t = new SQLTerm(tableName, columnName, operator, val);
			terms.add(t);
			if (sa.hasMoreWords())
				operators.add(sa.nextWord());
		}
		DBApp db = new DBApp();
		String[] arrayOperators = new String[operators.size()];
		for (int i = 0; i < arrayOperators.length; i++)
			arrayOperators[i] = operators.get(i).toUpperCase();
		SQLTerm[] sqlTerms = new SQLTerm[terms.size()];
		for (int i = 0; i < sqlTerms.length; i++)
			sqlTerms[i] = terms.get(i);
//		System.out.println(Arrays.toString(sqlTerms));
//		System.out.println(Arrays.toString(arrayOperators));
		return db.selectFromTable(sqlTerms, arrayOperators);
	}

	private void delete(StringAnalyzer sa) throws DBAppException {
		if (!sa.nextWord().equals("from"))
			throw new DBAppException("you have to add FROM to specify the deletion table");
		String tableName = sa.nextWord();
		String path = "src/main/resources/data/" + tableName + "/" + tableName + ".ser";
		Table table = (Table) Serializer.deserilize(path);
		Hashtable<String, Object> columnNameValue = new Hashtable<String, Object>();
		if (sa.hasMoreWords() && !sa.readNextWord().equals("where"))
			throw new DBAppException("you have to add WHERE to specify the ANDED Deletion conitions");
		else if (sa.hasMoreWords() && sa.nextWord().equals("where")) {
			while (sa.hasMoreWords()) {
				String columnName = sa.nextWord();
				if (!sa.hasMoreWords() || !sa.nextWord().equals("=")) {
					// System.out.println(sa.readNextWord());
					throw new DBAppException("expected = operator in deletion conditions");
				}
				String value = sa.nextWord();
				Column c = table.getColumn(columnName);
				String type = c.getDataType();
				if (type.equals("java.lang.String") || type.equals("java.util.Date"))
					if (value.charAt(0) != '\'' || value.charAt(value.length() - 1) != '\'')
						throw new DBAppException("put varchar and date in single quotes");
					else
						value = value.substring(1, value.length() - 1);
				Object val = Utilities.parseType(value, type);
				columnNameValue.put(columnName, val);
				if (sa.hasMoreWords() && !sa.nextWord().equals("and"))
					throw new DBAppException("expected AND operator in deletion condiitons");
			}
		} else if (sa.hasMoreWords())
			throw new DBAppException("syntax error: unexpected " + sa.nextWord());
		DBApp db = new DBApp();
		db.deleteFromTable(tableName, columnNameValue);
	}

	private void update(StringAnalyzer sa) throws DBAppException {
		String tableName = sa.nextWord();
		String path = "src/main/resources/data/" + tableName + "/" + tableName + ".ser";
		Table table = (Table) Serializer.deserilize(path);
		Hashtable<String, Object> columnNameValue = new Hashtable<String, Object>();
		if (!sa.nextWord().equals("set"))
			throw new DBAppException("you have to add SET to specify the update columns");
		while (sa.hasMoreWords()) {
			String columnName = sa.nextWord();
			if (!sa.nextWord().equals("="))
				throw new DBAppException("expected = after column name in updated columns");
			String value = sa.nextWord();
			Column c = table.getColumn(columnName);
			String type = c.getDataType();
			if (type.equals("java.lang.String") || type.equals("java.util.Date"))
				if (value.charAt(0) != '\'' || value.charAt(value.length() - 1) != '\'')
					throw new DBAppException("put varchar and date in single quotes");
				else
					value = value.substring(1, value.length() - 1);
			Object val = Utilities.parseType(value, type);
			columnNameValue.put(columnName, val);
			if (sa.readNextWord().equals("where"))
				break;
			if (!sa.nextWord().equals(","))
				throw new DBAppException("expected , between updated columns");
		}
		if (!sa.nextWord().equals("where"))
			throw new DBAppException("expected where after updated columns");
		String clusteringKey = sa.nextWord();
		if (!clusteringKey.equals(table.getClusteringKey()))
			throw new DBAppException("update condition should be only on clustering key");
		if (!sa.nextWord().equals("="))
			throw new DBAppException("expected = after clustering key");
		String clusteringKeyValue = sa.nextWord();
		if (sa.hasMoreWords())
			throw new DBAppException("syntax error: unexpected " + sa.nextWord());
		DBApp db = new DBApp();
		db.updateTable(tableName, clusteringKeyValue, columnNameValue);
	}

	private void insert(StringAnalyzer sa) throws DBAppException {
		Hashtable<String, Object> colNameValue = new Hashtable<String, Object>();
		if (sa.nextWord().equals("into")) {
			String tableName = sa.nextWord();
			String path = "src/main/resources/data/" + tableName + "/" + tableName + ".ser";
			Table table = (Table) Serializer.deserilize(path);
			Vector<Column> columns = table.getColumns();
			if (sa.nextWord().equals("values") && sa.nextWord().equals("(")) {
				for (int i = 0; i < columns.size(); i++) {
					String type = columns.get(i).getDataType();
					String value = sa.nextWord();
					if (type.equals("java.lang.String") || type.equals("java.util.Date"))
						if (value.charAt(0) != '\'' || value.charAt(value.length() - 1) != '\'')
							throw new DBAppException("put varchar and date in single quotes");
						else
							value = value.substring(1, value.length() - 1);

					Object val = Utilities.parseType(value, type);
					colNameValue.put(columns.get(i).getName(), val);
					if (sa.readNextWord().equals(")"))
						break;
					if (!sa.nextWord().equals(","))
						throw new DBAppException(
								"separate columns with commas && make sure to insert values for all columns");
				}
				if (!sa.nextWord().equals(")"))
					throw new DBAppException("end the parameters with )");
				DBApp db = new DBApp();
				db.insertIntoTable(tableName, colNameValue);
			} else {
				throw new DBAppException("Syntax error, expected command..Values followed by (");
			}

		} else {
			throw new DBAppException("Syntax error, expected command..INTO");
		}

	}

	private void createIndex(StringAnalyzer sa) throws DBAppException {
		sa.nextWord();
		if (!sa.nextWord().equals("on"))
			throw new DBAppException("syntax error: type on after index name");
		String tableName = sa.nextWord();
		Vector<String> columns = new Vector<String>();
		if (!sa.nextWord().equals("("))
			throw new DBAppException("put indexed columns in parantheses");
		while (true) {
			columns.add(sa.nextWord());
			if (sa.readNextWord().equals(")"))
				break;
			if (!sa.nextWord().equals(","))
				throw new DBAppException("separate indexed columns with commas");
		}
		sa.nextWord();
		String[] columnNames = new String[columns.size()];
		for (int i = 0; i < columnNames.length; i++)
			columnNames[i] = columns.get(i);
		DBApp db = new DBApp();
		db.createIndex(tableName, columnNames);

	}

	private void createTable(StringAnalyzer sa) throws DBAppException {
		String tableName = sa.nextWord();
		String clusteringKey = "";
		Hashtable<String, String> colNameType = new Hashtable<String, String>();
		Hashtable<String, String> colNameMin = new Hashtable<String, String>();
		Hashtable<String, String> colNameMax = new Hashtable<String, String>();
		if (!sa.nextWord().equals("("))
			throw new DBAppException("put table columns in parantheses");
		while (!sa.readNextWord().equals(")")) {
			String colName = sa.nextWord();
			if (colName.equals("primary")) {
				if (sa.nextWord().equals("key")) {
					if (!sa.nextWord().equals("("))
						throw new DBAppException("put primary key in parantheses");
					clusteringKey = sa.nextWord();
					if (!sa.nextWord().equals(")"))
						throw new DBAppException("put primary key in parantheses");
				} else
					throw new DBAppException("wrong syntax: type key after primary to specify primary key");
				if (sa.readNextWord().equals(")"))
					break;
				if (!sa.nextWord().equals(","))
					throw new DBAppException("separate input columns with commas");
				continue;
			}
			String colType = sa.nextWord();
			// System.out.println(sa.readNextWord());
			if (!sa.nextWord().equals("check") || !sa.nextWord().equals("(") || !sa.nextWord().equals(colName)
					|| !sa.nextWord().equals("between")) {
				throw new DBAppException(
						"Add check constraint similar to CHECK(COLUMN_NAME BETWEEN MIN_VALUE AND MAX_VALUE) to specify each column min and max values");
			}
			String colMin = sa.nextWord();
			if (colType.equals("varchar") || colType.equals("date"))
				if (colMin.charAt(0) != '\'' || colMin.charAt(colMin.length() - 1) != '\'')
					throw new DBAppException("put varchar and date in single quotes for min value");
				else
					colMin = colMin.substring(1, colMin.length() - 1);
			if (!sa.nextWord().equals("and")) {
				throw new DBAppException(
						"Add check constraint similar to CHECK(COLUMN_NAME BETWEEN MIN_VALUE AND MAX_VALUE) to specify each column min and max values");
			}
			String colMax = sa.nextWord();
			if (colType.equals("varchar") || colType.equals("date"))
				if (colMax.charAt(0) != '\'' || colMax.charAt(colMax.length() - 1) != '\'')
					throw new DBAppException("put varchar and date in single quotes for max value");
				else
					colMax = colMax.substring(1, colMax.length() - 1);
			if (!sa.nextWord().equals(")")) {
				throw new DBAppException(
						"Add check constraint similar to CHECK(COLUMN_NAME BETWEEN MIN_VALUE AND MAX_VALUE) to specify each column min and max values");
			}
			switch (colType) {
			case "int":
				colType = "java.lang.Integer";
				break;
			case "double":
				colType = "java.lang.Double";
				break;
			case "date":
				colType = "java.util.Date";
				break;
			case "varchar":
				colType = "java.lang.String";
				break;
			default:
				throw new DBAppException("Syntax error, unsupported data type " + colType);
			}
			colNameType.put(colName, colType);
			colNameMin.put(colName, colMin);
			colNameMax.put(colName, colMax);
			if (sa.readNextWord().equals(")"))
				break;
			if (!sa.nextWord().equals(","))
				throw new DBAppException("separate input columns with commas");
		}
		sa.nextWord();
		if (colNameType.size() == 0)
			throw new DBAppException("table must have at least one column");
		if (clusteringKey.equals(""))
			throw new DBAppException(
					"table must have a primary (clustering) key. Specify using: PRIMARY KEY(INTENDED_KEY)");
		DBApp db = new DBApp();
		db.createTable(tableName, clusteringKey, colNameType, colNameMin, colNameMax);
	}
	
	private static void deleteDir(File file) {
	    File[] contents = file.listFiles();
	    if (contents != null) {
	        for (File f : contents) {
	            deleteDir(f);
	        }
	    }
	    file.delete();
	}

	public static void main(String[] args) throws DBAppException {
		MainParser mp = new MainParser();
		// id,name,salary,dob
		// mp.parseSQL("create index yoyo on employee(id,salary)");
		// mp.parseSQL("create table employee(id int check(id between 1 and 1000),name varchar check (name between 'a' and 'zzzzzzzz'),salary double check(salary between 0 and 10000),dob date check(dob between '1930-01-01' and '2030-12-31'),primary key(id))");
//mp.parseSQL("update employee set name = 'ali' where id = 6");
//				for(int i = 1; i<700;i+=2) {
//			String name = i % 5 == 0?"mohamed":i%5==1?"hesham":i%5==2?"omar":i%5==3?"samer":"zinger";
//			mp.parseSQL("insert into employee values("+i+",'"+name+"',"+(i*10)+",'1999-10-10')");
//		}
//		//mp.parseSQL("insert into employee values(103,'omar',1000,'1990-10-10')");
//		//mp.parseSQL("delete from employee where id = 0");
		// Table t =
		// (Table)Serializer.deserilize("src/main/resources/data/employee/employee.ser");
//		//for(Column c : t.getColumns())
		// System.out.println(t.getIndecies().get(0));
		// t.getIndecies().remove(0);
		// Serializer.serialize("src/main/resources/data/employee/employee.ser", t);
		// System.out.println(t.getIndecies().get(0));
//			//System.out.println("ahmed".compareTo("z"));
		Scanner sc = new Scanner(System.in);
		while (true) {
			String instruction = sc.nextLine();
			if (instruction.equals("exit"))
				break;
			StringAnalyzer sa = new StringAnalyzer(instruction);
			if (sa.readNextWord().equals("show")) {
				sa.nextWord();
				if (sa.readNextWord().equals("table")) {
					sa.nextWord();
					String tableName = sa.nextWord();
					String path = "src/main/resources/data/" + tableName + "/" + tableName + ".ser";
					Table t = (Table) Serializer.deserilize(path);
					System.out.println(t);
				} else if (sa.nextWord().equals("index")) {
					int id = Integer.parseInt(sa.nextWord());
					sa.nextWord();
					sa.nextWord();
					String tableName = sa.nextWord();
					String path = "src/main/resources/data/" + tableName + "/" + tableName + ".ser";
					Table t = (Table) Serializer.deserilize(path);
					System.out.println(t.getIndecies().get(id));
				}
			} else if(sa.nextWord().equals("drop")) {
				if (sa.readNextWord().equals("table")) {
					sa.nextWord();
					String tableName = sa.nextWord();
					String path = "src/main/resources/data/" + tableName;
					File index = new File(path);
					deleteDir(index);
					System.out.println("the table "+tableName+" has been deleted");
				} else if (sa.nextWord().equals("index")) {
					int id = Integer.parseInt(sa.nextWord());
					sa.nextWord();
					sa.nextWord();
					String tableName = sa.nextWord();
					String path = "src/main/resources/data/" + tableName + "/index" + id;
					File f = new File(path);
					deleteDir(f);
					path = "src/main/resources/data/" + tableName + "/" + tableName + ".ser";
					Table t = (Table) Serializer.deserilize(path);
					t.getIndecies().remove(id);
					Serializer.serialize(path, t);
					System.out.println("the index "+id+" on table "+tableName+" have been deleted");
				}
			}
			
			else {
				Iterator res = mp.parseSQL(instruction);
				while (res!=null&&res.hasNext()) {
					System.out.println(res.next());
				}
			}
		}

	}
}
