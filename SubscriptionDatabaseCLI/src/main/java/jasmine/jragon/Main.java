package jasmine.jragon;

import java.text.ParseException;
import java.util.Scanner;

public class Main {
    private static final Scanner USER_INPUT = new Scanner(System.in);

    public static void main(String[] args) {
        System.out.println("Hello!");
        System.out.println("How can I help you today?!");

        boolean continueLoop;

        do {
            continueLoop = getUserOption();
            if (continueLoop)
                System.out.println("Main Menu");
        } while (continueLoop);

    }

    private static boolean getUserOption() {
        String input = USER_INPUT.nextLine()
                .trim()
                .toLowerCase();

        if (input.isEmpty() || input.isBlank()) {
            System.err.println("Invalid input, try again.");
            return true;
        }

        String[] parts = input.split(" ");
        try {
            switch (parts[0]) {
                case "help" -> displayHelp();
                case "add", "append", "+" -> appendToDatabase();
                case "delete", "del", "rm", "-", "remove" -> deleteFromDatabase();
                case "display", "show" -> DatabaseConnector.listContents(input);
                case "ex", "exit", "bye" -> {
                    return false;
                }
                default -> System.err.println("Invalid input, try again.");
            }
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
        } catch (ParseException ignored) {

        }
        return true;
    }

    private static void appendToDatabase() throws IllegalArgumentException, ParseException {
        System.out.println("What company is the subscription to?");
        String companyName = USER_INPUT.nextLine();
        System.out.println("How much money goes to them?");

        while (!USER_INPUT.hasNextDouble()) {
            System.err.println("Not a number, try again");
            USER_INPUT.nextLine();
        }

        double chargedAmount = Double.parseDouble(USER_INPUT.nextLine());

        System.out.println("Where is the money getting charged from?");
        String chargeLocation = USER_INPUT.nextLine();
        System.out.println("When was the first charge?");
        String dateOfFirstCharge = USER_INPUT.nextLine();
        System.out.println("What's the reason behind the charge?");
        String reason = USER_INPUT.nextLine();
        System.out.println("What's the subscription rate?");
        SubscriptionRate rate = getRate();
        DatabaseConnector.append(companyName, chargedAmount, chargeLocation, dateOfFirstCharge, reason, rate);
    }

    private static SubscriptionRate getRate() {
        while (true) {
            String input = USER_INPUT.nextLine()
                    .trim()
                    .toLowerCase();
            switch (input) {
                case "w", "week", "weekly" -> {
                    return SubscriptionRate.WEEKLY;
                }
                case "b", "bi", "2", "2week", "2weeks", "bi-weekly", "biweekly" -> {
                    return SubscriptionRate.BIWEEKLY;
                }
                case "y", "year", "yearly", "a", "ann", "annual", "annually" -> {
                    return SubscriptionRate.YEARLY;
                }
                case "m", "mon", "month", "monthly" -> {
                    return SubscriptionRate.MONTHLY;
                }
                default -> System.err.println("Invalid input, try again");
            }
        }
    }

    private static void deleteFromDatabase() {
        System.out.println("What company are you looking to delete?");
        String companyName = USER_INPUT.nextLine().trim();
        DatabaseConnector.delete(companyName);
    }

    private static void displayHelp(){
        System.out.println("""
                Add/Append -\tAppend a record to the database
                Delete/Remove -\tRemove a record from the database
                Display/Show -\tDisplay all records in the database
                Exit/Bye -\t\tExit the program
                Help -\t\t\tDisplay options
                """);
    }
}