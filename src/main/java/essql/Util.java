package essql;

import fj.Show;
import fj.data.List;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class Util {

    public static <A> String mkString(Show<A> sa, List<A> as, String delim) {
        if (as.isEmpty())
            return "";
        if (as.length() == 1)
            return sa.showS( as.head() );
        else
            return sa.showS( as.head() ) + delim + mkString( sa, as.tail(), delim );
    }

    public static <A> String mkString(Show<A> sa, List<A> as, String before, String delim, String after) {
        return before + mkString( sa, as, delim ) + after;
    }

    public static <A extends Throwable> Show<A> throwableShow() {
        return Show.showS( Throwable::getMessage );
    }

    public static <A> Show<A> reflectionShow(){
        return Show.showS(t-> ToStringBuilder.reflectionToString( t, ToStringStyle.NO_FIELD_NAMES_STYLE ));
    }

}
