package jasmine.jragon;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

public class DatabaseConnector {
    private static final String NAME = "CompanyName", CHARGE_AMT = "ChargeAmount", LOCATION = "ChargeLocation",
            FIRST_CHARGE_DATE = "FirstChargeDate", REASON = "Reason", SUB_RATE = "SubscriptionRate";
    private static final String TABLE_NAME = "";
    private static final String DATABASE_DATE_FORMAT = "MM/dd/yyyy";

    private static final BiFunction<Map<String, AttributeValue>, String, String>
            GET_ATTRIBUTE = (map, attr) -> map.get(attr).getS();

    private static final Function<Map<String, AttributeValue>, Double>
            GET_CHARGE_AMT = map -> Double.valueOf(map.get(CHARGE_AMT).getN());

    private static final Map<String, String> DATE_FORMAT_REGEXPS = new LinkedHashMap<>() {{
        put("^\\d{8}$", "yyyyMMdd");
        put("^\\d{1,2}-\\d{1,2}-\\d{4}$", "MM-dd-yyyy");
        put("^\\d{4}-\\d{1,2}-\\d{1,2}$", "yyyy-MM-dd");
        put("^\\d{1,2}/\\d{1,2}/\\d{4}$", DATABASE_DATE_FORMAT);
        put("^\\d{4}/\\d{1,2}/\\d{1,2}$", "yyyy/MM/dd");
        put("^\\d{1,2}\\s[a-z]{3}\\s\\d{4}$", "dd MMM yyyy");
        put("^\\d{1,2}\\s[a-z]{4,}\\s\\d{4}$", "dd MMMM yyyy");
        put("^[a-z]{4,}\\s\\d{1,2},\\s\\d{4}$", "MMMM dd,yyyy");
    }};

    private static final AmazonDynamoDB DEFAULT_CLIENT = AmazonDynamoDBClientBuilder.standard()
            .withRegion()
            .withCredentials(
                    new AWSStaticCredentialsProvider(
                            new BasicAWSCredentials(
                                    "",
                                    ""
                            )
                    )
            )
            .build();

    /**
     * Adds an instance of a recurring subscription to the Dynamo database
     *
     * @param companyName        The name of the company
     * @param chargedAmount      The dollar amount
     * @param chargeLocation     Where the money will come out of
     * @param dateOfFirstCharge  The date of the first charge of the subscription
     * @param subscriptionReason The purpose of the subscription
     * @param rate               How often money is being taken out
     * @throws IllegalArgumentException If a parameter is null, empty, or a negative cash value
     * @throws ParseException A parsing error occurs in the date format
     */
    public static void append(String companyName, double chargedAmount, String chargeLocation,
                              String dateOfFirstCharge, String subscriptionReason,
                              SubscriptionRate rate) throws IllegalArgumentException, ParseException {
        if (chargedAmount < 0.0) {
            throw new IllegalArgumentException("The charged amount cannot be negative");
        }
        checkForNull(companyName, chargeLocation, dateOfFirstCharge, subscriptionReason, rate);
        checkForEmpty(companyName, chargeLocation, dateOfFirstCharge, subscriptionReason);
        dateOfFirstCharge = toDatabaseDateFormat(dateOfFirstCharge);

        appendToDatabase(companyName, chargedAmount, chargeLocation,
                dateOfFirstCharge, subscriptionReason, rate);

        System.out.println("Addition complete!");
    }

    private static void appendToDatabase(String companyName, double chargedAmount, String chargeLocation,
                                         String dateOfFirstCharge, String subscriptionReason,
                                         SubscriptionRate rate) {
        AttributeValue chargedAmountAtt = new AttributeValue();
        chargedAmountAtt.setN(String.format("%.2f", chargedAmount));

        AttributeValue companyNameAtt = new AttributeValue(companyName),
                chargeLocationAtt = new AttributeValue(chargeLocation),
                dateOfFirstChargeAtt = new AttributeValue(dateOfFirstCharge),
                subscriptionReasonAtt = new AttributeValue(subscriptionReason),
                rateAtt = new AttributeValue(rate.toString());

        DEFAULT_CLIENT.putItem(TABLE_NAME, Map.of(
                NAME, companyNameAtt,
                CHARGE_AMT, chargedAmountAtt,
                LOCATION, chargeLocationAtt,
                FIRST_CHARGE_DATE, dateOfFirstChargeAtt,
                REASON, subscriptionReasonAtt,
                SUB_RATE, rateAtt
        ));
    }

    /**
     * Deletes an entry in the database based on the company name
     *
     * @param companyName The name of the company to delete
     * @throws IllegalArgumentException When the parameter given is empty, null or not in the database
     */
    public static void delete(String companyName) throws IllegalArgumentException {
        if (companyName == null || companyName.isEmpty()) {
            throw new IllegalArgumentException("Parameter cannot be null or empty");
        }
        verifyCompanyInDatabase(companyName);

        DEFAULT_CLIENT.deleteItem(
                TABLE_NAME,
                Map.of(NAME, new AttributeValue(companyName))
        );
        System.out.println("Deletion of unwanted file.");
    }

    /**
     * Lists the contents of the Dynamo database table in the following order:<p>
     * Company Name, Charge Amount, Charge Location, First Charge Date, Reason of subscription and Subscription Rate
     * <p>
     * For reference on scanning a full database: <a href="https://deveshsharmablogs.wordpress.com/2013/08/22/how-to-fetch-all-items-from-a-dynamodb-table-in-java/">...</a>
     *
     * @param command The full command used to check for a field to sort on
     */
    public static void listContents(String command) {
        if (!command.matches("(show|display)( -(company(name)?|name|charge|amount|location|(sub)?-?rate))?")) {
            System.err.println("Invalid input, try again.");
            return;
        }
        String[] parts = command.split(" -");
        Comparator<Map<String, AttributeValue>> customSort;

        ScanResult result = scanFullDatabase();

        List<Map<String, AttributeValue>> rows = result.getItems();

        if (parts.length == 2) {
            customSort = switch (parts[1]) {
                case "company", "companyname", "name" -> Comparator.comparing(
                        (Map<String, AttributeValue> m) -> GET_ATTRIBUTE.apply(m, NAME)
                );
                case "charge", "amount" -> Comparator.comparingDouble(GET_CHARGE_AMT::apply);
                case "subrate", "sub-rate", "rate" -> Comparator.comparing(
                        (Map<String, AttributeValue> m) -> GET_ATTRIBUTE.apply(m, SUB_RATE)
                );
                case "location" -> Comparator.comparing(
                        (Map<String, AttributeValue> m) -> GET_ATTRIBUTE.apply(m, LOCATION)
                );
                default -> null;
            };
            if (customSort == null) {
                System.err.println("Custom sort aborted\n");
            } else {
                rows.sort(customSort);
            }
        }

        System.out.printf("%15s | %13s | %22s | %17s | %30s | %17s\n", "Company Name", "Charge Amount",
                "Charge Location", "First Charge Date", REASON, "Subscription Rate");
        for (var row : rows) {
            System.out.printf("%15s | $%12.2f | %22s | %17s | %30s | %17s\n",
                    GET_ATTRIBUTE.apply(row, NAME), GET_CHARGE_AMT.apply(row),
                    GET_ATTRIBUTE.apply(row, LOCATION), GET_ATTRIBUTE.apply(row, FIRST_CHARGE_DATE),
                    GET_ATTRIBUTE.apply(row, REASON), GET_ATTRIBUTE.apply(row, SUB_RATE));
        }
    }

    private static ScanResult scanFullDatabase() {
        ScanRequest req = new ScanRequest();
        req.setTableName(TABLE_NAME);

        return DEFAULT_CLIENT.scan(req);
    }

    private static void verifyCompanyInDatabase(String companyName) throws IllegalArgumentException {
        boolean doesNotHaveEntry = scanFullDatabase()
                .getItems()
                .stream()
                .map(element -> GET_ATTRIBUTE.apply(element, NAME))
                .noneMatch(company -> company.equals(companyName));

        if (doesNotHaveEntry)
            throw new IllegalArgumentException("Database does not have company");
    }

    private static void checkForNull(Object... values) throws IllegalArgumentException {
        for (Object val : values) {
            if (val == null) {
                throw new IllegalArgumentException("Null instance detected");
            }
        }
    }

    private static void checkForEmpty(String... values) throws IllegalArgumentException {
        for (String val : values) {
            if (val.isEmpty()) {
                throw new IllegalArgumentException("Empty field detected");
            }
        }
    }

    /**
     * Finds the date format of the user entered String and formats it to what the database uses.
     *
     * @param dateOfFirstCharge The original data format
     * @return The format required for the database
     * @throws ParseException A parsing issue
     */
    private static String toDatabaseDateFormat(String dateOfFirstCharge) throws ParseException {
        String currentDateFormat = determineDateFormat(dateOfFirstCharge);

        return new SimpleDateFormat(DATABASE_DATE_FORMAT).format(
                new SimpleDateFormat(currentDateFormat).parse(dateOfFirstCharge)
        );
    }

    /**
     * Determine SimpleDateFormat pattern matching with the given date string. Returns null if
     * format is unknown. You can simply extend DateUtil with more formats if needed.
     * <p>
     * Code cited here: <a href="https://stackoverflow.com/questions/3389348/parse-any-date-in-java">...</a>
     *
     * @param dateString The date string to determine the SimpleDateFormat pattern for.
     * @return The matching SimpleDateFormat pattern
     * @throws IllegalArgumentException If the dateString format is unknown to the system
     * @see SimpleDateFormat
     */
    private static String determineDateFormat(String dateString) throws IllegalArgumentException {
        dateString = dateString.toLowerCase();
        for (var entry : DATE_FORMAT_REGEXPS.entrySet()) {
            if (dateString.matches(entry.getKey())) {
                return entry.getValue();
            }
        }
        throw new IllegalArgumentException("Unknown Date format specified.");
    }
}
