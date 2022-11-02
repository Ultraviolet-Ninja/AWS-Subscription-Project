package jasmine.jragon;

import java.time.LocalDate;

public enum Clairvoyance {
    TODAY(0, "Today"), ONE_DAY(1, "Tomorrow"),
    THREE_DAYS(3, "3 days from now");

    private final LocalDate datePrediction;
    private final String datePhrase;

    static final Clairvoyance[] PREDICTIONS = values();

    Clairvoyance(int daysOut, String phrase) {
        var prediction = LocalDate.now();

        if (daysOut > 0) {
            prediction = prediction.plusDays(daysOut);
        }
        datePrediction = prediction;
        datePhrase = phrase;
    }

    public LocalDate getDatePrediction() {
        return datePrediction;
    }

    @Override
    public String toString() {
        return datePhrase;
    }
}
