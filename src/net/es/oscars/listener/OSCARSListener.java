package net.es.oscars.listener;

import org.restlet.Application;
import org.restlet.Component;
import org.restlet.Restlet;
import org.restlet.data.Protocol;
import org.restlet.routing.Router;

public class OSCARSListener extends Application {
	public static String LISTEN_PORT = null;
	public static String OSCARS_SERVER = null;
	public static String OSCARS_DOMAIN = null;
	public static String KEY_STORE = null;
	
    @Override
    public synchronized Restlet createInboundRoot() {
        Router router = new Router(getContext());
        
        router.attach("/create", CreateReservationResource.class);
        router.attach("/request", RequestReservationResource.class);
        router.attach("/cancel", CancelReservationResource.class);
        router.attach("/cancel/{gri}", CancelReservationResource.class);
        router.attach("/list/{status}", ListReservationsResource.class);

        return router;
    }
	
	public static void main(String[] args) {
		try {

			if ((args == null) || (args.length != 4)) {
				// Display program arguments
				System.err.println("Can't launch the web server. List of "
						+ "required arguments:\n"
						+ " 1) Port to listen on\n"
						+ " 2) OSCARS Server URL.\n"
						+ " 3) OSCARS Domain.\n"
						+ " 4) OSCARS Keystores directory.\n");
			} else {
				OSCARSListener.LISTEN_PORT = args[0];
				OSCARSListener.OSCARS_SERVER = args[1];
				OSCARSListener.OSCARS_DOMAIN = args[2];
				OSCARSListener.KEY_STORE = args[3];
				
			    // Create a new Component.
			    Component component = new Component();

			    // Add a new HTTP server listening on port LISTEN_PORT.
			    component.getServers().add(Protocol.HTTP, Integer.parseInt(LISTEN_PORT));

			    // Attach the sample application.
			    component.getDefaultHost().attach("", new OSCARSListener());

			    // Start the component.
			    component.start();

			}
		} catch (Exception e) {
			System.err.println("Can't launch the web server.\nAn unexpected "
					+ "exception occured:");
			e.printStackTrace(System.err);
		}
	}
}