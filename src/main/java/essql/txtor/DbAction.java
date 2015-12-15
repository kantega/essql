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

import java.sql.*;
import java.util.ArrayList;

/**
 * A DbAction represents an operation on a jdbc connection. It an
 *
 * @param <A> The type the DbAction yields when run
 */
public abstract class DbAction<A> {


    /**
     * Creates a DbAction around a function that creates an A from a Connection. The function might throw an Exeption.
     *
     * @param f   an effect that yields an A or throws an exception
     * @param <A> The type of the returned value
     * @return An action that runs the effect when a connection is obtained.
     */
    public static <A> DbAction<A> db(Try1<Connection, A, Exception> f) {
        return new DbAction<A>() {
            @Override
            public Validation<NonEmptyList<Exception>, A> run(Connection c) {
                return Try.f(f).f(c).nel();
            }
        };
    }

    /**
     * Creates a DbAction around a function that yields a Validation
     *
     * @param f   an effect that yields a validation
     * @param <A> The type of the result
     * @return Am action that yields As
     */
    public static <A> DbAction<A> dbV(F<Connection, Validation<NonEmptyList<Exception>, A>> f) {
        return new DbAction<A>() {
            @Override
            public Validation<NonEmptyList<Exception>, A> run(Connection c) {
                return f.f(c);
            }
        };
    }

    /**
     * Creates a QueryBuilder that uses the provided sql to create a preparedstatement, and injects the SetParam values into the statement.
     *
     * @param sql       The sql statement.
     * @param setParams The values to inject into the preparedstatement
     * @return A builder.
     */
    public static QueryBuilder prepare(String sql, SetParam... setParams) {
        return new QueryBuilder(sql, setParams);
    }


    public static PreparedStatement setParams(PreparedStatement stmt, SetParam[] params) {
        for (int i = 0; i < params.length; i++) {
            params[i].set(stmt, new Index(i + 1));
        }
        return stmt;
    }

    /**
     * Runs the action, invoking any sideeffects.
     *
     * @param c the connection to run the action on
     * @return A validation with a list of failures or a result
     */
    public abstract Validation<NonEmptyList<Exception>, A> run(Connection c);


    /**
     * Map the result of the action over the function f.
     *
     * @param f   The mapping function
     * @param <B> The type the function yields
     * @return A new action that applies f on result of the database action.
     */
    public <B> DbAction<B> map(F<A, B> f) {
        return new DbAction<B>() {
            @Override
            public Validation<NonEmptyList<Exception>, B> run(Connection c) {
                return DbAction.this.run(c).map(f);
            }
        };
    }

    /**
     * Binds the action to the next action that is produced by f.
     *
     * @param f   A function that produces the next action
     * @param <B> The type variable of the next action.
     * @return A new action that runs both actions in sequence, yielding a B
     */
    public <B> DbAction<B> bind(F<A, DbAction<B>> f) {
        return new DbAction<B>() {
            @Override
            public Validation<NonEmptyList<Exception>, B> run(Connection c) {
                Validation<NonEmptyList<Exception>, DbAction<B>> v = DbAction.this.run(c).map(f);
                return v.bind(dbB -> dbB.run(c));
            }
        };
    }

    /**
     * Ignore the output from the action
     *
     * @return the same action but yielding Unit
     */
    public DbAction<Unit> drain() {
        return this.map((A a) -> Unit.unit());
    }

    /**
     * DSL to build a DbAction.
     */
    public static class QueryBuilder {

        final String sql;

        final SetParam[] params;

        public QueryBuilder(String sql, SetParam[] params) {
            this.sql = sql;
            this.params = params;
        }

        /**
         * Creates an update with PreparedStatement.executeUpdate()
         *
         * @return An action that performs an executeupdate on a preparestatement.
         */
        public DbAction<Integer> update() {
            return DbAction.db(connx -> setParams(connx.prepareStatement(sql), params).executeUpdate());
        }

        /**
         * Creates an update with PreparedStatement.executeUpdate()
         *
         * @return An action that performs an executeupdate on a preparestatement.
         */
        public DbAction<List<Long>> insertGetKeys() {
            return DbAction.db(connx -> {
                PreparedStatement stmt = setParams(connx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS), params);
                ResultSet rs = stmt.getGeneratedKeys();
                ArrayList<Long> keys = new ArrayList<>();
                while(rs.next()){
                    keys.add(rs.getLong(1));
                }
                rs.close();
                return Java.<Long>ArrayList_List().f(keys);

            });
        }

        /**
         * Queries the db with PreparedStatement.executeQuery(), using a mapper to translate one row to one A
         *
         * @param mapper a mapper that maps FROM ONE ROW to ONE A. Do not iterate through the resultset here.
         * @param <A>    The type of the content the Action returns a list of.
         * @return an action the yields a List of A's
         */
        public <A> DbAction<List<A>> query(Try1<ResultSet, A, Exception> mapper) {
            return DbAction.dbV(connx -> {
                try {
                    ResultSet rs = setParams(connx.prepareStatement(sql), params).executeQuery();

                    ArrayList<A> array = new ArrayList<>();
                    ArrayList<Exception> failures = new ArrayList<>();
                    while (rs.next()) {
                        try {
                            A a = mapper.f(rs);
                            array.add(a);
                        } catch (Exception e) {
                            failures.add(e);
                        }
                    }

                    if (failures.isEmpty())
                        return Validation.success(Java.<A>ArrayList_List().f(array));
                    else {
                        Exception head = failures.remove(0);
                        return Validation.fail(NonEmptyList.nel(head, Java.<Exception>ArrayList_List().f(failures)));
                    }
                } catch (Exception e) {
                    return Validation.<Exception, List<A>>fail(e).nel();
                }

            });
        }

        /**
         * Queries the db with PreparedStatement.executeQuery(), using a composite to translate one row to one A
         *
         * @param comp A composite that maps rows to objects
         * @param <A>  The type of the content the Action returns a list of.
         * @return an action the yields a List of A's
         */
        public <A> DbAction<List<A>> query(Composite<A> comp) {
            return DbAction.dbV(connx -> {
                try {
                    ResultSet rs = setParams(connx.prepareStatement(sql), params).executeQuery();

                    ArrayList<A> array = new ArrayList<>();
                    ArrayList<Exception> failures = new ArrayList<>();
                    while (rs.next()) {
                        Validation<NonEmptyList<Exception>, A> v = comp.read(rs);
                        if (v.isFail())
                            failures.addAll(v.fail().toCollection());
                        else
                            array.add(v.success());
                    }

                    if (failures.isEmpty())
                        return Validation.success(Java.<A>ArrayList_List().f(array));
                    else {
                        Exception head = failures.remove(0);
                        return Validation.fail(NonEmptyList.nel(head, Java.<Exception>ArrayList_List().f(failures)));
                    }
                } catch (Exception e) {
                    return Validation.<Exception, List<A>>fail(e).nel();
                }

            });
        }


    }
}
