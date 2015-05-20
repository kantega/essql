package essql.txtor;

import essql.Composite;
import essql.txtor.Atom.SetParam;
import fj.F;
import fj.Try;
import fj.Unit;
import fj.data.Java;
import fj.data.List;
import fj.data.NonEmptyList;
import fj.data.Validation;
import fj.function.Try1;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public abstract class DbAction<A> {


    public static <A> DbAction<A> db(Try1<Connection, A, Exception> f) {
        return new DbAction<A>() {
            @Override public Validation<NonEmptyList<Exception>, A> run(Connection c) {
                return Try.f( f ).f( c ).nel();
            }
        };
    }

    public static <A> DbAction<A> dbV(F<Connection, Validation<NonEmptyList<Exception>, A>> f) {
        return new DbAction<A>() {
            @Override public Validation<NonEmptyList<Exception>, A> run(Connection c) {
                return f.f( c );
            }
        };
    }

    public static QueryBuilder prepare(String sql, SetParam... setParams) {
        return new QueryBuilder( sql, setParams );
    }


    private static PreparedStatement setParams(PreparedStatement stmt, SetParam[] params) {
        for (int i = 0; i < params.length; i++) {
            params[i].set( stmt, new Index( i + 1 ) );
        }
        return stmt;
    }

    public abstract Validation<NonEmptyList<Exception>, A> run(Connection c);


    public <B> DbAction<B> map(F<A, B> f) {
        return new DbAction<B>() {
            @Override public Validation<NonEmptyList<Exception>, B> run(Connection c) {
                return DbAction.this.run( c ).map( f );
            }
        };
    }

    public <B> DbAction<B> bind(F<A, DbAction<B>> f) {
        return new DbAction<B>() {
            @Override public Validation<NonEmptyList<Exception>, B> run(Connection c) {
                Validation<NonEmptyList<Exception>, DbAction<B>> v = DbAction.this.run( c ).map( f );
                return v.bind( dbB -> dbB.run( c ) );
            }
        };
    }

    public DbAction<Unit> drain() {
        return this.map( (A a) -> Unit.unit() );
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

        public <A> DbAction<List<A>> query(F<ResultSet,A> mapper){

        }

        public <A> DbAction<List<A>> query(Composite<A> comp) {
            return DbAction.dbV( connx -> {
                try {
                    ResultSet rs = setParams( connx.prepareStatement( sql ), params ).executeQuery();

                    ArrayList<A> array = new ArrayList<>();
                    ArrayList<Exception> failures = new ArrayList<>();
                    while (rs.next()) {
                        Validation<NonEmptyList<Exception>, A> v = comp.read( rs );
                        if (v.isFail())
                            failures.addAll( v.fail().toCollection() );
                        else
                            array.add( v.success() );
                    }

                    if (failures.isEmpty())
                        return Validation.success( Java.<A>ArrayList_List().f( array ) );
                    else {
                        Exception head = failures.remove( 0 );
                        return Validation.fail( NonEmptyList.nel( head, Java.<Exception>ArrayList_List().f( failures ) ) );
                    }
                } catch (Exception e) {
                    return Validation.<Exception, List<A>>fail( e ).nel();
                }

            } );
        }
    }
}
