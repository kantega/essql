package essql.examples;

import fj.Show;
import fj.data.List;
import fj.data.Option;

public class User {

    public final String name;

    public final int age;

    public final List<Channel> channels;

    public final Option<Comment> maybeText;

    public User(String name, int age, List<Channel> channels,Option<Comment> maybeText) {
        this.name = name;
        this.age = age;
        this.channels = channels;
        this.maybeText = maybeText;
    }


    public static User User(String name, int age, List<Channel> channel){
        return new User(name,age,channel,Option.<Comment>none());
    }

    public User withText(Comment text){
        return new User( name,age,channels,Option.fromNull( text ) );
    }

    public interface Channel {

        Show<Channel> valueShow =
                Show.stringShow.contramap( Channel::stringValue );

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

        @Override
        public String toString() {
            return "Email("+stringValue()+")";
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

        @Override
        public String toString() {
            return "Phone("+stringValue()+")";
        }
    }

    public static class Comment{
        public final String value;

        public Comment(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return "Comment("+value+")";
        }
    }
}
