package essql.txtor;

public class Index {
    public final int value;

    public static Index Index(int i){
        return new Index(i);
    }

    public Index(int value) {
        this.value = value;
    }
}
