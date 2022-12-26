#!/usr/bin/env sh

eval $(minikube -p minikube docker-env)
gradle build
docker build --tag=myapp:latest .
echo "****** NOTE: If the services were already deleted you might see errors below that say: error when deleting ... not found"
kubectl delete -f deployment/myapp-deployment.yaml
kubectl apply -f deployment/myapp-deployment.yaml
