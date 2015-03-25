package essql.examples;

import fj.Show;
import fj.data.List;

public class User {

    public final String name;

    public final int age;

    public final List<Channel> channels;

    public User(String name, int age, List<Channel> channels) {
        this.name = name;
        this.age = age;
        this.channels = channels;
    }

    public static interface Channel {

        public final Show<Channel> valueShow =
                Show.stringShow.comap( Channel::stringValue );

        public String stringValue();

        public static Channel email(String value) {
            return new Email( value );
        }

        public static Channel phone(String number) {
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
