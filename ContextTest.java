import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class ContextTest extends SmallServer implements HttpHandler {


public void handle (HttpExchange exch)
{
    this.logReceive(exch.getRequestMethod(), exch.getRequestURI().getPath());
    doTest(exch);
}

public void doTest (HttpExchange exch)
{
    String method = exch.getRequestMethod();
    String path = exch.getRequestURI().getPath();
    String query = exch.getRequestURI().getQuery();

    HttpRes response;

    if (method.equals("GET")) {
        String resmsg = "GET request received";
        response = new HttpRes(200, resmsg);
    } else if ((method.equals("POST")) || (method.equals("PUT"))) {
        if (query == null) {
            String resmsg = "POST message received: null";
            response = new HttpRes(200, resmsg);
        } else {
            int eqindex = query.indexOf("=");
            String queryval = query.substring(eqindex + 1);
            String resmsg = "POST message received: " + queryval;
            response = new HttpRes(200, resmsg);
        }
    } else {
        response = HttpRes.notAllowedError(method, path);
    }

    response.contentType = null;
    sendResponse(exch, response);
}


}
