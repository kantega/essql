package essql;

import fj.*;

public class Products {


    public static <A, B> P2<A, B> tuple2(A a, B b) {
        return P.p( a, b );
    }

    public static <A, B, C> P3<A, B, C> flatten3(P2<A, P2<B, C>> nested) {
        return P.p( nested._1(), nested._2()._1(), nested._2()._2() );
    }

    public static <A, B, C> P3<A, B, C> tuple3(A a, B b, C c) {
        return P.p( a, b, c );
    }

    public static <A, B, C, D> P4<A, B, C, D> flatten4(P2<A, P2<B, P2<C, D>>> nested) {
        P3<B, C, D> p3 = flatten3( nested._2() );
        return P.p( nested._1(), p3._1(), p3._2(), p3._3() );
    }

    public static <A, B, C, D> P4<A, B, C, D> tuple4(A a, B b, C c, D d) {
        return P.p( a, b, c, d );
    }

    public static <A, B, C, D, E> P5<A, B, C, D, E> flatten5(P2<A, P2<B, P2<C, P2<D, E>>>> nested) {
        P4<B, C, D, E> p4 = flatten4( nested._2() );
        return P.p( nested._1(), p4._1(), p4._2(), p4._3(), p4._4() );
    }

    public static <A, B, C, D, E> P5<A, B, C, D, E> tuple5(A a, B b, C c, D d, E e) {
        return P.p( a, b, c, d, e );
    }

    public static <A, B, C, D, E, FF> P6<A, B, C, D, E, FF> flatten6(P2<A, P2<B, P2<C, P2<D, P2<E, FF>>>>> nested) {
        P5<B, C, D, E, FF> p5 = flatten5( nested._2() );
        return P.p( nested._1(), p5._1(), p5._2(), p5._3(), p5._4(), p5._5() );
    }

    public static <A, B, C, D, E, G> P6<A, B, C, D, E, G> tuple6(A a, B b, C c, D d, E e, G f) {
        return P.p( a, b, c, d, e, f );
    }

    public static <A, B, C, D, E, FF, G> P7<A, B, C, D, E, FF, G> flatten7(P2<A, P2<B, P2<C, P2<D, P2<E, P2<FF, G>>>>>> nested) {
        P6<B, C, D, E, FF, G> p6 = flatten6( nested._2() );
        return P.p( nested._1(), p6._1(), p6._2(), p6._3(), p6._4(), p6._5(), p6._6() );
    }

    public static <A, B, C, D, E, G, H> P7<A, B, C, D, E, G, H> tuple7(A a, B b, C c, D d, E e, G g, H h) {
        return P.p( a, b, c, d, e, g, h );
    }

    public static <A, B, C> F<P2<A, B>, C> tupleF2(F2<A, B, C> f) {
        return F2Functions.tuple( f );
    }

    public static <A, B, C, D> F<P3<A, B, C>, D> tupleF3(F3<A, B, C, D> f) {
        return t3 -> f.f( t3._1(), t3._2(), t3._3() );
    }

    public static <A, B, C, D, E> F<P4<A, B, C, D>, E> tupleF4(F4<A, B, C, D, E> f) {
        return t4 -> f.f( t4._1(), t4._2(), t4._3(), t4._4() );
    }

    public static <A, B, C, D, E,FF> F<P5<A, B, C, D,E>, FF> tupleF5(F5<A, B, C, D, E,FF> f) {
        return t5 -> f.f( t5._1(), t5._2(), t5._3(), t5._4(), t5._5() );
    }

    public static <A, B, C, D, E,FF,G> F<P6<A, B, C, D,E,FF>, G> tupleF6(F6<A, B, C, D, E,FF,G> f) {
        return t6 -> f.f( t6._1(), t6._2(), t6._3(), t6._4(), t6._5(), t6._6() );
    }

    public static <A, B, C, D, E,FF,G,H> F<P7<A, B, C, D,E,FF,G>, H> tupleF7(F7<A, B, C, D, E,FF,G,H> f) {
        return t7 -> f.f( t7._1(), t7._2(), t7._3(), t7._4(), t7._5(), t7._6(),t7._7() );
    }
}
