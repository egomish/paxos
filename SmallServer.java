import java.net.InetSocketAddress;
import com.sun.net.httpserver.HttpServer;


public class SmallServer
{


public static void
main(String[] args) throws Exception
{
    //get the ip and port from environment variables
    String ipport = System.getenv().get("IP_PORT");
    String[] strarr  = ipport.split(":");
    String ip = strarr[0];
    int port;
    try {
        port = Integer.parseInt(strarr[1]);
    } catch (NumberFormatException e) {
        e.printStackTrace();
        port = 8080;
    }

    HttpServer server = HttpServer.create(new InetSocketAddress(ip, port), 0);
    System.err.println("Server running on port " + port + ".");
//    server.createContext("/", new SmallServer());
    server.createContext("/hello", new ContextHello());
    server.createContext("/test", new ContextTest());
    server.createContext("/keyValue-store", new ContextKVS());
    server.createContext("/paxos/proposer", new ContextPaxosProposer());
    server.createContext("/paxos/acceptor", new ContextPaxosAcceptor());
    server.createContext("/history", new ContextHistory());
    server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
//    server.setExecutor(null); // creates a default executor
    server.start();
}


}
