package net.es.oscars.listener;

import org.restlet.data.CharacterSet;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

import net.es.oscars.api.soap.gen.v06.ListReply;
import net.es.oscars.api.soap.gen.v06.ListRequest;
import net.es.oscars.api.soap.gen.v06.ResDetails;
import net.es.oscars.client.OSCARSClient;
import net.es.oscars.client.OSCARSClientConfig;
import net.es.oscars.client.OSCARSClientException;
import net.es.oscars.common.soap.gen.OSCARSFaultMessage;

public class ListReservationsResource extends ServerResource {
	@Get
	public Representation list() throws Exception {
		String response = "";
		String status = "";
		try {
			try {
				status = (String) getRequestAttributes().get("status");
			} catch (Exception e) {
				System.out.println("Error");
			}
			
			if (status.equals(""))
				status = OSCARSClient.STATUS_ACTIVE;
			
			status = status.toUpperCase();
			
			// same as status in []
			if (!(status.equals(OSCARSClient.STATUS_ACCEPTED) ||
				status.equals(OSCARSClient.STATUS_ACTIVE) ||
				status.equals(OSCARSClient.STATUS_CANCELLED) ||
				status.equals(OSCARSClient.STATUS_FINISHED) ||
				status.equals(OSCARSClient.STATUS_INCOMMIT) ||
				status.equals(OSCARSClient.STATUS_INPATHCALCULATION)))
				throw new Exception("Invalid status");
			
			// Setup keystores
			OSCARSClientConfig.setClientKeystore("client", String.format(
					"%s/oscars-cert.jks", OSCARSListener.KEY_STORE),
					"client");
			
			OSCARSClientConfig.setSSLKeyStore(String.format(
					"%s/oscars-cert.jks", OSCARSListener.KEY_STORE), "client");

			// initialize client with service URL
			OSCARSClient client = new OSCARSClient(String.format(
					"https://%s:9001/OSCARS", OSCARSListener.OSCARS_SERVER));

			// Build request that asks for all ACTIVE and RESERVED reservations
			ListRequest request = new ListRequest();
				
			request.getResStatus().add(status);

			// send request
			ListReply reply = client.listReservations(request);

			// handle case where no reservations returned
			if (reply.getResDetails().size() == 0) {
				System.out.println("No " + status + " reservations found.");
				return answer("No " + status + " reservations found.");
			}

			// print reservations
			for (ResDetails resDetails : reply.getResDetails()) {
				response += "GRI: " + resDetails.getGlobalReservationId() + " ";
				response += "Login: " + resDetails.getLogin() + " ";
				response += "Status: " + resDetails.getStatus() + " ";
				response += "Start Time: "
						+ resDetails.getUserRequestConstraint().getStartTime() + " ";
				response += "End Time: "
						+ resDetails.getUserRequestConstraint().getEndTime() + " ";
				response += "Bandwidth: "
						+ resDetails.getUserRequestConstraint().getBandwidth();
				response += "\n";
			}
		} catch (OSCARSClientException e) {
			System.err.println("Error configuring client: " + e.getMessage());
			return answer("Error configuring client: " + e.getMessage());
		} catch (OSCARSFaultMessage e) {
			System.err.println("Error returned from server: " + e.getMessage());
			return answer("Error returned from server: " + e.getMessage());
		}
		catch (Exception e) {
			System.err.println("Error returned from server: " + e.getMessage());
			return answer("Error returned from server: " + e.getMessage());
		}

		return answer(response);
	}

	public Representation answer(String A) {
		Representation representation = new StringRepresentation(String.format(
				"{'response': '%s'}", A), MediaType.TEXT_PLAIN);
		representation.setCharacterSet(CharacterSet.UTF_8);
		return representation;
	}
}
