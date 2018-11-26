import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.net.URLDecoder;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;

public class ContextView extends SmallServer implements HttpHandler {


public void handle (HttpExchange exch)
{
    this.logReceive(exch.getRequestMethod(), exch.getRequestURI().getPath());

    String query = exch.getRequestURI().getQuery();

    HttpRes response;

    if ((query != null) && (query.contains("fromhistory=true"))) {
        //the request is from the server's history--execute it
        response = doView(exch);
    } else {
        //do consensus to add this request to the server's history
        String ip = this.ipAndPort;
        String method = exch.getRequestMethod();
        String url = exch.getRequestURI().toString();
        String reqbody = Client.fromInputStream(exch.getRequestBody());
        POJOReq prop = new POJOReq(ip, method, url, reqbody);
        Client cl = new Client(new POJOReq(ip, "POST", "/paxos/propose", prop.toJSON()));
        cl.doSync();
        HttpRes res = cl.getResponse();
        if (res.resCode == 200) {
            try {
                int reqindex = Integer.valueOf(res.resBody);
                response = this.playHistoryTo(reqindex);
            } catch (NumberFormatException e) {
                //something has gone horribly wrong if resbody isn't reqindex
                e.printStackTrace();
                response = HttpRes.serverError();
            }
        } else if (res.resCode == 513) {
            //there was consensus but not all nodes could be reached
            response = res;
        } else {
            response = HttpRes.serverError();
        }
    }

    sendResponse(exch, response);
}

public HttpRes doView (HttpExchange exch)
{
    String method = exch.getRequestMethod();
    String path = exch.getRequestURI().getPath();

    int rescode;
    POJOView resbody;
    HttpRes response;

    if (method.equals("GET")) {
        rescode = 200;
        resbody = new POJOView(this.getNodeView());
    } else if ((method.equals("PUT")) || (method.equals("POST"))) {
        //XXX: get node caught up, THEN add it to the cluster
        String body = Client.fromInputStream(exch.getRequestBody());
        String ipport = POJOIPPort.fromJSON(body).ip_port;
        boolean newlyadded = this.addToView(ipport);
        resbody = new POJOView(this.getNodeView());
        if (newlyadded) {
            //get the new node caught up
            String history = new POJOHistory(this.getHistory()).toJSON();
            POJOReq request = new POJOReq(ipport, "POST", "/history", history);
            Client cl = new Client(request);
            cl.doSync();
            HttpRes res = cl.getResponse();
            if (res.resCode == 200) {
                rescode = 200;
                resbody.result = "Success";
                resbody.msg = "Successfully added " + ipport + " to view";
            } else {
                rescode = 513;
                resbody.result = "Error";
                resbody.msg = ipport + " added, but without history";
            }
        } else {
            rescode = 404;
            resbody.result = "Error";
            resbody.msg = ipport + " is already in view";
        }
//    } else if (method.equals("DELETE")) {
    } else {
        rescode = 405;
        resbody = new POJOView();
        resbody.result = "Error";
        resbody.msg = method + " " + path + " not allowed";
    }

    response = new HttpRes(rescode, resbody.toJSON());
    return response;
}


}
