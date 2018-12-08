import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.net.URLDecoder;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;

public class ContextView extends SmallServer implements HttpHandler {


public void handle (HttpExchange exch)
{
    this.logReceive(exch.getRequestMethod(), exch.getRequestURI().getPath());

    POJOReq request = this.parseRequest(exch);
    request = this.consensusProtocol(request);
    HttpRes response;

    response = doView(request);
    sendResponse(exch, response);
}

public HttpRes doView (POJOReq request)
{
    String method = request.reqMethod;

    int rescode;
    POJOView resbody;
    HttpRes response;

    if (method.equals("GET")) {
        rescode = 200;
        resbody = new POJOView(this.getNodeView());
    } else if ((method.equals("PUT")) || (method.equals("POST"))) {
        String ipport = POJOIPPort.fromJSON(request.reqBody).ip_port;
        //help the newly-added node catch up on history
        String history = new POJOHistory(this.getHistory()).toJSON();
        POJOReq histreq = new POJOReq(ipport, "POST", "/history", history);
        Client cl = new Client(histreq);
        cl.doSync();

        //if the newly-added node has a recent history, add it to the view
        HttpRes res = cl.getResponse();
        resbody = new POJOView();
        if (res.resCode == 200) {
            //the node has the history--add it to the view
            boolean added = this.addToView(ipport);
            if (added) {
                rescode = 200;
                resbody.result = "Success";
                resbody.msg = "Successfully added " + ipport + " to view";
            } else {
                rescode = 404;
                resbody.result = "Error";
                resbody.msg = ipport + " is already in view";
            }
        } else {
            rescode = 503;
            resbody.result = "Error";
            resbody.msg = "Could not get " + ipport + " caught up";
            //if we wanted to STNITH tardy nodes, this might be the place
        }
    } else if (method.equals("DELETE")) {
        String ipport = POJOIPPort.fromJSON(request.reqBody).ip_port;
        resbody = new POJOView();
        boolean success = this.removeFromView(ipport);
        if (success) {
            rescode = 200;
            resbody.result = "Success";
            resbody.msg = "Successfully removed " + ipport + " from view";
            response = new HttpRes(200, resbody.toJSON());
        } else {
            rescode = 404;
            resbody.result = "Error";
            resbody.msg = ipport + " is not in view";
        }
    } else {
        rescode = 405;
        resbody = new POJOView();
        resbody.result = "Error";
        resbody.msg = method + " " + request.reqURL + " not allowed";
    }

    resbody.payload = this.getHistory();
    response = new HttpRes(rescode, resbody.toJSON());
    return response;
}


}
