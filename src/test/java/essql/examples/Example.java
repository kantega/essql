package essql.examples;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import essql.Composite;
import essql.Field;
import essql.Util;
import essql.examples.User.Channel;
import essql.txtor.Atom;
import essql.txtor.DatasourceTransactor;
import essql.txtor.DbAction;
import fj.Show;
import fj.Unit;
import fj.control.parallel.Strategy;
import fj.data.List;
import fj.data.Option;
import fj.data.Stream;
import no.kantega.concurrent.Task;
import no.kantega.effect.Tried;
import org.apache.commons.lang3.StringUtils;

import static essql.Composite.comp;
import static essql.examples.User.Channel.email;
import static essql.examples.User.Channel.phone;
import static essql.examples.User.User;
import static essql.txtor.Atom.num;
import static essql.txtor.Atom.string;
import static fj.data.List.list;

public class Example {
    public static void main(String[] args) throws InterruptedException {
        Task.defaultStrategy = Strategy.seqStrategy();

        HikariConfig config = new HikariConfig();
        config.setDriverClassName("org.h2.Driver");
        config.setJdbcUrl("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
        config.setUsername("da");
        config.setPassword("");
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");

        HikariDataSource ds = new HikariDataSource(config);

        DatasourceTransactor tx = new DatasourceTransactor(ds);

        Show<Tried<?>> triedShow =
                Show.stringShow.contramap((Tried<?> tried) -> tried.fold(Throwable::getMessage, Object::toString));

        Atom<List<Channel>> channelAtom =
                string.xmap(
                        str -> Stream.stream(StringUtils.split(str, ";")).map((String elem) -> elem.contains("@") ? email(elem) : phone(elem)).toList(),
                        channels -> Util.mkString(Channel.valueShow, channels, ";"));

        Atom<User.Comment> commentAtom =
                string.xmap(User.Comment::new, User.Comment::getValue);

        User leif = User("Leif Haraldson", 34, list(email("leif@tesgin.com"), phone("+47 30 24 04 55")));


        DbAction<Integer> createTable = DbAction.prepare(
                "CREATE TABLE user(" +
                        "id INT AUTO_INCREMENT NOT NULL," +
                        "name VARCHAR(255) NOT NULL," +
                        "channels TEXT NOT NULL," +
                        "age INT NOT NULL," +
                        "maybeText VARCHAR(255), " +
                        "PRIMARY KEY(id)" +
                        ")").update();


        DbAction<Integer> addLeif =
                DbAction
                        .prepare("INSERT INTO user (name,age,channels) VALUES (?,?,?)", string.set(leif.name), num.set(leif.age), channelAtom.set(leif.channels))
                        .update();

        DbAction<List<User>> getLeif =
                DbAction.prepare("SELECT name,age,channels,maybeText FROM user WHERE name = ?", string.set(leif.name))
                        .query(comp(string, num, channelAtom, Atom.optional(commentAtom), User::new));


        tx.transact(createTable)
                .flatMap(r -> tx.transact(addLeif))
                .flatMap(rr -> tx.transact(getLeif))
                .map((List<User> list) -> Show.listShow(Util.<User>reflectionShow()).showS(list))
                .execute(prepend("* Creating table, adding one user, fetching the user and printing the results\n", triedShow)::println);


        Composite<User> userComp =
                comp(Atom.string).flatMap(
                        name -> comp(Atom.num).flatMap(
                                age -> comp(channelAtom).flatMap(
                                        channels -> comp(Atom.optional(commentAtom)).map(
                                                maybeComment -> new User(name, age, channels, maybeComment)
                                        )
                                )
                        )
                );

        DbAction<List<User>> getLeifWithComp =
                DbAction.prepare("SELECT name,age,channels,maybeText FROM user WHERE name = ?", string.set(leif.name))
                        .query(userComp);

        tx.transact(getLeifWithComp)
                .map((List<User> list) -> Show.listShow(Util.<User>reflectionShow()).showS(list))
                .execute(prepend("* Fetching the user with a Comp<User> type\n", triedShow)::println);


        DbAction<Unit> updateUser =
                DbAction.prepare("UPDATE user SET age = 35 WHERE age = 34").update().drain();

        tx.transact(updateUser.bind(x -> getLeifWithComp)).map((List<User> list) -> Show.listShow(Util.<User>reflectionShow()).showS(list))
                .execute(prepend("* Updating the user, then fetching the user with a Comp<User> type\n", triedShow)::println);


        DbAction<List<User>> getUserWithMaualMapping =
                DbAction
                        .prepare("SELECT name,age,channels,maybeText FROM user WHERE name = ?", string.set(leif.name))
                        .query(rs -> {
                            String name = Field.readField("name", Atom.string, rs);
                            int age = Field.readField("age", Atom.num, rs);
                            List<Channel> channels = Field.readField("channels", channelAtom, rs);
                            return User(name, age, channels);
                        });

        tx.transact(getUserWithMaualMapping)
                .map((List<User> list) -> Show.listShow(Util.<User>reflectionShow()).showS(list))
                .execute(prepend("* Getting the user with manual rs to type mapping\n", triedShow)::println);

        DbAction<List<User>> failingGetUserWithMaualMapping =
                DbAction
                        .prepare("SELECT name,age,channels,maybeText FROM user WHERE name = ?", string.set(leif.name))
                        .query(rs -> {
                            String name = Field.readField("name", Atom.string, rs);
                            int age = Field.readField("name", Atom.num, rs);
                            List<Channel> channels = Field.readField("channels", channelAtom, rs);
                            return User(name, age, channels);
                        });

        tx.transact(failingGetUserWithMaualMapping)
                .map((List<User> list) -> Show.listShow(Util.<User>reflectionShow()).showS(list))
                .execute(prepend("* Running a query where the object type and databse type does not align\n", triedShow)::println);


        DbAction<Unit> updateNoText =
                DbAction.prepare("UPDATE user SET maybeText = ? WHERE age = 35", Atom.optional(commentAtom).set(Option.some(new User.Comment("jalla")))).update().drain();

        tx.transact(updateNoText.bind(u -> getLeif))
                .map((List<User> list) -> Show.listShow(Util.<User>reflectionShow()).showS(list))
                .execute(prepend("* Running an update with a Some<Comment>\n", triedShow)::println);

        DbAction<Unit> updateText =
                DbAction.prepare("UPDATE user SET maybeText = ? WHERE age = 35", Atom.optional(commentAtom).set(Option.none())).update().drain();

        tx.transact(updateText.bind(u -> getLeif))
                .map((List<User> list) -> Show.listShow(Util.<User>reflectionShow()).showS(list))
                .execute(prepend("* Setting the comment to 'none' and then get the user again\n", triedShow)::println);

        User arne = User("Arne Arnaldsson", 34, list(email("arne@tesgin.com"), phone("+47 30 24 04 56")));

        DbAction<Integer> insertNewUserWithMaybeComment =
                DbAction.prepare("INSERT INTO user (name,age,channels,maybeText) VALUES (?,?,?,?)", string.set(arne.name), num.set(arne.age), channelAtom.set(arne.channels), Atom.optional(commentAtom).set(arne.maybeText))
                        .update();

        DbAction<List<User>> getAllUsers =
                DbAction.prepare("SELECT name,age,channels,maybeText FROM user")
                        .query(comp(string, num, channelAtom, Atom.optional(commentAtom), User::new));


        tx.transact(insertNewUserWithMaybeComment.bind(i -> getAllUsers))
                .map((List<User> list) -> Show.listShow(Util.<User>reflectionShow()).showS(list))
                .execute(prepend("* Insert a new user with None<Comment> and get all users:\n", triedShow)::println);

    }


    public static <A> Show<A> prepend(String str, Show<A> aShow) {
        return Show.showS(a -> str + aShow.showS(a));
    }
}
