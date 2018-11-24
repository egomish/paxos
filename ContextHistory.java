import java.io.IOException;
import java.net.URI;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;


public class ContextHistory extends SmallServer implements HttpHandler
{


public void handle (HttpExchange exch) throws IOException
{
    String path = exch.getRequestURI().getPath();
    System.err.println(this.receiveLog(exch.getRequestMethod(), path));
    doHistory(exch);
}


private void doHistory (HttpExchange exch)
{
    String method = exch.getRequestMethod();
    URI uri = exch.getRequestURI();
    String path = uri.getPath();

    POJOResHttp response;
    int rescode;
    String resmsg = "";
    if (method.equals("GET")) {
        POJOHistory history = this.reqHistory;
        response = new POJOResHttp(200, history.toJSON());
    } else if ((method.equals("PUT")) || (method.equals("POST"))) {
        String reqbody = Client.fromInputStream(exch.getRequestBody());
        POJOHistory history = POJOHistory.fromJSON(reqbody);
        this.replayHistory(history);
        POJOResBody resbody = new POJOResBody(true, "replayed history");
        response = new POJOResHttp(200, resbody.toJSON());
    } else {
        String info = method + " " + path + " not allowed";
        POJOResBody resbody = new POJOResBody(false, info);
        response = new POJOResHttp(405, resbody.toJSON());
    }
    sendResponse(exch, response);
}


}
