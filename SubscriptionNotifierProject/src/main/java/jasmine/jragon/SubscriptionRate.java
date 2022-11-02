package jasmine.jragon;

import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

public enum SubscriptionRate implements BiPredicate<LocalDate, Clairvoyance> {
    WEEKLY("Weekly") {
        @Override
        public boolean test(LocalDate date, Clairvoyance prediction) {
            long dayDiff = ChronoUnit.DAYS.between(date, prediction.getDatePrediction());
            return dayDiff % 7 == 0;
        }
    },
    BIWEEKLY("Biweekly") {
        @Override
        public boolean test(LocalDate date, Clairvoyance prediction) {
            long dayDiff = ChronoUnit.DAYS.between(date, prediction.getDatePrediction());
            return dayDiff % 14 == 0;
        }
    },
    MONTHLY("Monthly") {
        @Override
        public boolean test(LocalDate date, Clairvoyance prediction) {
            Period timeElapsed = Period.between(date, prediction.getDatePrediction());
            return timeElapsed.getDays() == 0;
        }
    },
    YEARLY("Yearly") {
        @Override
        public boolean test(LocalDate date, Clairvoyance prediction) {
            Period timeElapsed = Period.between(date, prediction.getDatePrediction());
            return timeElapsed.getMonths() == 0 && timeElapsed.getDays() == 0;
        }
    };

    private final String rateStr;

    static final Map<String, SubscriptionRate> RATE_TO_ENUM = Arrays.stream(values())
            .collect(Collectors.toMap(rate -> rate.rateStr, rate -> rate));

    SubscriptionRate(String rateStr) {
        this.rateStr = rateStr;
    }

    @Override
    public String toString() {
        return rateStr;
    }
}