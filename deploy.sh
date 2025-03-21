#!/bin/bash

# Constants
AWS_ACCOUNT_ID="442557178688"
IMAGE_NAME="jupytereverywhere"
ECR_URL="$AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com" 
ECR_REPO="$ECR_URL/$IMAGE_NAME"
echo "Using ECR repository: $ECR_REPO"

# Getting the last image on the ECR and save the push date on a variable
DATE=$(aws ecr describe-images --repository-name $IMAGE_NAME --image-ids imageTag=latest --output json --no-cli-pager | jq --raw-output '.imageDetails[0].imagePushedAt' | cut -d'T' -f 1 )

echo "date: "
echo $DATE

# Getting the last ECR image manifest
MANIFEST=$(aws ecr batch-get-image --repository-name $IMAGE_NAME --image-ids imageTag=latest --output json --no-cli-pager | jq --raw-output '.images[0].imageManifest')
echo "MANIFEST"
echo $MANIFEST

# Put a new tag to the image
aws ecr put-image --repository-name $IMAGE_NAME --image-tag "v$DATE" --image-manifest "$MANIFEST" --no-cli-pager

# Delete latest tag from old image
aws ecr batch-delete-image --repository-name $IMAGE_NAME --image-ids imageTag=latest --no-cli-pager

# Authenticate Docker client in AWS ECR (non-interactive)
aws ecr get-login-password --region us-west-1 --no-cli-pager | docker login --username AWS --password-stdin $ECR_URL

# For Intel (adding -y flag for non-interactive)
docker build -t $IMAGE_NAME . --no-cache

# Tag new image as "latest"
docker tag $IMAGE_NAME:latest $ECR_REPO:latest
echo "pushing image..."
# Push the image to the repository
docker push $ECR_REPO:latest

# Update ECS service (non-interactive)
aws ecs update-service --cluster jupytereverywhere-testing-cluster --service jupytereverywhere-testing --force-new-deployment --no-cli-pager
