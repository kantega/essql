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
import fj.data.List;
import fj.data.Option;
import fj.data.Stream;
import no.kantega.effect.Tried;
import org.apache.commons.lang3.StringUtils;

import static essql.Composite.comp;
import static essql.examples.User.*;
import static essql.examples.User.Channel.email;
import static essql.examples.User.Channel.phone;
import static essql.txtor.Atom.num;
import static essql.txtor.Atom.string;
import static fj.data.List.list;

public class Example {
    public static void main(String[] args) {

        HikariConfig config = new HikariConfig();
        config.setDriverClassName( "org.h2.Driver" );
        config.setJdbcUrl( "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1" );
        config.setUsername( "da" );
        config.setPassword( "" );
        config.addDataSourceProperty( "cachePrepStmts", "true" );
        config.addDataSourceProperty( "prepStmtCacheSize", "250" );
        config.addDataSourceProperty( "prepStmtCacheSqlLimit", "2048" );
        config.addDataSourceProperty( "useServerPrepStmts", "true" );

        HikariDataSource ds = new HikariDataSource( config );

        DatasourceTransactor tx = new DatasourceTransactor( ds );

        Show<Tried<?>> triedShow =
                Show.stringShow.comap( (Tried<?> tried) -> tried.fold( Throwable::getMessage, Object::toString ) );

        Atom<List<Channel>> channelAtom =
                string.xmap(
                        str -> Stream.stream( StringUtils.split( str, ";" ) ).map( (String elem) -> elem.contains( "@" ) ? email( elem ) : phone( elem ) ).toList(),
                        channels -> Util.mkString( Channel.valueShow, channels, ";" ) );

        User leif = User( "Leif Haraldson", 34, list( email( "leif@tesgin.com" ), phone( "+47 30 24 04 55" ) ) );


        DbAction<Integer> createTable = DbAction.prepare(
                "CREATE TABLE user(" +
                        "id INT AUTO_INCREMENT NOT NULL," +
                        "name VARCHAR(255) NOT NULL," +
                        "channels TEXT NOT NULL," +
                        "age INT NOT NULL," +
                        "maybeText VARCHAR(255), " +
                        "PRIMARY KEY(id)" +
                        ")" ).update();


        DbAction<Integer> addLeif =
                DbAction
                        .prepare( "INSERT INTO user (name,age,channels) VALUES (?,?,?)", string.set( leif.name ), num.set( leif.age ), channelAtom.set( leif.channels ) )
                        .update();

        DbAction<List<User>> getUsers =
                DbAction.prepare( "SELECT name,age,channels,maybeText FROM user WHERE name = ?", string.set( leif.name ) )
                        .query( comp( string, num, channelAtom,Atom.optional( Atom.string ), User::new ) );

        tx.transact( createTable )
                .flatMap( r -> tx.transact( addLeif ) )
                .flatMap( rr -> tx.transact( getUsers ) )
                .map( (List<User> list) -> Show.listShow( Util.<User>reflectionShow() ).showS( list ) )
                .execute( triedShow::println );


        Composite<User> userComp =
                comp( Atom.string ).flatMap(
                        name -> comp( Atom.num ).flatMap(
                                age -> comp( channelAtom ).flatMap(
                                        channels -> comp( Atom.optional( Atom.string ) ).map(
                                                maybeText -> new User( name, age, channels, maybeText )
                                        )
                                )
                        )
                );

        DbAction<List<User>> getUsersWithComp =
                DbAction.prepare( "SELECT name,age,channels,maybeText FROM user WHERE name = ?", string.set( leif.name ) )
                        .query( userComp );

        tx.transact( getUsersWithComp )
                .map( (List<User> list) -> Show.listShow( Util.<User>reflectionShow() ).showS( list ) )
                .execute( triedShow::println );


        DbAction<Unit> updateUser =
                DbAction.prepare( "UPDATE user SET age = 35 WHERE age = 34", Atom.string.set( "Bjarne" ) ).update().drain();


        tx.transact( updateUser.bind( x -> getUsersWithComp ) ).map( (List<User> list) -> Show.listShow( Util.<User>reflectionShow() ).showS( list ) )
                .execute( triedShow::println );


        DbAction<List<User>> getUserWithMaualMapping =
                DbAction
                        .prepare( "SELECT name,age,channels,maybeText FROM user WHERE name = ?", string.set( leif.name ) )
                        .query( rs -> {
                            String name = Field.readField( "name", Atom.string, rs );
                            int age = Field.readField( "age", Atom.num, rs );
                            List<Channel> channels = Field.readField( "channels", channelAtom, rs );
                            return User( name, age, channels );
                        } );

        tx.transact( getUserWithMaualMapping )
                .map( (List<User> list) -> Show.listShow( Util.<User>reflectionShow() ).showS( list ) )
                .execute( triedShow::println );

        DbAction<List<User>> faileingGetUserWithMaualMapping =
                DbAction
                        .prepare( "SELECT name,age,channels,maybeText FROM user WHERE name = ?", string.set( leif.name ) )
                        .query( rs -> {
                            String name = Field.readField( "name", Atom.string, rs );
                            int age = Field.readField( "name", Atom.num, rs );
                            List<Channel> channels = Field.readField( "channels", channelAtom, rs );
                            return User( name, age, channels );
                        } );

        tx.transact( faileingGetUserWithMaualMapping )
                .map( (List<User> list) -> Show.listShow( Util.<User>reflectionShow() ).showS( list ) )
                .execute( triedShow::println );


        DbAction<Unit> updateNoText =
                DbAction.prepare( "UPDATE user SET maybeText = ? WHERE age = 35", Atom.optional( Atom.string ).set( Option.<String>none() ) ).update().drain();

        tx.transact( updateNoText.bind( u -> getUsers ) )
                .map( (List<User> list) -> Show.listShow( Util.<User>reflectionShow() ).showS( list ) )
                .execute( triedShow::println );

        DbAction<Unit> updateText =
                DbAction.prepare( "UPDATE user SET maybeText = ? WHERE age = 35", Atom.string.set( "Jalla" ) ).update().drain();

        tx.transact( updateText.bind( u -> getUsers ) )
                .map( (List<User> list) -> Show.listShow( Util.<User>reflectionShow() ).showS( list ) )
                .execute( triedShow::println );

    }

}
