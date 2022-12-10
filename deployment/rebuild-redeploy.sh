#!/usr/bin/env sh

eval $(minikube -p minikube docker-env)
gradle build
docker build --tag=myapp:latest .
kubectl delete -f deployment/myapp-deployment.yaml
kubectl apply -f deployment/myapp-deployment.yaml
