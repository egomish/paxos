import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.util.ArrayList;

public class ContextPaxos extends SmallServer implements HttpHandler {


public void handle (HttpExchange exch)
{
//    this.logReceive(exch.getRequestMethod(), exch.getRequestURI().getPath());

    String method = exch.getRequestMethod();
    String path = exch.getRequestURI().getPath();

    HttpRes response;

    if (path.startsWith("/paxos/propose")) {
        //---- begin transaction ---
try {
        response = doPaxosPropose(exch);
} catch (Exception e) {
    e.printStackTrace();
    response = null;
}
        //---- end transaction ---
    } else if (path.startsWith("/paxos/prepare")) {
        response = doPaxosPrepare(exch);
    } else if (path.startsWith("/paxos/accept")) {
        response = doPaxosAccept(exch);
    } else if (path.startsWith("/paxos/commit")) {
        response = doPaxosCommit(exch);
    } else {
        String resmsg = method + " " + path + " not allowed";
        response = new HttpRes(405, resmsg);
        response.contentType = null;
    }

    sendResponse(exch, response);
}

private synchronized HttpRes doPaxosPropose (HttpExchange exch)
{
    //extract the KVS request from the HTTP request
    String reqstr = Client.fromInputStream(exch.getRequestBody());
    POJOReq request = POJOReq.fromJSON(reqstr);

    HttpRes res = null;
    int reqindex = -1;
    while (reqindex < 0) {
        this.log("proposing " + request.reqURL + "...");
        int seqnum = nextProposalNumber();
        if (reqindex == -1) {
            reqindex = this.getNextHistoryIndex();
        }
        reqindex = doProposal(seqnum, reqindex, request);
    }
    HttpRes response = new HttpRes(200, Integer.toString(reqindex));
    return response;
}

/*
 *  Returns new index of request in reqHistory, or -1 if it couldn't be added.
 */
private int doProposal (int seqnum, int reqindex, POJOReq request)
{
    POJOPaxos prop = new POJOPaxos(seqnum, reqindex, null);
    int commiti = reqindex;

    //prepare
    POJOReq preparedvalue = null;
    int preparedindex = reqindex;
    int didprepare = 0;
    for (String node : this.getNodeView()) {
        POJOReq req = new POJOReq(node, "POST", "/paxos/prepare", prop.toJSON());
        Client cl = new Client(req);
        cl.doSync();
        HttpRes res = cl.getResponse();
        if (res.resCode == 200) {
            //the proposal is recent
            didprepare += 1;
            //TODO: choose latest accValue, not last
            POJOPaxos prepres = POJOPaxos.fromJSON(res.resBody);
            if (!prepres.accValue.isEmpty()) {
                preparedindex = prepres.reqIndex;
                preparedvalue = prepres.accValue;
            }
        } else if (res.resCode == 408) {
            //the proposal is too old for the acceptor--do nothing
        } else if (res.resCode == 503) {
            //the acceptor is behind--drop the response on the floor
            //XXX: if we want the acceptor to catch up, this might be the place
        } else {
            //something went wrong--drop the response on the floor
        }
    }
    if (didprepare < this.getQuorumSize()) {
        //failed to reach majority on prepare--try again
        this.log("prepare failed--no majority");
        commiti = -1;
        return commiti;
    } else {
        this.log("got prepare quorum");
    }

    //accept
    if (preparedvalue != null) {
        //the originally proposed request won't be completed this proposal
        commiti = -1;
        prop.reqIndex = preparedindex;
        prop.accValue = preparedvalue;
    } else {
        //proceed with the originally proposed request
        prop.accValue = request;
    }
    int didaccept = 0;
    for (String node : this.getNodeView()) {
        POJOReq req = new POJOReq(node, "POST", "/paxos/accept", prop.toJSON());
        Client cl = new Client(req);
        cl.doSync();
        HttpRes res = cl.getResponse();
        if (res.resCode == 200) {
            didaccept += 1;
            //Lamport's paxos would have us store the seqnum, but 200 means OK
        } else if (res.resCode == 408) {
            //this proposal is old--catch up and try again
            commiti = -1;
            return commiti;
        } else if (res.resCode == 503) {
            //the acceptor is behind--drop the response on the floor
            //XXX: if we want the acceptor to catch up, this might be the place
        } else {
            //something went wrong--drop the response on the floor
        }
    }
    if (didaccept < this.getQuorumSize()) {
        //failed to reach majority on accept--try again
        this.log("accept failed--no majority");
        commiti = -1;
        if (didaccept > 0) {
            this.log("at least one acceptor accepted");
            //XXX: adding another fail case doesn't work--or this isn't enough
            commiti = -2;
        }
        return commiti;
    } else {
        this.log("consensus!");
    }
    /*
     *  Consensus! Tell the cluster so they can do consensus again later.
     */

    //commit
    for (String node : this.getNodeView()) {
        POJOReq req = new POJOReq(node, "POST", "/paxos/commit", prop.toJSON());
        Client cl = new Client(req);
        cl.doSync();
        HttpRes res = cl.getResponse();
        if (res.resCode != 200) {
            //If we wanted to know about a network partition (or a 
            //potentially-dead node), this is the easiest place to detect it.
        }
    }
    return commiti;
}

private HttpRes doPaxosPrepare (HttpExchange exch)
{
try {
    String recvstr = Client.fromInputStream(exch.getRequestBody());
    POJOPaxos recv = POJOPaxos.fromJSON(recvstr);

    HttpRes response;

    //get the request from history at the reqindex--it will be
    //                     a request that was already committed, or
    //                     a request that is currently being proposed, or
    //                     null if no request is currently proposed
    POJOReq hist = this.getHistoryAt(recv.reqIndex);
    if (hist == null) {
        hist = new POJOReq();
    }

    if (recv.seqNum > this.seqNum) {
        int reqindex = this.getNextHistoryIndex();
        if (recv.reqIndex > reqindex + 1) {
            //this acceptor fell behind and is missing history--refuse it
            this.log("[Acceptor] " + recv.seqNum + " -> NAK(missing history for prepare");
            POJOPaxos resbody = new POJOPaxos(this.seqNum, reqindex, null);
            response = new HttpRes(503, resbody.toJSON());
        }
        //promise to agree and respond with accepted value at reqindex
        this.log("[Acceptor] " + recv.seqNum + " -> ACK(prepare [" + recv.reqIndex + "] " + hist + ").");
        this.seqNum = recv.seqNum;
        System.out.println("[Acceptor] history[" + recv.reqIndex + "] is: " + hist);
        POJOPaxos resbody = new POJOPaxos(recv.seqNum, recv.reqIndex, hist);
        response = new HttpRes(200, resbody.toJSON());
    } else {
        //the proposal is old--refuse it
        this.log("[Acceptor] " + recv.seqNum + " -> NAK(" + this.seqNum + ").");
        POJOPaxos resbody = new POJOPaxos(this.seqNum, recv.reqIndex, hist);
        response = new HttpRes(408, resbody.toJSON());
    }

    return response;
} catch (Exception e) {
    e.printStackTrace();
    return null;
}
}

private HttpRes doPaxosAccept (HttpExchange exch)
{
    String recvstr = Client.fromInputStream(exch.getRequestBody());
    POJOPaxos recv = POJOPaxos.fromJSON(recvstr);

    HttpRes response;

    //get the request from history at the reqindex--it will be
    //                     a request that was already committed, or
    //                     a request that is currently being proposed, or
    //                     null if no request is currently proposed
    POJOReq hist = this.getHistoryAt(recv.reqIndex);
    if (hist == null) {
        hist = new POJOReq();
    }

    if (recv.seqNum >= this.seqNum) {
        int reqindex = this.getNextHistoryIndex();
        if (recv.reqIndex > reqindex) {
            //this acceptor fell behind and is missing history--refuse it
            this.log("[Acceptor] " + recv.seqNum + " -> NAK(missing history for accept).");
            POJOPaxos resbody = new POJOPaxos(this.seqNum, reqindex, null);
            response = new HttpRes(503, resbody.toJSON());
            //TODO: catch up this node
        } else {
            //the proposal is recent and the acceptor is caught up--accept it
            //----| begin transaction |----
            this.seqNum = recv.seqNum;
            this.addToHistoryAt(recv.reqIndex, recv.accValue);
            this.log("history[" + recv.reqIndex + "]: " + recv.accValue);
            //----|  end transaction  |----
            this.log("[Acceptor] " + recv.seqNum + " -> ACK(accept [" + recv.reqIndex + "] " + recv.accValue + ").");
            //respond with this.seqNum, recv.reqIndex, and history[recv.reqIndex]
            response = new HttpRes(200, recv.toJSON());
        }
    } else {
        //the proposal is old--refuse it
        //but send the already-committed request so the tardy node catches up
        this.log("[Acceptor] " + recv.seqNum + " -> NAK(" + this.seqNum + ").");
        POJOPaxos resbody = new POJOPaxos(this.seqNum, recv.reqIndex, hist);
        response = new HttpRes(408, resbody.toJSON());
    }

    return response;
}

private HttpRes doPaxosCommit (HttpExchange exch)
{
    String recvstr = Client.fromInputStream(exch.getRequestBody());
    POJOPaxos recv = POJOPaxos.fromJSON(recvstr);

    HttpRes response;

    //parse the proposal value into an API call
    POJOReq request = recv.accValue;

    //add the request to history, if it isn't already there
    //XXX: what if there was already a value but it wasn't <request>?
    this.addToHistoryAt(recv.reqIndex, request);

    //expose the proposal value so it's really in the history
    System.out.println("exposing history at " + recv.reqIndex);
    this.exposeHistoryAt(recv.reqIndex);

    this.log("[Acceptor] " + recv.seqNum + " -> ACK(committed [" + recv.reqIndex + "] " + request + ").");
    response = new HttpRes(200, "committed");
    response.contentType = null;

    return response;
}

//XXX: the magic number 100 is the max number of processes in the paxos cluster
protected int nextProposalNumber ()
{
    this.monotonicNumber += 1;
    return (this.monotonicNumber * 100) + this.processID;
}

//XXX: the magic number 100 is the max number of processes in the paxos cluster
protected int nextProposalNumber (int propnum)
{
    this.monotonicNumber = propnum / 100;
    return nextProposalNumber();
}

protected ContextPaxos ()
{
    monotonicNumber = 0;
    seqNum = 0;
}


private int monotonicNumber;
private int seqNum;

}
