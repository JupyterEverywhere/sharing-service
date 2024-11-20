#!/bin/bash
echo "Starting localstack init..."
echo "Creating secrets"
file_directory=".aws-secrets"
for file in "$file_directory"/*.json; do
  file_name="${file##*/}"
  echo "Creating secret for service ${file_name%.json}"
  echo awslocal \
  --endpoint-url=http://localhost:4567 \
  secretsmanager \
  create-secret \
  --name local-${file_name%.json} \
  --secret-string file://${file} \
  --region us-west-1
done
echo "Creating secrets done"
echo "Execution finished"
