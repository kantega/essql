package essql.builder;

public class Table extends DBObject<Table> {

	public Table(String expression) {
		super(expression);
	}

	public static Table table(String table) {
		return new Table(table);
	}

	public Field field(String fieldName) {
		if (hasAlias()) {
			return Field.field(alias,fieldName);
		}
		return Field.field(expression, fieldName);
	}
}
