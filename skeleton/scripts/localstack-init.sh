#!/bin/bash
# LocalStack initialization script
# This script runs when LocalStack starts and creates AWS resources

set -euo pipefail

echo "Initializing LocalStack resources..."

{%- if values.database == "dynamodb" %}
# Create DynamoDB table
echo "Creating DynamoDB table..."
awslocal dynamodb create-table \
    --table-name ${{values.name}}-table \
    --attribute-definitions AttributeName=pk,AttributeType=S AttributeName=sk,AttributeType=S \
    --key-schema AttributeName=pk,KeyType=HASH AttributeName=sk,KeyType=RANGE \
    --billing-mode PAY_PER_REQUEST \
    --tags Key=Environment,Value=local
{%- endif %}

{%- if values.messaging == "sqs" or values.messaging == "sns-sqs" %}
# Create SQS queues
echo "Creating SQS queues..."

# Main queue
awslocal sqs create-queue --queue-name ${{values.name}}-queue

# Dead letter queue
awslocal sqs create-queue --queue-name ${{values.name}}-dlq

# Configure DLQ redrive policy
QUEUE_URL=$(awslocal sqs get-queue-url --queue-name ${{values.name}}-queue --output text --query 'QueueUrl')
DLQ_ARN=$(awslocal sqs get-queue-attributes --queue-url $(awslocal sqs get-queue-url --queue-name ${{values.name}}-dlq --output text --query 'QueueUrl') --attribute-names QueueArn --output text --query 'Attributes.QueueArn')

awslocal sqs set-queue-attributes \
    --queue-url "$QUEUE_URL" \
    --attributes "{\"RedrivePolicy\": \"{\\\"deadLetterTargetArn\\\":\\\"$DLQ_ARN\\\",\\\"maxReceiveCount\\\":\\\"3\\\"}\"}"
{%- endif %}

{%- if values.messaging == "sns-sqs" %}
# Create SNS topic
echo "Creating SNS topic..."
awslocal sns create-topic --name ${{values.name}}-topic

# Subscribe SQS queue to SNS topic
TOPIC_ARN=$(awslocal sns list-topics --output text --query "Topics[?contains(TopicArn, '${{values.name}}-topic')].TopicArn")
QUEUE_ARN=$(awslocal sqs get-queue-attributes --queue-url "$QUEUE_URL" --attribute-names QueueArn --output text --query 'Attributes.QueueArn')

awslocal sns subscribe \
    --topic-arn "$TOPIC_ARN" \
    --protocol sqs \
    --notification-endpoint "$QUEUE_ARN"
{%- endif %}

echo "LocalStack initialization complete!"
