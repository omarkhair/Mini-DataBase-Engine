To test our project parser and logic:-
1-run the main method in the MainParser class
2-type in the console one of the following statements:-
	1-drop table TABLE_NAME ---to drop a table from the database engine
	2-drop index INDEX_ID on table TABLE_NAME ---to drop an index from the database engine
	3-show table TABLE_NAME ---prints the toString() method of the table
	4-show index INDEX_ID on table TABLE_NAME ---prints the toString() method of the index
	5-exit ---to terminate the program
	6-create table TABLE_NAME(COLUMN_NAME COLUMN_DATA_TYPE CHECK(COLUMN_NAME BETWEEN COLUMN_MIN and COLUMN_MAX), ..., primary key(CLUSTERING_KEY_NAME))
	7-create index INDEX_NAME on TABLE_NAME(COLUMN_NAME_1,...)
	8-select * from TABLE_NAME where CONDITION_1 OPERATOR_1 CONDITION_2 ....
	9-insert into TABLE_NAME values(COLUMN_1_VALUE, COLUMN_2_VALUE,...) ---columns order should be according to the order displayed during the table creation because the hastable does not preserve insertion order
	10-update TABLE_NAME set COLUMN_1 = VALUE_1, COLUMN_2 = VALUE_2,... where CLUSTERING_KEY_NAME = CLUSTERING_KEY_VALUE
	11-delete from TABLE_NAME where CONDITION_1 and CONDITION_2 ....
3-the parser does not support commenting or nested queries
4-the parser is not case sensitive
5-the conditions in the delete statement must have = as their operators
6-the conditions in the select statement could have >=,>,<,<=,=, or !=
7-the operators in the select statement could be AND, OR, or XOR
8-each sql statement must be passed to the console in one line (althouth the parser can support mutliple lines sql commands but not through the console)

