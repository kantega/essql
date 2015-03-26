package essql.examples;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import essql.*;
import essql.examples.User.Channel;
import fj.Show;
import fj.data.List;
import fj.data.Stream;
import org.apache.commons.lang3.StringUtils;

import static essql.Atom.*;
import static essql.Composite.*;
import static essql.examples.User.Channel.email;
import static essql.examples.User.Channel.phone;
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


        Atom<List<Channel>> channelAtom =
                string.xmap(
                        str -> Stream.stream( StringUtils.split( str, ";" ) ).map( (String elem) -> elem.contains( "@" ) ? email( elem ) : phone( elem ) ).toList(),
                        channels -> Util.mkString( Channel.valueShow, channels, ";" ) );

        User leif = new User( "Leif Haraldson", 34, list( email( "leif@tesgin.com" ), phone( "+47 30 24 04 55" ) ) );


        DbAction<Integer> createTable = Query.prepare(
                "create table user(" +
                        "id int auto_increment not null," +
                        "name varchar(255) not null," +
                        "channels text not null," +
                        "age int not null," +
                        "primary key(id)" +
                        ")" ).update();


        DbAction<Integer> addLeif =
                Query
                        .prepare( "INSERT INTO user (name,age,channels) VALUES (?,?,?)", string.set( leif.name ), num.set( leif.age ), channelAtom.set( leif.channels ) )
                        .update();

        DbAction<List<User>> users =
                Query.prepare( "SELECT name,age,channels FROM user WHERE name = ?", string.set( leif.name ) )
                        .query( comp( string, num, channelAtom, User::new ) );

        tx.transact( createTable )
                .flatMap( r -> tx.transact( addLeif ) )
                .flatMap( rr -> tx.transact( users ) )
                .map( (List<User> list) -> Show.listShow( Util.<User>reflectionShow() ).showS( list ) )
                .execute( System.out::println );



    }

}
