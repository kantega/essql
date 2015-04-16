package essql;

import essql.txtor.Atom;
import essql.txtor.Index;
import fj.*;
import fj.data.NonEmptyList;
import fj.data.Validation;

import java.sql.ResultSet;
import java.sql.SQLException;

import static essql.Products.*;
import static fj.P.*;
import static fj.P2.*;

public class Composite<A> {

    final F2<ResultSet, Index, Validation<NonEmptyList<SQLException>, P2<A, Index>>> f;

    protected Composite(F2<ResultSet, Index, Validation<NonEmptyList<SQLException>, P2<A, Index>>> f) {
        this.f = f;
    }

    public static <A> Composite<A> composite(F2<ResultSet, Index, Validation<NonEmptyList<SQLException>, P2<A, Index>>> f) {
        return new Composite<>( f );
    }


    public static <A> Composite<A> comp(Atom<A> a) {
        return composite( (rs, index) -> a.read( rs, index.increment() ).nel().map( val -> p( val, index.increment() ) ) );
    }

    public static <A, B> Composite<P2<A, B>> comp(Atom<A> a, Atom<B> b) {
        return comp( a ).then( comp( b ) );
    }

    public static <A, B, C> Composite<C> comp(Atom<A> a, Atom<B> b, F2<A, B, C> f) {
        return comp( a, b ).map( tuple( f ) );
    }

    public static <A, B, C> Composite<P3<A, B, C>> comp(Atom<A> a, Atom<B> b, Atom<C> c) {
        return comp( a ).then( comp( b ).then( comp( c ) ) ).map( Products::flatten3 );
    }

    public static <A, B, C, D> Composite<D> comp(Atom<A> a, Atom<B> b, Atom<C> c, F3<A, B, C, D> f) {
        return comp( a, b, c ).map( tupleF3( f ) );
    }

    public static <A, B, C, D> Composite<P4<A, B, C, D>> comp(Atom<A> a, Atom<B> b, Atom<C> c, Atom<D> d) {
        return comp( a ).then( comp( b ).then( comp( c ).then( comp( d ) ) ) ).map( Products::flatten4 );
    }

    public static <A, B, C, D, E> Composite<E> comp(Atom<A> a, Atom<B> b, Atom<C> c, Atom<D> d, F4<A, B, C, D, E> f) {
        return comp( a, b, c, d ).map( Products.tupleF4( f ) );
    }

    public static <A, B, C, D, E> Composite<P5<A, B, C, D, E>> comp(Atom<A> a, Atom<B> b, Atom<C> c, Atom<D> d, Atom<E> e) {
        return comp( a ).then( comp( b ).then( comp( c ).then( comp( d ).then( comp( e ) ) ) ) ).map( Products::flatten5 );
    }

    public static <A, B, C, D, E, FF> Composite<FF> comp(Atom<A> a, Atom<B> b, Atom<C> c, Atom<D> d, Atom<E> e, F5<A, B, C, D, E, FF> f) {
        return comp( a, b, c, d, e ).map( Products.tupleF5( f ) );
    }

    public static <A, B, C, D, E, FF> Composite<P6<A, B, C, D, E, FF>> comp(Atom<A> a, Atom<B> b, Atom<C> c, Atom<D> d, Atom<E> e, Atom<FF> ff) {
        return comp( a ).then( comp( b ).then( comp( c ).then( comp( d ).then( comp( e ).then( comp( ff ) ) ) ) ) ).map( Products::flatten6 );
    }

    public static <A, B, C, D, E, FF, G> Composite<G> comp(Atom<A> a, Atom<B> b, Atom<C> c, Atom<D> d, Atom<E> e, Atom<FF> ff, F6<A, B, C, D, E, FF, G> f) {
        return comp( a, b, c, d, e, ff ).map( Products.tupleF6( f ) );
    }

    public <B> Composite<B> map(F<A, B> g) {
        return new Composite<>( (rs, index) -> f.f( rs, index ).map( map1_( g ) ) );
    }

    public <B> Composite<P2<A, B>> then(Composite<B> other) {
        return this.flatMap( a -> other.map( b -> p( a, b ) ) );
    }

    public <B> Composite<B> flatMap(F<A, Composite<B>> g) {
        return new Composite<>( (rs, index) ->
                this.f.f( rs, index ).bind( (aAndIndex) -> g.f( aAndIndex._1() ).f.f( rs, aAndIndex._2() ) ) );
    }

    public Validation<NonEmptyList<SQLException>, A> read(ResultSet rs) {
        return f.f( rs, Index.Index( 0 ) ).map( __1() );
    }

}
