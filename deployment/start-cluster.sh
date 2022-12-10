#!/usr/bin/env sh
# Run this script from the main project root directory like this:
# ./deployment/stop-cluster.sh
# Before using this script to start up your system, install docker runtime (not docker desktop) and minikube as per this guide:
# https://itnext.io/goodbye-docker-desktop-hello-minikube-3649f2a1c469
# Make sure you also install helm: brew install kubernetes-helm

minikube start --driver=hyperkit --container-runtime=docker
eval $(minikube -p minikube docker-env)