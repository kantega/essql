package essql;

import essql.txtor.Atom;
import essql.txtor.Index;
import fj.data.Validation;

import java.sql.ResultSet;

public class Field {


    public static <A> A readField(String name, Atom<A> atom, ResultSet rs) throws Exception {
        Validation<Exception, A> v = atom.read( rs, new Index( rs.findColumn( name ) ) );

        if (v.isSuccess())
            return v.success();
        else
            throw v.fail();
    }
}
