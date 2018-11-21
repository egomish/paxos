import java.io.IOException;
import java.net.URI;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;


public class ContextView extends SmallServer implements HttpHandler
{


public void handle (HttpExchange exch) throws IOException
{
    String uri = exch.getRequestURI().toString();
    String query = exch.getRequestURI().getQuery();
    System.err.println(this.receiveLog(exch.getRequestMethod(), uri));

    POJOResHttp response;

    if ((query == null) || (!query.contains("fromhistory=true"))) {
        //get consensus to do the request
        POJOResHttp paxosres = doProposal(exch);
        if (paxosres.resCode == 200) {
            int resultindex;
            try {
                String info = POJOResBody.fromJSON(paxosres.resBody).info;
                resultindex = Integer.parseInt(info);
            } catch (NumberFormatException e) {
                //something really bad happened if info isn't reqindex
                e.printStackTrace();
                throw e;
            }
            //do requests up to and including this one
            response = this.playHistoryTo(resultindex);
        } else {
            //something terrible has happened while getting consensus
            throw new IllegalStateException();
        }

        //return the result to the client
        sendResponse(exch, response);
        return;
    }

    //query contains "fromhistory=true"--actually execute the request
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
        System.out.println("resmsg: " + resmsg);
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
            POJOResBody resbody = new POJOResBody(true, "node added");
            resbody.debug = newnode;
            resbody.result = "Success";
            resbody.msg = "Successfully added " + newnode + " to view";
            response = new POJOResHttp(200, resbody.toJSON());
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
