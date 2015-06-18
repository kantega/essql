package essql.examples;

import fj.Show;
import fj.data.List;
import fj.data.Option;

public class User {

    public final String name;

    public final int age;

    public final List<Channel> channels;

    public final Option<String> mayebText;

    public User(String name, int age, List<Channel> channels,Option<String> maybeText) {
        this.name = name;
        this.age = age;
        this.channels = channels;
        this.mayebText = maybeText;
    }


    public static User User(String name, int age, List<Channel> channel){
        return new User(name,age,channel,Option.<String>none());
    }

    public User withText(String text){
        return new User( name,age,channels,Option.fromNull( text ) );
    }

    public interface Channel {

        Show<Channel> valueShow =
                Show.stringShow.comap( Channel::stringValue );

        String stringValue();

        static Channel email(String value) {
            return new Email( value );
        }

        static Channel phone(String number) {
            return new Phone( number );
        }
    }

    public static class Email implements Channel {
        public final String address;

        public Email(String address) {
            this.address = address;
        }

        @Override public String stringValue() {
            return address;
        }
    }

    public static class Phone implements Channel {
        public final String number;

        public Phone(String number) {
            this.number = number;
        }

        @Override public String stringValue() {
            return number;
        }
    }
}
