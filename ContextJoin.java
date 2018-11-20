import java.io.IOException;
import java.net.URI;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;


public class ContextJoin extends SmallServer implements HttpHandler
{


public void handle (HttpExchange exch) throws IOException
{
    String uri = exch.getRequestURI().toString();
    System.err.println(this.receiveLog(exch.getRequestMethod(), uri));

    doJoin(exch);
}

private void doJoin (HttpExchange exch)
{
    String method = exch.getRequestMethod();
    POJOResHttp response;

    if ((method.equals("PUT")) || (method.equals("POST"))) {
        String reqbody = Client.fromInputStream(exch.getRequestBody());
        POJOViewHistory vh = POJOViewHistory.fromJSON(reqbody);

        String view = vh.view;
        String[] nodes = view.split(",");
        for (String node : nodes) {
            this.nodeView.add(node);
        }

        String history = vh.history.substring(1, vh.history.length() - 1);
        this.replayHistory(POJOHistory.fromJSON(history));

        POJOResBody resbody = new POJOResBody(true, "view updated");
        response = new POJOResHttp(200, resbody.toJSON());
    } else {
        POJOResBody resbody = new POJOResBody(false, "method not supported");
        response = new POJOResHttp(405, resbody.toJSON());
    }
    sendResponse(exch, response);
}

private String parseIpPort (String reqbody)
{
    if (reqbody == null) {
        return null;
    }
    //remove outer braces and return value without surrounding quotes
    reqbody = reqbody.substring(1, reqbody.length() - 1);
    String[] strarr = reqbody.split(": ");
    return strarr[1].substring(1, strarr[1].length() - 1);
}


}
