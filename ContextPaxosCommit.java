import java.io.IOException;
import java.net.URI;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;


public class ContextPaxosCommit extends BaseContext implements HttpHandler
{


public void handle (HttpExchange exch) throws IOException
{
    Logger.log_request_in(this.getClass(), exch);

    String path = exch.getRequestURI().getPath();
    if (!path.startsWith("/paxos/commit")) {
        int rescode = 404;
        String restype = "application/json";
        String resmsg = ResponseBody.clientError().toJSON();
        sendResponse(exch, rescode, resmsg, restype);
        return;
    }

    doPaxosCommit(exch);
}

private void doPaxosCommit (HttpExchange exch)
{
    String reqbody = ClientRequest.inputStreamToString(exch.getRequestBody());
    PaxosProposal received = PaxosProposal.fromJSON(reqbody);
    String tocommit = received.getAcceptedValue();

    //TODO: actually commit the proposal
        //parse tocommit into a API call
        //execute the API call

    int rescode = 200;
    String restype = "application/json";
    ResponseBody resbody = new ResponseBody(true, "didn't actually commit but woulda");
    String resmsg = resbody.toJSON();
    System.out.println("ACK(" + tocommit + ")");
    sendResponse(exch, rescode, resmsg, restype);
}


}
