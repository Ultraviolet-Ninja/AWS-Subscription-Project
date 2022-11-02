# Subscription Notifier

## Description

Using free elements of AWS, I will set up a notification system that will send me messages of any upcoming subscription payment.

The Lambda function code that draws from my Dynamo database to see if I have any upcoming subscription payments. If it detects that I have any payments coming <u>today</u>, <u>tomorrow</u>, or <u>3 days from now</u>, it will send a message to an SNS Topic to get sent to an email that I have subscribed to the topic.



## Helpful Links
- [Package Info](https://github.com/aws/aws-sdk-java-v2/#using-the-sdk)
- [DynamoDB Examples](https://github.com/awsdocs/aws-doc-sdk-examples/tree/main/javav2/example_code/dynamodb)
- [Lambda Examples](https://github.com/awsdocs/aws-doc-sdk-examples/tree/main/javav2/example_code/lambda)
- [SNS Examples](https://github.com/awsdocs/aws-doc-sdk-examples/tree/main/javav2/example_code/sns)



## Technologies

- Java 11
  - AWS Lambda uses Java 11

- Gradle 7.4
- AWS Java SDK v2
  - DynamoDB ver. `2.18.1`
  - Lambda ver. `2.18.1`
  - SNS ver. `2.18.1`

