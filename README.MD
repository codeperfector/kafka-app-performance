# Demo project for Kafka Consumer Throughput
I struggled a bit to find good resources online to setup a simple kafka producer and consumer in minikube with prometheus metrics. That last part was pretty hard because setting up prometheus can be really challenging as there are so many knobs. Eventually I found a way to set things up without diving too deep into prometheus giving me what I need. I intend to use this demo project to explore kafka throughput related concerns, examining details with prometheus metrics.

This project uses the following infrastructure to setup a basic Kubernetes Cluster with Helm installed to make it easier to run apps: 
 - minikube v1.24.0
 - Docker Runtime Client and Server - version 20.10 (Not Docker Desktop!)
 - Helm 3

We will additionally use the following for our project:
 - Bitnami Kafka Helm Chart (Kafka 2.8.1) 
 - Prometheus Community Helm Chart (Prometheus 2.34.0)
 - Kotlin JVM 1.6.0
 - Shadow plugin from com.github.johnrengelman.shadow to create uberjars for the docker image


## Setting up the minikube environment

set cpu and mem first:
```
minikube config set cpus 4
minikube config set memory 6g
```

start minikube k8s cluster:
```
minikube start --driver=hyperkit --container-runtime=docker
```

To point your shell to minikube's docker-daemon, run:
```
eval $(minikube -p minikube docker-env)
```
Now you can use docker and/or kubectl commands or use k9s.

Set up an alias to make it easier to use kubectl, add this to you .bashrc or .zshrc file:
```
alias k=kubectl
```

To stop minikube:
```
minikube stop
```

Removes cluster and resources from hyperkit:
```
minikube delete
```

Install helm 3, Helm 3 does not use tiller so commands are a bit different from helm 2:
https://helm.sh/docs/topics/v2_v3_migration/
https://www.starkandwayne.com/blog/helm-3-how-do-i-do-helm-2-stuff/
```
brew install kubernetes-helm
```

Check your versions:
```
minikube version
docker version
helm version
```

Now we are ready for the next step - add the helm charts.

Add bitnami repository
```
helm repo add bitnami https://charts.bitnami.com/bitnami
helm repo list
```
For reference you can read this post: https://github.com/bitnami/charts/tree/master/bitnami/kafka


Install kafka, first get the minikube ip and replace the external ip address specified in the advertisedListeners property in bitnami-kafka-values.yaml 
```
*************** WARNING!!!! If you fail to do this, the kafka broker will not be available from outside the local k8s cluster. ***************
helm install kafka bitnami/kafka --values=./bitnami-kafka-values.yaml
```

Install prometheus:
```
helm install prometheus prometheus-community/prometheus
```

Expose the prometheus server pod
```
export POD_NAME=$(kubectl get pods --namespace default -l "app=prometheus,component=server" -o jsonpath="{.items[0].metadata.name}")
kubectl --namespace default port-forward $POD_NAME 9090
```

Get status of install
```
helm status kafka
```

Get list of helm releases
```
helm list
```

Now we are ready for our app installation and experiments. If you want to uninstall kafka you can just do this:
```
helm uninstall kafka
```

If at any time you want to stop or pause your experiments and return to it later, you can do minikube pause and unpause to stop resource hogging on your laptop while preserving the containers.
```
minikube pause
```
```
minikube unpause
```


## Building and deploying our apps

At the top level of the project directory for kafka-consumer-throughput, build the docker container image:
```
eval $(minikube -p minikube docker-env)
```
Build the uberjar. This artifact can be configured to be our kafka producer or consumer.
```
gradle build
```

Verify the uberjar was built (kafka-consumer-throughput-1.0-SNAPSHOT-all.jar)
```
ls -la build/libs
```

Build the docker image.
```
docker build --tag=myapp:latest .
```

Deploy container images to kubernetes cluster. This yaml configures our kafka producer and consumer apps.
```
kubectl apply -f myapp-deployment.yaml
```

View the running pods
```
kubectl get pods
```

View pod logs 
```
kubectl logs << your app name >> --follow
```

We're all set with our producer and consumer pods.

You can use the kubectl delete commands if you want to delete the deployment or pods.


Figure out your pod's DNS domain
```
kubectl exec -it myapp cat /etc/resolv.conf
```
This produces output like this:
```
 nameserver 10.96.0.10
 search default.svc.cluster.local svc.cluster.local cluster.local
 options ndots:5
```

Scaling your pods can be done like this:
```
kubectl scale deploy myapp --replicas=2
```

