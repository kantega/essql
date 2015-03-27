package essql;

import essql.txtor.Atom;
import essql.txtor.Index;
import fj.*;
import fj.data.NonEmptyList;
import fj.data.Validation;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Composite<A> {

    final F<ResultSet, Validation<NonEmptyList<SQLException>, A>> f;

    private static final Semigroup<NonEmptyList<SQLException>> sqlExNelsg =
            Semigroup.<SQLException>nonEmptyListSemigroup();

    public static <A> Composite<A> composite(F<ResultSet, Validation<NonEmptyList<SQLException>, A>> f) {
        return new Composite<>( f );
    }

    protected Composite(F<ResultSet, Validation<NonEmptyList<SQLException>, A>> f) {
        this.f = f;
    }

    public static <A> Composite<A> comp(Atom<A> a) {
        return composite( rs -> a.read( rs, new Index( 1 ) ).nel() );
    }

    public static <A, B> Composite<P2<A, B>> comp(Atom<A> a, Atom<B> b) {
        return Composite.<P2<A, B>>composite( rs -> a.read( rs, Index.Index( 1 ) ).nel().accumulate( sqlExNelsg, b.read( rs, Index.Index( 2 ) ).nel(), Products::tuple2 ) );
    }

    public static <A, B, C> Composite<C> comp(Atom<A> a, Atom<B> b, F2<A, B, C> f) {
        return comp( a, b ).map( P2.tuple( f ) );
    }

    public static <A, B, C> Composite<P3<A, B, C>> comp(Atom<A> a, Atom<B> b, Atom<C> c) {
        return Composite.<P3<A, B, C>>composite( rs -> a.read( rs, Index.Index( 1 ) ).nel().accumulate( sqlExNelsg, b.read( rs, Index.Index( 2 ) ).nel(), c.read( rs, Index.Index( 3 ) ).nel(), Products::tuple3 ) );
    }

    public static <A, B, C, D> Composite<D> comp(Atom<A> a, Atom<B> b, Atom<C> c, F3<A, B, C, D> f) {
        return comp( a, b, c ).map( t3 -> f.f( t3._1(), t3._2(), t3._3() ) );
    }

    public static <A, B, C, D> Composite<P4<A, B, C, D>> comp(Atom<A> a, Atom<B> b, Atom<C> c, Atom<D> d) {
        return Composite.<P4<A, B, C, D>>composite( rs -> a.read( rs, Index.Index( 1 ) ).nel().accumulate( sqlExNelsg, b.read( rs, Index.Index( 2 ) ).nel(), c.read( rs, Index.Index( 3 ) ).nel(), d.read( rs, Index.Index( 4 ) ).nel(), Products::tuple4 ) );
    }

    public <B> Composite<B> map(F<A, B> g) {
        return new Composite<B>( Function.andThen( f, v -> v.map( g ) ) );
    }

    public Validation<NonEmptyList<SQLException>, A> read(ResultSet rs) {
        return f.f( rs );
    }

}
