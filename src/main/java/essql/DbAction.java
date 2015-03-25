package essql;

import fj.F;
import fj.Try;
import fj.Unit;
import fj.data.NonEmptyList;
import fj.data.Validation;
import fj.function.Try1;

import java.sql.Connection;
import java.sql.SQLException;

public abstract class DbAction<A> {


    public static <A> DbAction<A> db(Try1<Connection, A, SQLException> f) {
        return new DbAction<A>() {
            @Override public Validation<NonEmptyList<SQLException>, A> run(Connection c) {
                return Try.f( f ).f( c ).nel();
            }
        };
    }

    public static <A> DbAction<A> dbV(F<Connection, Validation<NonEmptyList<SQLException>, A>> f) {
        return new DbAction<A>() {
            @Override public Validation<NonEmptyList<SQLException>, A> run(Connection c) {
                return f.f( c );
            }
        };
    }

    public abstract Validation<NonEmptyList<SQLException>, A> run(Connection c);


    public <B> DbAction<B> map(F<A,B> f){
        return new DbAction<B>() {
            @Override public Validation<NonEmptyList<SQLException>, B> run(Connection c) {
                return DbAction.this.run(c).map(f);
            }
        };
    }

    public <B> DbAction<B> bind(F<A,DbAction<B>> f){
        return new DbAction<B>() {
            @Override public Validation<NonEmptyList<SQLException>, B> run(Connection c) {
                Validation<NonEmptyList<SQLException>, DbAction<B>> v = DbAction.this.run(c).map(f);
                return v.bind(dbB -> dbB.run(c));
            }
        };
    }

    public DbAction<Unit> drain(){
        return this.map((A a) -> Unit.unit());
    }
}
