package essql.txtor;

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
import java.sql.Timestamp;
import java.time.Instant;

public class Atom<A> {

    private final TryEffect3<A, PreparedStatement, Index, Exception> setParam;

    private final Try2<ResultSet, Index, A, Exception> read;

    public Atom(
            TryEffect3<A, PreparedStatement, Index, Exception> f,
            Try2<ResultSet, Index, A, Exception> read) {
        this.setParam = f;
        this.read = read;
    }

    public static Atom<String> string =
            new Atom<>(
                    (a, stmt, index) -> stmt.setString( index.value, a ),
                    (rs, index) -> rs.getString( index.value ) );


    public static Atom<Integer> num =
            new Atom<>(
                    (a, stmt, index) -> stmt.setInt( index.value, a ),
                    (rs, index) -> rs.getInt( index.value ) );


    public static Atom<Instant> timestamp =
            new Atom<>(
                    (a, stmt, index) -> stmt.setTimestamp( index.value, new Timestamp( a.toEpochMilli() ) ),
                    (rs, index) -> rs.getTimestamp( index.value ).toInstant() );

    public Validation<Exception, A> read(ResultSet rs, Index index) {
        return Try.f( read ).f( rs, index );
    }

    public SetParam set(A a) {
        return (PreparedStatement stmt, Index index) -> TryEffect.f( setParam ).f( a, stmt, index );
    }

    public <B> Atom<B> xmap(F<A, B> f, F<B, A> g) {
        return new Atom<>( (b, stmt, index) -> setParam.f( g.f( b ), stmt, index ), (rs, index) -> f.f( read.f( rs, index ) ) );
    }

    public static interface SetParam {

        public Validation<Exception, Unit> set(PreparedStatement stmt, Index index);

    }
}
