import java.io.IOException;
import java.net.URI;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;


public class ContextPaxosProposer extends BaseContext implements HttpHandler
{


public void handle (HttpExchange exch) throws IOException
{
    System.err.println("Handling " + exch.getRequestMethod() + " request...");
    if (!isPrimary) {
        HttpResponse response = forwardRequestToPrimary(exch);
        sendResponse(exch, response.getResponseCode(), response.getResponseBody(), null);
        return;
    }

    String path = exch.getRequestURI().getPath();
    if (!path.startsWith("/paxos/proposer")) {
        int rescode = 404;
        String restype = "application/json";
        String resmsg = ResponseBody.clientError().toJSON();
        sendResponse(exch, rescode, resmsg, restype);
        return;
    }

    doPaxosProposer(exch);
}

private void doPaxosProposer (HttpExchange exch)
{
    System.out.println("paxos!!");
    int rescode;
    String resmsg;
    String restype;

    //prepare
    String value = sendPrepare(theProposal);
    if (value != null) {
        theProposal.setAcceptedValue(value);
    } else {
        //set the value in theProposal to incoming argument
        System.out.println("value was null!!");
        theProposal.setAcceptedValue("this is a placeholder value");
    }

    //accept
    int seqnum = sendAccept(theProposal);

    //commit
    if (seqnum == theProposal.getSequenceNumber()) {
        boolean success = sendCommit(theProposal);
        if (success) {
            rescode = 200;
            restype = "application/json";
            ResponseBody resbody = new ResponseBody(true, "success is not implemented");
            resmsg = resbody.toJSON();
        } else {
            //TODO: rebroadcast instead of failing out
            rescode = 522;
            restype = "application/json";
            ResponseBody resbody = new ResponseBody(false, "broadcast failed, not rebroadcasting");
            resmsg = resbody.toJSON();
        }
    } else {
        //TODO: do something better here
        rescode = 501;
        restype = "application/json";
        ResponseBody resbody = new ResponseBody(false, "commit was refused");
        resmsg = resbody.toJSON();
    }

    sendResponse(exch, rescode, resmsg, restype);
}

private String sendPrepare (PaxosProposal prop)
{
    String value = null;
    String reqbody = prop.toJSON();
    HttpResponse[] responses = ClientRequest.sendBroadcastRequest(this.getNodeView(), "POST", "paxos/acceptor/prepare", null, reqbody);
    for (HttpResponse res : responses) {
        PaxosProposal resprop = PaxosProposal.fromJSON(res.getResponseBody());
        if (resprop.getAcceptedValue() != null) {
            //TODO: set the value if it's the latest, not if it's the last
            value = resprop.getAcceptedValue();
        }
    }
    return value;
}

private int sendAccept (PaxosProposal prop)
{
    String reqbody = prop.toJSON();
    HttpResponse[] responses = ClientRequest.sendBroadcastRequest(this.getNodeView(), "POST", "paxos/acceptor/accept", null, reqbody);
    for (HttpResponse res : responses) {
        PaxosProposal resprop = PaxosProposal.fromJSON(res.getResponseBody());
        if (resprop.getSequenceNumber() != prop.getSequenceNumber()) {
            //XXX: what if there are multiple seqnums?
            return resprop.getSequenceNumber();
        }
    }
    return prop.getSequenceNumber();
}

//TODO: use reilable broadcast
private boolean sendCommit (PaxosProposal prop)
{
    String reqbody = prop.toJSON();
    HttpResponse[] responses = ClientRequest.sendBroadcastRequest(this.getNodeView(), "POST", "paxos/commit", null, reqbody);

    for (HttpResponse res : responses) {
        int rescode = res.getResponseCode();
        if (rescode != 200) {
            return false;
        }
    }
    return true;
}


private PaxosProposal theProposal;

}
