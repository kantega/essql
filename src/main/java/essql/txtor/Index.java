package essql.txtor;

/**
 * Wraps a index.
 */
public class Index {
    public final int value;

    public static Index Index(int i) {
        return new Index( i );
    }

    public Index(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public Index increment() {
        return new Index( value + 1 );
    }

    public Index plus(int value){
        return new Index(value + this.value);
    }
}
