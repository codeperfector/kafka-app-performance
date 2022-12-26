#!/usr/bin/env sh

eval $(minikube -p minikube docker-env)
kubectl delete -f deployment/myapp-deployment.yaml
