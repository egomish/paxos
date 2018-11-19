import java.io.IOException;
import java.net.URI;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;


public class ContextTest extends SmallServer implements HttpHandler
{


public void handle (HttpExchange exch) throws IOException
{
    String path = exch.getRequestURI().getPath();
    System.err.println(this.receiveLog(exch.getRequestMethod(), path));

    if (!isPrimary) {
        POJOResHttp response = forwardToPrimary(exch);
        sendResponse(exch, response.resCode, response.resBody);
        return;
    }

    doTest(exch);
}

private void doTest (HttpExchange exch)
{
    String method = exch.getRequestMethod();
    URI uri = exch.getRequestURI();
    String path = uri.getPath();
    String query = uri.getQuery();
    int rescode = 200;
    String resmsg = "";
    if (method.equals("GET")) {
        String info = "GET message received";
        resmsg = new POJOResBody(true, info).toJSON();
    } else if (method.equals("POST")) {
        if (query == null) {
            String info = "POST message received: null";
            resmsg = new POJOResBody(true, info).toJSON();
        } else {
            int eqindex = query.indexOf("=");
            String info = "POST message received: " 
                        + query.substring(eqindex + 1);
            resmsg = new POJOResBody(true, info).toJSON();
        }
    } else {
        rescode = 405;
        String info = method + " " + path + " not allowed";
        resmsg = new POJOResBody(false, info).toJSON();
    }
    sendResponse(exch, rescode, resmsg);
}


}
