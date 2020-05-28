FROM openjdk:11-jre-slim
COPY target/k8s-namespace-manager-*.jar /opt/consol/k8s-namespace-manager/app.jar
WORKDIR /opt/consol/k8s-namespace-manager
CMD java ${JAVA_OPTS} -jar app.jar
