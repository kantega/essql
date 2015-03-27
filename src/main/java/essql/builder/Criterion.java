package essql.builder;

import static essql.builder.Constants.*;

public abstract class Criterion {
    protected final Operator operator;

    Criterion(Operator operator) {
        this.operator = operator;
    }

    public static Criterion and(final Criterion criterion, final Criterion... criterions) {
        return new Criterion(Operator.and) {

            protected void populate(StringBuilder sb) {
                sb.append(criterion);
                for (Criterion criterion : criterions) {
                    sb.append(SPACE).append(AND).append(SPACE).append(criterion);
                }
            }
        };
    }

    public static Criterion or(final Criterion criterion, final Criterion... criterions) {
        return new Criterion(Operator.or) {

            protected void populate(StringBuilder sb) {
                sb.append(criterion);
                for (Criterion criterion : criterions) {
                    sb.append(SPACE).append(OR).append(SPACE).append(criterion.toString());
                }
            }
        };
    }

    public static Criterion exists(final Query query) {
        return new Criterion(Operator.exists) {

            protected void populate(StringBuilder sb) {
                sb.append(EXISTS).append(SPACE).append(LEFT_PARENTHESIS).append(query).append(RIGHT_PARENTHESIS);
            }
        };
    }

    public static Criterion not(Criterion criterion) {
        return new Criterion(null) {

            protected void populate(StringBuilder sb) {
                sb.append(Operator.not).append(SPACE).append(RIGHT_PARENTHESIS);
                criterion.populate( sb );
                sb.append(LEFT_PARENTHESIS);
            }
        };
    }

    protected abstract void populate(StringBuilder sb);

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(LEFT_PARENTHESIS);
        populate(builder);
        builder.append(RIGHT_PARENTHESIS);
        return builder.toString();
    }

}
