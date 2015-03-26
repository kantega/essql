package essql;

import fj.data.NonEmptyList;
import fj.data.Validation;
import no.kantega.concurrent.Async;
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

    @Override public <A> Async<A> transact(DbAction<A> dbAction) {
        return Async.async( resolver -> {
            try {

                Connection c = ds.getConnection();
                c.setAutoCommit( false );
                Savepoint sp = c.setSavepoint();
                Validation<NonEmptyList<SQLException>, A> v =
                        dbAction.run( c );

                if (v.isFail()) {
                    c.rollback( sp );
                    resolver.resolve( Tried.fail( new Exception( Util.mkString( Util.<SQLException>throwableShow(), v.fail().toList(), ", " ) ) ) );
                }
                else {
                    c.commit();
                    resolver.resolve( Tried.value( v.success() ) );
                }
            } catch (SQLException e) {
                resolver.resolve( Tried.fail( e ) );
            }
        } );


    }
}
