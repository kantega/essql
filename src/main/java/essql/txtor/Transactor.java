package essql.txtor;

import no.kantega.concurrent.Task;

public abstract class Transactor {

    public abstract <A> Task<A> transact(DbAction<A> dbAction);
}
