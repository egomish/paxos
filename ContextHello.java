import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class ContextHello extends SmallServer implements HttpHandler {


public void handle (HttpExchange exch)
{
    this.logReceive(exch.getRequestMethod(), exch.getRequestURI().getPath());
    doHello(exch);
}

public void doHello (HttpExchange exch)
{
    String method = exch.getRequestMethod();
    String path = exch.getRequestURI().getPath();

    HttpRes response;

    if (method.equals("GET")) {
        String resmsg = "Hello world!";
        response = new HttpRes(200, resmsg);
    } else {
        response = HttpRes.notAllowedError(method, path);
    }

    response.contentType = null;
    sendResponse(exch, response);
}


}
