#!/usr/bin/env sh
# Run this script from the main project root directory like this:
# ./deployment/install-cluster.sh
# Before using this script to start up your system, install docker runtime (not docker desktop) and minikube as per this guide:
# https://itnext.io/goodbye-docker-desktop-hello-minikube-3649f2a1c469
# Make sure you also install helm: brew install kubernetes-helm

# to stop:
minikube stop

# set cpu and mem
minikube config set cpus 4
minikube config set memory 6g

# removes cluster and resources from hyperkit:
minikube delete

# If you have docker desktop installed this should start just fine.
minikube start

# If using hyperkit driver and docker runtime without docker desktop, specify driver and container runtime:
# minikube start --driver=hyperkit --container-runtime=docker

# To point your shell to minikube's docker-daemon, run:
eval $(minikube -p minikube docker-env)

# Add helm repositories that we need - bitnami, prometheus and grafana
helm repo add bitnami https://charts.bitnami.com/bitnami
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo add grafana https://grafana.github.io/helm-charts

# View helm repos, bitnami, prometheus and grafana should already be there
helm repo list

# Install kafka using our customized kakfa values.yaml that exposes a NodePort so that you can optionally use the kafka broker outside the cluster if desired.
# This is not necessary but it is useful to set it up once so that it can be useful for future experiments.
# *************** NOTE: You must get the minikube ip and replace the external ip address specified in the advertisedListeners property in bitnami-kafka-values.yaml
# *************** If you fail to do this, the kafka broker will not be available from outside the local k8s cluster.
helm install kafka bitnami/kafka --values=deployment/bitnami-kafka-values.yaml

# Install prometheus using our customized values.yaml
# This customized yaml allows prometheus to scrape the headless services for the producer and consumer that we are going to use with kafka.
helm install prometheus prometheus-community/prometheus --values=deployment/bitnami-prometheus.yaml

# Create prometheus node port to expose it outside the kubernetes cluster:
kubectl expose service prometheus-server --type=NodePort --target-port=9090 --name=prometheus-server-np

# Install grafana
# https://blog.marcnuri.com/prometheus-grafana-setup-minikube
helm install grafana grafana/grafana

# Expose grafana nodeport
kubectl expose service grafana --type=NodePort --target-port=3000 --name=grafana-np

# List all installed helm charts. The status for all of them should say 'deployed'
helm list

export WAIT_TIME=180
echo "Deployed helm charts for kafka, prometheus and grafana. Sleeping for $WAIT_TIME seconds to ensure they all start up fully."
sleep $WAIT_TIME

echo "Listing status of all services. They should all be deployed and running"
helm list
kubectl get pods
kubectl get services

chmod +x ./deployment/rebuild-redeploy.sh
./deployment/rebuild-redeploy.sh
