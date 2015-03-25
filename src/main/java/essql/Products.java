package essql;

import fj.*;

public class Products {


    public static <A, B> P2<A, B> tuple2(A a, B b) {
        return P.p( a, b );
    }

    public static <A, B, C> P3<A, B, C> tuple3(A a, B b, C c) {
        return P.p( a, b, c );
    }

    public static <A, B, C, D> P4<A, B, C, D> tuple4(A a, B b, C c, D d) {
        return P.p( a, b, c, d );
    }

    public static <A, B, C, D, E> P5<A, B, C, D, E> tuple5(A a, B b, C c, D d, E e) {
        return P.p( a, b, c, d, e );
    }

    public static <A, B, C, D, E, G> P6<A, B, C, D, E, G> tuple6(A a, B b, C c, D d, E e, G f) {
        return P.p( a, b, c, d, e, f );
    }

    public static <A, B, C, D, E, G, H> P7<A, B, C, D, E, G, H> tuple7(A a, B b, C c, D d, E e, G g, H h) {
        return P.p( a, b, c, d, e, g, h );
    }
}
