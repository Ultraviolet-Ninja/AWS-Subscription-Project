package jasmine.jragon;

public enum SubscriptionRate {
    WEEKLY("Weekly"), BIWEEKLY("Biweekly"),
    MONTHLY("Monthly"), YEARLY("Yearly");

    private final String rateStr;

    SubscriptionRate(String rateStr) {
        this.rateStr = rateStr;
    }

    @Override
    public String toString() {
        return rateStr;
    }
}
