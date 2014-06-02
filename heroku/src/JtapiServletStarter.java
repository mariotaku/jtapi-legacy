import java.io.IOException;
import javax.servlet.ServletException;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.*;
import org.mariotaku.jtapi.*;

public class JtapiServletStarter {
	
	public static void main(String[] args) throws Exception {
		Server server = new Server(Integer.valueOf(System.getenv("PORT")));
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setContextPath("/");
		server.setHandler(context);
		context.addServlet(new ServletHolder(new JtapiServlet()), "/*");
		server.start();
		server.join();
	}
	
}