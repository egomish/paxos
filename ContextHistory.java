import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class ContextHistory extends SmallServer implements HttpHandler {


public void handle (HttpExchange exch)
{
    this.logReceive(exch.getRequestMethod(), exch.getRequestURI().getPath());
    doHistory(exch);
}

public void doHistory (HttpExchange exch)
{
    String method = exch.getRequestMethod();
    String path = exch.getRequestURI().getPath();

    HttpRes response;

    if (method.equals("GET")) {
        POJOHistory history = new POJOHistory(this.getHistory());
        response = new HttpRes(200, history.toJSON());
    } else if ((method.equals("POST")) || (method.equals("PUT"))) {
        String reqbody = Client.fromInputStream(exch.getRequestBody());
        POJOReq[] history = POJOHistory.fromJSON(reqbody).history;
        //----| begin transaction |----
        for (int i = 0; i < history.length; i += 1) {
            this.addToHistoryAt(i, history[i]);
        }
        //----|  end transaction  |----
        response = new HttpRes(200, Integer.toString(history.length));
        response.contentType = null;
    } else {
        String resmsg = method + " " + path + " not allowed";
        response = new HttpRes(405, resmsg);
        response.contentType = null;
    }

    sendResponse(exch, response);
}


}
