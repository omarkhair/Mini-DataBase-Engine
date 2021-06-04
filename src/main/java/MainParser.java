
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

public class MainParser {

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
		if (!sa.nextWord().equals("where"))
			throw new DBAppException("you have to add WHERE to specify the selection terms");
		while (sa.hasMoreWords()) {
			String columnName = sa.nextWord();
			String operator = sa.nextWord();
			String value = sa.nextWord();
			String type = table.getColumn(columnName).getDataType();
			Object val = Utilities.parseType(value, type);
			SQLTerm t = new SQLTerm(tableName, columnName, operator, val);
			terms.add(t);
			if (sa.hasMoreWords())
				operators.add(sa.nextWord());
		}
		DBApp db = new DBApp();
		String[] arrayOperators = new String[operators.size()];
		for (int i = 0; i < arrayOperators.length; i++)
			arrayOperators[i] = operators.get(i);
		SQLTerm[] sqlTerms = new SQLTerm[terms.size()];
		for (int i = 0; i < sqlTerms.length; i++)
			sqlTerms[i] = terms.get(i);
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
		else if (sa.hasMoreWords() && sa.readNextWord().equals("where")) {
			while (sa.hasMoreWords()) {
				String columnName = sa.nextWord();
				if (!sa.hasMoreWords() || !sa.nextWord().equals("="))
					throw new DBAppException("expected = operator in deletion condiitons");
				String val = sa.nextWord();
				Column c = table.getColumn(columnName);
				Object value = Utilities.parseType(val, c.getDataType());
				columnNameValue.put(columnName, value);
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
			String val = sa.nextWord();
			Column c = table.getColumn(columnName);
			Object value = Utilities.parseType(val, c.getDataType());
			columnNameValue.put(columnName, value);
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
					String val = sa.nextWord();
					if (type.equals("java.lang.String")) {
						if (val.charAt(0) != '\'' || val.charAt(val.length() - 1) != '\'')
							throw new DBAppException("put the string in single quotes");
						val = val.substring(1, val.length() - 1);
					}

					Object value = Utilities.parseType(val, type);
					colNameValue.put(columns.get(i).getName(), value);
					if (!sa.nextWord().equals(","))
						throw new DBAppException("separate columns with commas");
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
		String[] columnNames = (String[]) columns.toArray();
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
			String colMin = "";
			String colMax = "";
			switch (colType) {
			case "int":
				colType = "java.lang.Integer";
				colMin = Integer.MIN_VALUE + "";
				colMax = Integer.MAX_VALUE + "";
				break;
			case "double":
				colType = "java.lang.Double";
				colMin = Double.MIN_VALUE + "";
				colMax = Double.MAX_VALUE + "";
				break;
			case "date":
				colType = "java.util.Date";
				colMin = "0000-00-00";
				colMax = "9999-12-31";
				break;
			case "varchar":
				colType = "java.lang.String";
				colMin = ((char) 32) + "";
				colMax = ((char) 126) + "";
				break;
			default:
				throw new DBAppException("Syntax error, unsupported data type " + colType);
			}
			colNameType.put(colName, colType);
			colNameMin.put(colName, colMin);
			colNameMax.put(colName, colMax);
			if (!sa.nextWord().equals(","))
				throw new DBAppException("separate input columns with commas");
		}
		if (colNameType.size() == 0)
			throw new DBAppException("table must have at least one column");
		if (clusteringKey.equals(""))
			throw new DBAppException(
					"table must have a primary (clustering) key. Specify using: PRIMARY KEY(INTENDED_KEY)");
		DBApp db = new DBApp();
		db.createTable(tableName, clusteringKey, colNameType, colNameMin, colNameMax);
	}

	public static void main(String[] args) {

	}

}
