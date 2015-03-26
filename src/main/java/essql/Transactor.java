package essql;

import no.kantega.concurrent.Async;

public abstract class Transactor {

    public abstract <A> Async<A> transact(DbAction<A> dbAction);
}
