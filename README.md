# Vlog - MicroProfile Reactive Messaging on WildFLy on Openshift with RHOSAK

[RHOSAK](https://developers.redhat.com/products/red-hat-openshift-streams-for-apache-kafka) is a Managed Service from Red Hat providing users with zero maintenance Kafka instances.

## Prequisites
* An account on https://developers.redhat.com
  * Go to https://developers.redhat.com/developer-sandbox/get-started and provision an OpenShift instance 
* Download the following and make available in your PATH
  * [rhoas](https://developers.redhat.com/products/red-hat-openshift-streams-for-apache-kafka/download?tcDownloadFileName=rhoas-0.36.4-darwin.zip&tcRedirect=5000&tcSrcLink=https%3A%2F%2Fdevelopers.redhat.com%2Fcontent-gateway%2Fcontent%2Forigin%2Ffiles%2Fsha256%2Fbf%2Fbf39f89aa081b87aca868ca8ac05161ed5969cfab75809e09abd02b12d4eeb05%2Frhoas-0.36.4-darwin.zip&p=Product%3A+RHOAS+CLI&pv=RHOAS+CLI&tcDownloadURL=https%3A%2F%2Faccess.cdn.redhat.com%2Fcontent%2Forigin%2Ffiles%2Fsha256%2Fbf%2Fbf39f89aa081b87aca868ca8ac05161ed5969cfab75809e09abd02b12d4eeb05%2Frhoas-0.36.4-darwin.zip%3F_auth_%3D1652459810_dce7e63c60d470a1e6b90d64032faf4a) - this is the command line tool to interact with RHOSAK
  * [helm](https://helm.sh/docs/intro/install/)
    * Use it to add the `wildfly` Helm repostory as outlined in https://docs.wildfly.org/wildfly-charts/
  * `oc` - This is the OpenShift command line interface. It can be downloaded from the OpensShift console of the instance you provisioned a few steps back, as outlined [here](https://developers.redhat.com/openshift/command-line-tools).
    * Log into your OpenShift instance by copying the login command from the OpenShift console. This is available from the menu under your user name in the top right.
  
## Starting a Kafka instance in RHOSAK
* Go to https://developers.redhat.com/products/red-hat-openshift-streams-for-apache-kafka/getting-started and provision a Kafka instance. The initial step takes a few minutes.
  * Names of Kafka instances must be unique on the instance, so call it `<your-name>-kafka`. In my case that is `kabir-kafka` and what will be used for the rest of these instructions.
* Once the Kafka instance is up and running, go into it and create a topic called `testing`. Use the suggested defaults for all the options.
* Log in using rhoas, by running `rhoas login` in a terminal.
* Then we need to make `rhoas` use our Kafka instance (substitute `kabir-kafka` with what you called your instance):
```shell
$ rhoas kafka use --name kabir-kafka
```
* Then go to https://console.redhat.com/openshift/token and get the token to authenticate with your OpenShift cluster.
* Run the following command with the obtained token
```shell
$ rhoas cluster connect --service-type kafka --yes --token {your token pasted here}
```
* The output of the above command will show a command of the format `rhoas kafka acl grant-access --producer --consumer --service-account srvc-acct-<UID> --topic all --group all`. Copy this and run it in the terminal.

## Building and deploying the application
The application is a very simple REST endpoint which allows you to post messages with [MicroProfile Reactive Messaging](https://github.com/eclipse/microprofile-reactive-messaging). These messages are then sent to Kafka via the contained `Emitter`. Messages from Kafka come in to the `@Incoming` annotated method. We store the most recently received messages, and expose those via another REST endpoint.

From the root directory of the application, install the Helm chart containing the application:
```shell
$ helm install rhosak-example -f ./helm.yml wildfly/wildfly 
```
// #helm install rhosak-example -f ./helm.yml wildfly/wildfly --set build.uri=https://github.com/kabir/vlog-mp-reactive-messaging-rhosak.git --set build.ref={WildFlyQuickStartRepoTag}
THis will return quickly but that does not mean the application is up and running yet. Check the application in the OpenShift console or using `oc get deployment mp-rm-qs -w`.

While the application is deploying to OpenShift, run the following command to bind the RHOSAK Kafka instance to your OpenShift instance. 
```
rhoas cluster bind --app-name rhosak-example  --binding-name kafka-config --yes
```

This command will inject a bunch of secrets into your OpenShift application, and put them in the `/bindings/kafka-config` directory. 

The [initialize-server.cli](src/main/scripts/initialize-server.cli) script triggered by the `bootable-jar-openshift` profile 
in the POM (which in turn is set up in the `helm.yml`) maps this in the MicroProfile Config subsystem and maps the properties 
understood by the [SmallRye](https://smallrye.io/smallrye-reactive-messaging/3.16.0/kafka/kafka/) implementation 
used to implement MicroProfile Reactive Messaging. Additional properties to configure the application come from
the contained [microprofile-config.properties](src/main/resources/META-INF/microprofile-config.properties).


## Trying the application
First get the URL of the application:
```shell
$ oc get route
NAME             HOST/PORT                                                          PATH   SERVICES         PORT    TERMINATION     WILDCARD
rhosak-example   rhosak-example-kkhan1-dev.apps.sandbox.x8i5.p1.openshiftapps.com          rhosak-example   <all>   edge/Redirect   None
```
In my case the URL is `rhosak-example-kkhan1-dev.apps.sandbox.x8i5.p1.openshiftapps.com`. You should of course substitute 
that with the URL of your application in the following steps.

First let's add some entries using Curl:
```shell
$ curl  -X POST https://rhosak-example-kkhan1-dev.apps.sandbox.x8i5.p1.openshiftapps.com/one
$ curl  -X POST https://rhosak-example-kkhan1-dev.apps.sandbox.x8i5.p1.openshiftapps.com/two 
```
These will be sent to Kafka, and received again by the application which will keep a list of the most recently received values.
To read this list of recently received values, we can run Curl again:
```shell
$ curl  https://rhosak-example-kkhan1-dev.apps.sandbox.x8i5.p1.openshiftapps.com  
[one, two]
$                                                                                                  
```

# Troubleshooting
Delete everything relating to what we have been installing to start from a clean slate.

Uninstall the application:
``` shell
$ helm uninstall rhosak-example
```

Delete the service account by going to the 'Service Accounts' section of the RHOAS console and delete any associated with your Red Hat ID.

Delete the Kafka connection:
``` shell
$ oc get kafkaconnection
NAME          AGE
kabir-kafka   3d22h

$ oc delete kafkaconnection kabir-kafka
kafkaconnection.rhoas.redhat.com "kabir-kafka" deleted
```

Delete the associated Service Binding:
``` shell
$ oc get servicebinding                
NAME           READY   REASON              AGE
kafka-config   True    ApplicationsBound   3d20h

$ oc delete servicebinding kafka-config
servicebinding.binding.operators.coreos.com "kafka-config" deleted
```

Delete the secrets pertaining to the Service Account. (This is often needed on redeploy of the application. Do this before redeploying the application, and run the earlier `rhoas cluster bind` command while waiting for the application to build.)
```shell
$ oc get secret
NAME                                    TYPE                                  DATA   AGE
-- SNIP - The following two are the important ones --
rh-cloud-services-accesstoken           Opaque                                1      3d22h
rh-cloud-services-service-account       Opaque                                2      3d22h

$ oc delete secret rh-cloud-services-accesstoken
secret "rh-cloud-services-accesstoken" deleted

$ oc delete secret rh-cloud-services-service-account 
secret "rh-cloud-services-service-account" deleted
```