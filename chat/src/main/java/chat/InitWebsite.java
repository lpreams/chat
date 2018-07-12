package chat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.core.UriBuilder;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

public class InitWebsite {
	public static void main(String[] args) {
		runServer();
	}
	
	private static void runServer() {
		
		Logger l = Logger.getLogger("org.glassfish.grizzly.http.server.HttpHandler");
		l.setLevel(Level.FINE);
		l.setUseParentHandlers(false);
		ConsoleHandler ch = new ConsoleHandler();
		ch.setLevel(Level.ALL);
		l.addHandler(ch);
		
		HttpServer server = GrizzlyHttpServerFactory.createHttpServer(UriBuilder.fromUri("http://0.0.0.0/").port(9090).build(), jaxrsConfig(), false);
		try {
			server.start();
		} catch (IOException e) {
			System.exit(26);
			throw new RuntimeException(e);
		}
		System.out.println("Server started");
	}
	
	private static ResourceConfig jaxrsConfig() {
		ArrayList<Class<?>> list = Stream.of(API.class).collect(Collectors.toCollection(ArrayList::new));
		ResourceConfig resourceConfig = new ResourceConfig(list.toArray(new Class<?>[0]));
		return resourceConfig;
	}
}
