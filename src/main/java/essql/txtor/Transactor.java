package essql.txtor;

import no.kantega.concurrent.Task;

/**
 * Interface for types that run a DbAction in a database transaction.
 */
public abstract class Transactor {

    public abstract <A> Task<A> transact(DbAction<A> dbAction);
}
