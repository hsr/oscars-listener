package net.es.oscars.listener;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import net.es.oscars.api.soap.gen.v06.CancelResReply;
import net.es.oscars.api.soap.gen.v06.CancelResContent;
import net.es.oscars.client.OSCARSClient;
import net.es.oscars.client.OSCARSClientConfig;
import net.es.oscars.client.OSCARSClientException;
import net.es.oscars.common.soap.gen.OSCARSFaultMessage;

import org.restlet.data.CharacterSet;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.MappingJsonFactory;

/**
 * @author hsr
 * 
 */
public class CancelReservationResource extends ServerResource {
	public static String GRI = "gri";
	
	private Representation cancel(String gri) {
		String response;
		try {
			// Setup keystores
			OSCARSClientConfig.setClientKeystore("client", String.format(
					"%s/oscars-cert.jks", OSCARSListener.KEY_STORE),
					"client");
			
			OSCARSClientConfig.setSSLKeyStore(String.format(
					"%s/oscars-cert.jks", OSCARSListener.KEY_STORE), "client");

			// initialize client with service URL
			OSCARSClient client = new OSCARSClient(String.format(
					"https://%s:9001/OSCARS", OSCARSListener.OSCARS_SERVER));
			
			CancelResContent request = new CancelResContent();

			request.setGlobalReservationId(gri);
			
			// send request
			CancelResReply reply = client.cancelReservation(request);
			if (!reply.getStatus().equals(OSCARSClient.STATUS_OK)) {
				System.err.println("OSCARS returned non-OK status: "
						+ reply.getStatus());
				response = reply.getStatus();
			}
			else {
				// poll until circuit is reserved
				response = reply.getStatus();
			}
		} catch (OSCARSClientException e) {
			System.err.println("Error configuring client: " + e.getMessage());
			return answer("Error configuring client: " + e.getMessage());

		} catch (OSCARSFaultMessage e) {
			System.err.println("Error returned from server: " + e.getMessage());
			return answer("Error returned from server: " + e.getMessage());
		}

		return answer("Cancellation status for reservation " + gri + " is " + response);
	}
	
	@Get
	public Representation handleGet() {
		String gri = null;
		try {
			gri = ((String) getRequestAttributes().get("gri")).toUpperCase();
		} catch (Exception e) {
			return answer("Error trying to get gri: " + e.getMessage());
		}
		
		return cancel(gri);
	}
	
	@Post
	public Representation handlePost(String data) {
		Map<String, String> r = null;
		try {
			r = parseData(data);
		} catch (IOException e) {
			System.err.println("Error returned from client: " + e.getMessage());
			return answer("Error returned from client: " + e.getMessage());
		}
		return cancel(r.get(GRI));
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

			if (n == GRI)
				entry.put(GRI, jp.getText());
		}

		if (!entry.containsKey(GRI))
			throw new IOException("Missing " + GRI);

		return entry;
	}


	public Representation answer(String A) {
		Representation representation = new StringRepresentation(String.format(
				"{'response': '%s'}", A), MediaType.TEXT_PLAIN);
		representation.setCharacterSet(CharacterSet.UTF_8);
		return representation;
	}
	
}
