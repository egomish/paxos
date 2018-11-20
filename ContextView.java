import java.io.IOException;
import java.net.URI;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;


public class ContextView extends SmallServer implements HttpHandler
{


public void handle (HttpExchange exch) throws IOException
{
    String uri = exch.getRequestURI().toString();
    String path = exch.getRequestURI().getPath();
    String query = exch.getRequestURI().getQuery();
    System.err.println(this.receiveLog(exch.getRequestMethod(), uri));

    if ((query == null) || (!query.equals("consensus=true"))) {
        POJOResHttp response = doProposal(exch);
        if (response.resCode != 200) {
            throw new UnsupportedOperationException();
        }
        sendResponse(exch, response);
        return;
    }

    doView(exch);
}

//XXX: We assume if the request has a body, it has a single key "ip_port"
private void doView (HttpExchange exch)
{
    String method = exch.getRequestMethod();
    URI uri = exch.getRequestURI();
    String path = uri.getPath();
    String newnode = parseIpPort(Client.fromInputStream(exch.getRequestBody()));

    POJOResHttp response;

    if (method.equals("GET")) {
        String resmsg = this.nodeViewAsJSON();
        response = new POJOResHttp(200, resmsg);
    } else if ((method.equals("PUT")) || (method.equals("POST"))) {
        //TODO: make sure newnode isn't null
        boolean success = this.addToView(newnode);
        if (!success) {
            POJOResBody resbody = new POJOResBody(false, "node not added");
            resbody.debug = newnode;
            resbody.result = "Error";
            resbody.msg = newnode + " already in view";
            response = new POJOResHttp(404, resbody.toJSON());
        } else {
            POJOResHttp joinres = sendJoinTo(newnode);
            if (joinres.resCode == 200) {
                POJOResBody resbody = new POJOResBody(true, "node added");
                resbody.debug = newnode;
                resbody.result = "Success";
                resbody.msg = "Successfully added " + newnode + " to view";
                response = new POJOResHttp(200, resbody.toJSON());
            } else {
                //XXX: it would be very very strange if this happened
                POJOResBody resbody = new POJOResBody(false, "view update failed");
                resbody.debug = newnode;
                resbody.result = "Error";
                resbody.msg = "Added " + newnode + " to cluster, but couldn't update view";
                response = new POJOResHttp(500, resbody.toJSON());
            }
        }
    } else if (method.equals("DELETE")) {
        boolean success = this.removeFromView(newnode);
        if (!success) {
            POJOResBody resbody = new POJOResBody(false, "node not removed");
            resbody.debug = newnode;
            resbody.result = "Error";
            resbody.msg = newnode + "is not in view";
            response = new POJOResHttp(404, resbody.toJSON());
        } else {
            POJOResBody resbody = new POJOResBody(true, "node removed");
            resbody.debug = newnode;
            resbody.result = "Success";
            resbody.msg = "Successfully removed " + newnode + " from view";
            response = new POJOResHttp(200, resbody.toJSON());
        }
    } else {
        POJOResBody resbody = new POJOResBody(false, "method not supported");
        response = new POJOResHttp(405, resbody.toJSON());
    }
    sendResponse(exch, response);
}

private POJOResHttp sendJoinTo (String newnode)
{
    String view = this.getNodeViewAsString();
    String history = "\"" + this.getHistoryAsJSON() + "\"";
    POJOViewHistory reqbody = new POJOViewHistory(view, history);
    Client cl = new Client(newnode, "PUT", "/join", reqbody.toJSON());
    cl.doSync();
    POJOResHttp response = cl.getResponse();
    return response;
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
