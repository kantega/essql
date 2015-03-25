package essql;

import fj.data.NonEmptyList;
import fj.data.Validation;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class DatasourceTransactor extends Transactor {

    final DataSource ds;

    public DatasourceTransactor(DataSource ds) {
        this.ds = ds;
    }

    @Override public <A> CompletionStage<A> transact(DbAction<A> dbAction) {
        CompletableFuture<A> f = new CompletableFuture<>();
        try{

            Connection c = ds.getConnection();
            c.setAutoCommit( false );
            Savepoint sp = c.setSavepoint();
            Validation<NonEmptyList<SQLException>,A> v =
                    dbAction.run( c );

            if(v.isFail()) {
                c.rollback( sp );
                f.completeExceptionally( new Exception( Util.mkString( Util.<SQLException>throwableShow(), v.fail().toList(), ", " ) ));
            }
            else {
                c.commit();
                f.complete( v.success() );
            }
        }catch (SQLException e){
            f.completeExceptionally( e );
        }

        return f;
    }
}
