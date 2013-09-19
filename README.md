# OSCARSListener

Simple HTTP service that translates JSON requests to OSCARS API

## Why? 
In the summer of 2013, I worked with the integration of [OSCARS](http://www.es.net/services/virtual-circuits-oscars/ OSCARS) and [Floodlight](http://www.projectfloodlight.org/floodlight/). In the SDN context, OSCARS is an application that controls the [ESnet](http://es.net/) 100G network and Floodlight is a SDN controller that communicates with network devices using [OpenFlow](http://www.openflow.org/). 

This repository has the code for OSCARSListener: a simple HTTP service that enables an external application to request OSCARS circuits using JSON. This code is based on the code found at the [OSCARS API wiki page](https://code.google.com/p/oscars-idc/wiki/OSCARS_Java_Client_API).

## Usage

#### OSCARS Installation
The first thing you need to do is, not surprisingly, to install OSCARS. Follow instructions on the [OSCARS Repository](http://github.com/hsr/oscars) to install OSCARS. 

#### Setup Keys

Generate your keys using the alias `client` and password `client` (if you decide to use different alias and password, you need to change that in the code as well):

    keytool -genkey -keyalg rsa -alias client -keystore oscars-cert.jks

After creating your keystore, extract the certificate from it and copy the certificate to your server running OSCARS:

    keytool -export -keystore ./oscar-cert.jks -file oscars-client.cer -alias client
    scp oscars-client.cer ${YOUR_SERVER}:~/

On your server, import the certificate to each keystore created after the OSCARS installation:

    cd ${OSCARS_HOME}/sampleDomain/certs/
    keytool -import -file oscars-client.cer -alias client -keystore {client,localhost,oscarsidc,service,truststore}.jks

The default password for OSCARS's keystores is `changeit`

Create a user using OSCARS web interface. You'll need the X509 subject and issuer name of your certificate.  You can get it after importing your certificate to one of the OSCARS keystores. It looks  like: 

"Owner: CN=client client, OU=net, O=esnet, L=berkeley, ST=ca, C=us"

Now extract the OSCARS server certificates that will be imported on your client. In my case I imported theca and thera. You might need other certificates depending on your setup:

    keytool -list -keystore oscarsidc.jks
    keytool -export -keystore oscarsidc.jks -alias theca -file theca.cer
    keytool -export -keystore oscarsidc.jks -alias thera -file thera.cer

Copy the certificates to your client:

    scp {theca,thera}.cer ${YOUR_CLIENT}:~/${KEYS_PATH}/

And import them to your keystore:

    keytool -import -keystore oscars-cert.jks -file ${KEYS_PATH}/theca.cer -alias theca
    keytool -import -keystore oscars-cert.jks -file ${KEYS_PATH}/theca.cer -alias thera
    
#### Run service:

The parameters for OSCARSListener are:

 - Port that OSCARSListener should listen for JSON requests.
 - OSCARS server URL (without port).
 - OSCARS domain that will be included in the requests.
 - OSCARS directory/path where your keystore is.

Start the service:

    PORT=9911
    DOMAIN="testdomain-1"
    java -jar oscars-listener.jar ${PORT} ${YOUR_SERVER} ${DOMAIN} ${KEYS_PATH}
    

#### Examples:

**List all finished circuits:**

    curl http://${YOUR_SERVER}:${PORT}/list/finished
    
**List all active circuits:**

    curl http://${YOUR_SERVER}:${PORT}/list/active

**Request a reservation from node N1 port N1P1 to node N2 port N2P1 of 100mbps:**

    curl -d '{"bandwidth":100000000,"src-switch":"N1","src-port":"N1P1","dst-switch":"N2","dst-port":"N2P1","ofrule":"nop"}' http://${YOUR_SERVER}:${PORT}/request 

**Request the same reservation, but to carry only UDP traffic (only with SDNPSS - replace node names with DPID):**

    curl -d '{"bandwidth":100000000,"src-switch":"N1","src-port":"N1P1","dst-switch":"N2","dst-port":"N2P1","ofrule":"nw_proto=17"}' http://${YOUR_SERVER}:${PORT}/request 

### Known issues

**Time out of sync:** If you get a message like "Error returned from server: The message has expired (WSSecurityEngine: Invalid timestamp The security semantics of the message have expired)", make sure that your server's clock is accurate. If not, adjust it manually or using NTP.
