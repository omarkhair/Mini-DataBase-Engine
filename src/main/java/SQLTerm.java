
public class SQLTerm {
    String _strTableName;
    String _strColumnName;
    String _strOperator;
    Object _objValue;
    
    public SQLTerm() {
    	
    }

    public SQLTerm(String _strTableName, String _strColumnName, String _strOperator, Object _objValue){
    	this._objValue = _objValue;
    	this._strColumnName = _strColumnName;
    	this._strOperator = _strOperator;
    	this._strTableName = _strTableName;
    }

	public String get_strTableName() {
		return _strTableName;
	}

	public void set_strTableName(String _strTableName) {
		this._strTableName = _strTableName;
	}

	public String get_strColumnName() {
		return _strColumnName;
	}

	public void set_strColumnName(String _strColumnName) {
		this._strColumnName = _strColumnName;
	}

	public String get_strOperator() {
		return _strOperator;
	}

	public void set_strOperator(String _strOperator) {
		this._strOperator = _strOperator;
	}

	public Object get_objValue() {
		return _objValue;
	}

	public void set_objValue(Object _objValue) {
		this._objValue = _objValue;
	}
	
	public boolean sameColumn(Column c) {
    	return c.getName().equals(_strColumnName);
    }
	public boolean checkSameValue(Object value) throws DBAppException {
		Comparable comparedValue = (Comparable)value; 
		Comparable termValue  = (Comparable) this._objValue;
		switch(this.get_strOperator()) {
		case ">":  return (comparedValue.compareTo(termValue)>0);
		case ">=": return (comparedValue.compareTo(termValue)>=0); 
		case "<":  return (comparedValue.compareTo(termValue)<0); 
		case "<=": return (comparedValue.compareTo(termValue)<=0);  
		case "=":  return (comparedValue.compareTo(termValue)==0); 
		case "!=": return (comparedValue.compareTo(termValue)!=0); 	
		default: throw new DBAppException("wrong passed sql term");

	}
	}
	
	public String toString() {
		return _strColumnName+" "+_strOperator+" "+_objValue.toString();
	}

}
