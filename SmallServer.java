import java.net.InetSocketAddress;
import com.sun.net.httpserver.HttpServer;


public class SmallServer
{


public static void
main(String[] args) throws Exception
{
    int port = 8080;
    HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
    System.err.println("Server running on port " + port + ".");
//    server.createContext("/", new SmallServer());
    server.createContext("/hello", new ContextHello());
    server.createContext("/test", new ContextTest());
    server.createContext("/keyValue-store", new ContextKVS());
    server.createContext("/paxos/proposer", new ContextPaxosProposer());
    server.createContext("/paxos/acceptor", new ContextPaxosAcceptor());
    server.setExecutor(null); // creates a default executor
    server.start();
}


}
