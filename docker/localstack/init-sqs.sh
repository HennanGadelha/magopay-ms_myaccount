#!/bin/bash
set -e

echo "==> Creating SQS queues..."

awslocal sqs create-queue \
  --queue-name myaccount-user-registered \
  --region "${DEFAULT_REGION:-us-east-1}"

awslocal sqs create-queue \
  --queue-name myaccount-user-registered-dlq \
  --region "${DEFAULT_REGION:-us-east-1}"

awslocal sqs create-queue \
  --queue-name user-events-queue \
  --region "${DEFAULT_REGION:-us-east-1}"

echo "==> SQS queues created successfully."
awslocal sqs list-queues

