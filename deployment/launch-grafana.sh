#!/usr/bin/env sh

# To access grafana, we need to get grafana admin password with this command
kubectl get secret --namespace default grafana -o jsonpath="{.data.admin-password}" | base64 --decode | xargs echo "****** Grafana Login - user: admin, password: "

echo ""

# Launch grafana in browser and login with user admin and the password above.
minikube service grafana-np

echo ""

echo "You have to add a data source in grafana before creating any dashboards."
echo "Go to Configuration > Data Sources and select add a data source."
echo "Create the data source for prometheus at http://prometheus-server:80"
echo "Now you can import the Myapp dashboard from deployment/myapp-dashboard.json"
