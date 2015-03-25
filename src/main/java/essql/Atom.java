package essql;

import fj.F;
import fj.Try;
import fj.TryEffect;
import fj.Unit;
import fj.data.Validation;
import fj.function.Try2;
import fj.function.TryEffect3;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Atom<A> {

    private final TryEffect3<A, PreparedStatement, Index, SQLException> setParam;

    private final Try2<ResultSet, Index, A, SQLException> read;

    public Atom(
            TryEffect3<A, PreparedStatement, Index, SQLException> f,
            Try2<ResultSet, Index, A, SQLException> read) {
        this.setParam = f;
        this.read = read;
    }

    public static Atom<String> string =
            new Atom<>(
                    (a, stmt, index) -> stmt.setString( index.value, a ),
                    (rs, index) -> rs.getString( index.value ) );


    public static Atom<Integer> num =
            new Atom<>(
                    (a, stmt, index) -> stmt.setInt( index.value, a ), (rs, index) -> rs.getInt( index.value ) );


    public Validation<SQLException, A> read(ResultSet rs, Index index) {
        return Try.f( read ).f( rs, index );
    }

    public SetParam set(A a) {
        return (PreparedStatement stmt, Index index) -> TryEffect.f( setParam ).f( a, stmt, index );
    }

    public <B> Atom<B> xmap(F<A, B> f, F<B, A> g) {
        return new Atom<>( (b, stmt, index) -> setParam.f( g.f( b ), stmt, index ), (rs, index) -> f.f( read.f( rs, index ) ) );
    }

    public static interface SetParam {

        public Validation<SQLException, Unit> set(PreparedStatement stmt, Index index);

    }
}
