package jasmine.jragon;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import software.amazon.awssdk.services.sns.model.SnsException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static jasmine.jragon.Clairvoyance.PREDICTIONS;
import static jasmine.jragon.SubscriptionRate.RATE_TO_ENUM;

public class SubscriptionLambdaFunction {
    private static final String ACCESS_KEY = "",
            SECRET_KEY = "";

    private static final String TABLE_NAME = "";
    private static final String TOPIC_ARN = "";
    private static final String DATABASE_DATE_FORMAT = "MM/dd/yyyy";

    private static final Supplier<StaticCredentialsProvider> CRED_PROVIDER = () -> StaticCredentialsProvider.create(
            AwsBasicCredentials.create(ACCESS_KEY, SECRET_KEY));

    private static final SnsClient SNS_CLIENT = SnsClient.builder()
            .region()
            .credentialsProvider(CRED_PROVIDER.get())
            .build();

    private static final DynamoDbClient DATABASE_CLIENT = DynamoDbClient.builder()
            .credentialsProvider(CRED_PROVIDER.get())
            .region()
            .build();

    public static void main(String[] args) {
        invokeFunction();
    }

    public static void invokeFunction() {
        var databaseList = scanDatabase();
        var upcomingSubscriptionMap = checkSubscriptionDates(databaseList);

        boolean areAllEmpty = upcomingSubscriptionMap.values()
                .stream()
                .allMatch(List::isEmpty);

        if (areAllEmpty) {
            return;
        }

        String message = convertEnumMapToMessage(upcomingSubscriptionMap);
        //For debugging purposes
//        System.out.println(message);
        publishToTopic(message);
    }

    private static List<Map<String, AttributeValue>> scanDatabase() {
        ScanRequest req = ScanRequest.builder()
                .tableName(TABLE_NAME)
                .attributesToGet(
                        List.of("CompanyName","ChargeAmount", "ChargeLocation",
                                "FirstChargeDate", "Reason", "SubscriptionRate")
                )
                .build();

        ScanResponse response = DATABASE_CLIENT.scan(req);
        return response.items();
    }

    private static EnumMap<Clairvoyance, List<Map<String, AttributeValue>>> checkSubscriptionDates(List<Map<String, AttributeValue>> rows) {
        final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern(DATABASE_DATE_FORMAT);
        EnumMap<Clairvoyance, List<Map<String, AttributeValue>>> rateSeparationMap = setupMap();

        for (var element : rows) {
            var rate = RATE_TO_ENUM.get(element.get("SubscriptionRate").s());

            String date = element.get("FirstChargeDate").s();
            LocalDate firstChargeDate = LocalDate.parse(date, dateFormat);

            for (var prediction : PREDICTIONS) {
                if (rate.test(firstChargeDate, prediction)) {
                    rateSeparationMap.get(prediction).add(element);
                    break;
                }
            }
        }
        return rateSeparationMap;
    }

    private static String convertEnumMapToMessage(EnumMap<Clairvoyance, List<Map<String, AttributeValue>>> rateSeparationMap) {
        return rateSeparationMap.entrySet()
                .stream()
                .filter(entry -> !entry.getValue().isEmpty())
                .map(entry -> parseToMessage(entry.getKey(), entry.getValue()))
                .collect(Collectors.joining("-".repeat(25) + "\n"));
    }

    private static String parseToMessage(Clairvoyance prediction, List<Map<String, AttributeValue>> subscriptions) {
        final LocalDate day = prediction.getDatePrediction();
        String body = subscriptions.stream()
                .map(element -> String.format(
                        "A payment of $%.2f to %s for %s.\nThis will be charged to your %s.",
                        Double.parseDouble(element.get("ChargeAmount").n()),
                        element.get("CompanyName").s(),
                        element.get("Reason").s(),
                        element.get("ChargeLocation").s()
                ))
                .collect(Collectors.joining("\n"));

        return String.format("%s (%d/%02d):\n\n%s",
                prediction,
                day.getMonthValue(),
                day.getDayOfMonth(),
                body
        );
    }

    private static EnumMap<Clairvoyance, List<Map<String, AttributeValue>>> setupMap() {
        EnumMap<Clairvoyance, List<Map<String, AttributeValue>>> outputMap =
                new EnumMap<>(Clairvoyance.class);
        for (var prediction : PREDICTIONS) {
            outputMap.put(prediction, new ArrayList<>());
        }
        return outputMap;
    }

    private static void publishToTopic(String message) {
        PublishRequest request = PublishRequest.builder()
                .message(message)
//                .messageStructure()
                .subject("Upcoming Subscription(s)")
                .topicArn(TOPIC_ARN)
                .build();

        try {
            PublishResponse result = SNS_CLIENT.publish(request);
            System.out.println(result.messageId() + " Message sent. Status is " + result.sdkHttpResponse().statusCode());
        } catch (SnsException e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }
    }
}
