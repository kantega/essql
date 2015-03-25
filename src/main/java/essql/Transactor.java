package essql;

import java.util.concurrent.CompletionStage;

public abstract class Transactor {

    public abstract <A> CompletionStage<A> transact(DbAction<A> dbAction);
}
