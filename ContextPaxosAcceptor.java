import java.io.IOException;
import java.net.URI;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;


public class ContextPaxosAcceptor extends SmallServer implements HttpHandler
{


public void handle (HttpExchange exch) throws IOException
{
    String path = exch.getRequestURI().getPath();
    System.err.println(this.receiveLog(exch.getRequestMethod(), path));

    if (path.startsWith("/paxos/acceptor/prepare")) {
        doPaxosAcceptorPrepare(exch);
    } else if (path.startsWith("/paxos/acceptor/accept")) {
        doPaxosAcceptorAccept(exch);
    } else if (path.startsWith("/paxos/acceptor/commit")) {
        doPaxosAcceptorCommit(exch);
    } else {
        sendResponse(exch, 404, POJOResBody.clientError().toJSON());
        return;
    }
}

private void doPaxosAcceptorPrepare (HttpExchange exch)
{
    String reqbody = Client.fromInputStream(exch.getRequestBody());
    POJOPaxosBody recv = POJOPaxosBody.fromJSON(reqbody);

    POJOResHttp response;

    if (recv.seqNum > this.seqNum) {
        //promise to agree and send currently accepted value
        this.seqNum = recv.seqNum;
        printLog(recv.seqNum + " -> ACK(prepare)");
        //XXX: only seqnum is required
        POJOPaxosBody resbody = new POJOPaxosBody(this.seqNum, this.accValue);
        response = new POJOResHttp(200, resbody.toJSON());
    } else {
        //the recv proposal is old--refuse it
        //but also send request history so the tardy node can catch up
        printLog(recv.seqNum + " -> NAK(" + this.seqNum + ").");
        String info = this.historyAsJSON();
        POJOResBody resbody = new POJOResBody(false, info);
        response = new POJOResHttp(409, resbody.toJSON());
    }
    sendResponse(exch, response);
}

private void doPaxosAcceptorAccept (HttpExchange exch)
{
    String reqbody = Client.fromInputStream(exch.getRequestBody());
    POJOPaxosBody recv = POJOPaxosBody.fromJSON(reqbody);

    POJOResHttp response;

    if (recv.seqNum >= this.seqNum) {
        //take the offered proposal because it's later (or the same)
        this.seqNum = recv.seqNum;
        this.accValue = recv.accValue;
        printLog(recv.seqNum + " -> ACK(accept)");
        POJOPaxosBody resbody = new POJOPaxosBody(this.seqNum, this.accValue);
        response = new POJOResHttp(200, resbody.toJSON());
    } else {
        //discard the offered proposal because it's outdated
        //but return requestHistory so the tardy node can catch up
        printLog(recv.seqNum + " -> NAK(" + this.toString() + ").");
        response = new POJOResHttp(409, this.historyAsJSON());
    }
    sendResponse(exch, response);
}

private void doPaxosAcceptorCommit (HttpExchange exch)
{
    String reqbody = Client.fromInputStream(exch.getRequestBody());
    POJOPaxosBody recv = POJOPaxosBody.fromJSON(reqbody);

    //store accepted value in requestHistory
    this.addToHistory(recv.seqNum, recv.accValue);

    //parse the committed value into an API call
    //XXX: if the client set the ip, it'll be clobbered
    //     (but why would the client ever set the ip?)
    POJOReqHttp request = POJOReqHttp.fromJSON(recv.accValue);
    request.ip = this.ipAndPort;
    request.service += "?consensus=true"; //XXXESG DEBUG

    //reset the proposal value so that new proposals can get accepted
    //XXX: this resets the seqnum and the value, not just the value
    this.accValue = null;

    //execute the API call
    Client cl = new Client(request);
    cl.doSync();

    POJOResHttp response = cl.getResponse();
    printLog(recv.seqNum + " -> ACK(commit: " + request.method + ")");
    sendResponse(exch, response);
}

public String toString ()
{
    String str = "";
    str += seqNum;
    str += ": ";
    if (accValue == null) {
        str += "null";
    } else {
        str += accValue;
    }
    return str;
}

protected ContextPaxosAcceptor ()
{
    seqNum = 0;
    accValue = null;
}


private int seqNum;
private String accValue;

}
