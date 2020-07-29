# gotitinc-lex-publisher
Java app that converts a GotIt Inc Indie-formatted JSON file into an AWS Lex bot

## How to Run

Run the app via *LexPublisherApplication*'s *main* method.
Pass through the following command line arguments:-
1. AWS IAM Access Key
2. AWS IAM Secret Key
3. AWS Region
4. Indie File Location (local path assumed)
5. AWS ARN for Fulfillment Lambda function

## Enhancements

1. Convert to AWS Lambda function
2. Read Indie file from S3 bucket
3. Use CloudFormtion template to create the publisher lambda, fulfillment lambda, buckets, roles, permissions etc as needed
