package essql.txtor;

import essql.Util;
import fj.*;
import fj.data.Option;
import fj.data.Validation;
import fj.function.Try2;
import fj.function.TryEffect3;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

/**
 * A mapping from a column in a resultset to a type. Use @see Field to read values from a resultset with column name.
 *
 * @param <A> The type the atom maps to and from
 */
public class Atom<A> {

    private final TryEffect3<A, PreparedStatement, Index, Exception> setParam;

    private final Try2<ResultSet, Index, A, Exception> read;

    public Atom(
            TryEffect3<A, PreparedStatement, Index, Exception> f,
            Try2<ResultSet, Index, A, Exception> read) {
        this.setParam = f;
        this.read = (rs, index) -> {
            try {
                return read.f( rs, index );
            } catch (Exception e) {
                throw new Exception( "Could not read value from resultset at index " + index.value + " / field " + rs.getMetaData().getColumnName( index.value ) + " : " + e.getMessage(), e );
            }
        };
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

    public static <A> Atom<Option<A>> optional(Atom<A> atom) {
        return new Atom<>(
                (Option<A> maybeA, PreparedStatement stmt, Index index) -> {
                    if (maybeA.isSome())
                        atom.setParam.f( maybeA.some(), stmt, index );
                    else
                        atom.setParam.f( null, stmt, index );
                },
                (ResultSet rs, Index index) -> {
                    if (rs.getObject( index.getValue() ) != null) {
                        return Option.some( atom.read.f( rs, index ) );
                    }
                    else
                        return Option.none();

                } );
    }

    /**
     * Reads a value of type A from a resultset.
     *
     * @param rs    The resultset the Atom reads from
     * @param index The index of the column the Atom reads from
     * @return A validation with either an exception or the translated value.
     */
    public Validation<Exception, A> read(ResultSet rs, Index index) {
        return Try.f( read ).f( rs, index );
    }

    /**
     * Creates a SetParam that sets the value a in a preparedstatement.
     *
     * @param a the value to set in the prepared statemement
     * @return a SetParam instance that sets the param on the resultset.
     */
    public SetParam set(A a) {
        return (PreparedStatement stmt, Index index) -> TryEffect.f( setParam ).f( a, stmt, index );
    }

    public <B> Atom<B> xmap(F<A, B> f, F<B, A> g) {
        return new Atom<>( (b, stmt, index) -> setParam.f( g.f( b ), stmt, index ), (rs, index) -> f.f( read.f( rs, index ) ) );
    }

    public interface SetParam {

        Validation<Exception, Unit> set(PreparedStatement stmt, Index index);

    }
}
