package essql;

import essql.Atom.SetParam;
import fj.data.Java;
import fj.data.List;
import fj.data.NonEmptyList;
import fj.data.Validation;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class Query {


    public static QueryBuilder prepare(String sql, SetParam... setParams) {
        return new QueryBuilder( sql, setParams );
    }

    public static class QueryBuilder {

        final String sql;

        final SetParam[] params;

        public QueryBuilder(String sql, SetParam[] params) {
            this.sql = sql;
            this.params = params;
        }


        public DbAction<Integer> update() {
            return DbAction.db( connx -> setParams( connx.prepareStatement( sql ), params ).executeUpdate() );
        }

        public <A> DbAction<List<A>> query(Composite<A> comp) {
            return DbAction.dbV( connx -> {
                try {
                    ResultSet rs = setParams( connx.prepareStatement( sql ), params ).executeQuery();


                    ArrayList<A> array = new ArrayList<>();
                    ArrayList<SQLException> failures = new ArrayList<>();
                    while (rs.next()) {
                        Validation<NonEmptyList<SQLException>, A> v = comp.read( rs );
                        if (v.isFail())
                            failures.addAll( v.fail().toCollection() );
                        else
                            array.add( v.success() );
                    }

                    if (failures.isEmpty())
                        return Validation.success( Java.<A>ArrayList_List().f( array ) );
                    else {
                        SQLException head = failures.remove( 0 );
                        return Validation.fail( NonEmptyList.nel( head, Java.<SQLException>ArrayList_List().f( failures ) ) );
                    }
                } catch (SQLException e) {
                    return Validation.<SQLException, List<A>>fail( e ).nel();
                }

            } );
        }


    }

    private static PreparedStatement setParams(PreparedStatement stmt, SetParam[] params) {
        for (int i = 0; i < params.length; i++) {
            params[i].set( stmt, new Index( i + 1 ) );
        }
        return stmt;
    }


}
