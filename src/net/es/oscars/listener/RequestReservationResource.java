package net.es.oscars.listener;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import net.es.oscars.api.soap.gen.v06.CreateReply;
import net.es.oscars.api.soap.gen.v06.Layer2Info;
import net.es.oscars.api.soap.gen.v06.PathInfo;
import net.es.oscars.api.soap.gen.v06.ResCreateContent;
import net.es.oscars.api.soap.gen.v06.UserRequestConstraintType;
import net.es.oscars.client.OSCARSClient;
import net.es.oscars.client.OSCARSClientConfig;
import net.es.oscars.client.OSCARSClientException;
import net.es.oscars.common.soap.gen.OSCARSFaultMessage;

import org.restlet.data.CharacterSet;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.MappingJsonFactory;

/**
 * Request a reservation only. Do not wait for the outcome/result of the
 * reservation (i.e. status = ACTIVE|RESERVED|FAILURE).
 * 
 * @author hsr
 * 
 */
public class RequestReservationResource extends ServerResource {
	public static String SRC_PORT = "src-port";
	public static String DST_PORT = "dst-port";
	public static String SRC_NODE = "src-switch";
	public static String DST_NODE = "dst-switch";
	public static String BANDWIDTH = "bandwidth";
	public static String OFRULE = "ofrule";

	@Post
	public Representation create(String data) {
		Representation representation = new StringRepresentation(data,
				MediaType.TEXT_PLAIN);
		representation.setCharacterSet(CharacterSet.UTF_8);
		String urn = "urn:ogf:network:domain=%s:node=%s:port=%s:link=1";
		String response = "";
		String gri = "";
		try {
			Map<String, String> r = parseData(data);

			System.out.println("Requested");
			for (Map.Entry<String, String> e : r.entrySet()) {
				System.out.println("key: " + e.getKey() + ", value: "
						+ e.getValue());
			}

			// Setup keystores
			OSCARSClientConfig.setClientKeystore("client", String.format(
					"%s/oscars-cert.jks", OSCARSListener.KEY_STORE), "client");

			OSCARSClientConfig.setSSLKeyStore(String.format(
					"%s/oscars-cert.jks", OSCARSListener.KEY_STORE), "client");

			// initialize client with service URL
			OSCARSClient client = new OSCARSClient(String.format(
					"https://%s:9001/OSCARS", OSCARSListener.OSCARS_SERVER));

			// Build request for 100Mbps circuit for 1 hour between two points
			ResCreateContent request = new ResCreateContent();
			request.setDescription(r.get(OFRULE));
			UserRequestConstraintType userConstraint = new UserRequestConstraintType();

			// start immediately
			userConstraint.setStartTime(System.currentTimeMillis() / 1000);

			// 1 hour in the future
			userConstraint.setEndTime(System.currentTimeMillis() / 1000 + 3600);

			// bandwidth in Mbps
			userConstraint.setBandwidth(Integer.parseInt(r.get(BANDWIDTH))/1000/1000);
			PathInfo pathInfo = new PathInfo();
			Layer2Info layer2Info = new Layer2Info();
			layer2Info.setSrcEndpoint(String.format(urn,
					OSCARSListener.OSCARS_DOMAIN, r.get(SRC_NODE),
					r.get(SRC_PORT)));

			layer2Info.setDestEndpoint(String.format(urn,
					OSCARSListener.OSCARS_DOMAIN, r.get(DST_NODE),
					r.get(DST_PORT)));

			pathInfo.setLayer2Info(layer2Info);
			userConstraint.setPathInfo(pathInfo);
			request.setUserRequestConstraint(userConstraint);

			// send request
			CreateReply reply = client.createReservation(request);
			if (!reply.getStatus().equals(OSCARSClient.STATUS_OK)) {
				System.err.println("OSCARS returned non-OK status: "
						+ reply.getStatus());
			}

			gri = reply.getGlobalReservationId();			
			System.out.println("Circuit" + gri + " has been reserved");
			
			response = gri + ", " + reply.getStatus();
		} catch (OSCARSClientException e) {
			System.err.println("Error configuring client: " + e.getMessage());
			return answer("Error configuring client: " + e.getMessage());

		} catch (OSCARSFaultMessage e) {
			System.err.println("Error returned from server: " + e.getMessage());
			return answer("Error returned from server: " + e.getMessage());

		} catch (IOException e) {
			System.err.println("Error returned from client: " + e.getMessage());
			return answer("Error returned from client: " + e.getMessage());
		}

		return answer("Status for reservation " + gri + " is " + response);
	}

	/**
     */
	public static Map<String, String> parseData(String data) throws IOException {
		Map<String, String> entry = new HashMap<String, String>();
		MappingJsonFactory f = new MappingJsonFactory();
		JsonParser jp;

		try {
			jp = f.createJsonParser(data);
		} catch (JsonParseException e) {
			throw new IOException(e);
		}

		jp.nextToken();
		if (jp.getCurrentToken() != JsonToken.START_OBJECT) {
			throw new IOException("Expected START_OBJECT");
		}

		while (jp.nextToken() != JsonToken.END_OBJECT) {
			if (jp.getCurrentToken() != JsonToken.FIELD_NAME) {
				throw new IOException("Expected FIELD_NAME");
			}

			String n = jp.getCurrentName();
			jp.nextToken();
			if (jp.getText().equals(""))
				continue;

			if (n == SRC_PORT)
				entry.put(SRC_PORT, jp.getText());
			else if (n == DST_PORT)
				entry.put(DST_PORT, jp.getText());
			else if (n == SRC_NODE)
				entry.put(SRC_NODE, jp.getText());
			else if (n == DST_NODE)
				entry.put(DST_NODE, jp.getText());
			else if (n == BANDWIDTH)
				entry.put(BANDWIDTH, jp.getText());
			else if (n == OFRULE)
				entry.put(OFRULE, jp.getText());

		}

		if (!entry.containsKey(SRC_PORT))
			throw new IOException("Missing " + SRC_PORT);

		if (!entry.containsKey(DST_PORT))
			throw new IOException("Missing " + DST_PORT);
		if (!entry.containsKey(SRC_NODE))
			throw new IOException("Missing " + SRC_NODE);
		if (!entry.containsKey(DST_NODE))
			throw new IOException("Missing " + DST_NODE);
		if (!entry.containsKey(BANDWIDTH))
			throw new IOException("Missing " + BANDWIDTH);
		if (!entry.containsKey(OFRULE))
			throw new IOException("Missing " + OFRULE);

		return entry;
	}

	public Representation answer(String A) {
		Representation representation = new StringRepresentation(String.format(
				"{'response': '%s'}", A), MediaType.TEXT_PLAIN);
		representation.setCharacterSet(CharacterSet.UTF_8);
		return representation;
	}
}
