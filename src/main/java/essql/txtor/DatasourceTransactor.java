package essql.txtor;

import essql.Util;
import fj.data.NonEmptyList;
import fj.data.Validation;
import no.kantega.concurrent.Task;
import no.kantega.effect.Tried;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;

public class DatasourceTransactor extends Transactor {

    final DataSource ds;

    public DatasourceTransactor(DataSource ds) {
        this.ds = ds;
    }

    @Override public <A> Task<A> transact(DbAction<A> dbAction) {
        return Task.async( resolver -> {
            try {

                Connection c = ds.getConnection();
                try {
                    c.setAutoCommit( false );
                    Savepoint sp = c.setSavepoint();
                    Validation<NonEmptyList<Exception>, A> v =
                            dbAction.run( c );

                    if (v.isFail()) {
                        c.rollback( sp );
                        resolver.resolve( Tried.fail( new Exception( Util.mkString( Util.<Exception>throwableShow(), v.fail().toList(), ", " ) ) ) );
                    }
                    else {
                        c.commit();
                        resolver.resolve( Tried.value( v.success() ) );
                    }
                } finally {
                    c.close();
                }

            } catch (SQLException e) {
                resolver.resolve( Tried.fail( e ) );
            }
        } );


    }
}
