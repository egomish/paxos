import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.util.ArrayList;

public class ContextPaxos extends SmallServer implements HttpHandler {


public void handle (HttpExchange exch)
{
    this.logReceive(exch.getRequestMethod(), exch.getRequestURI().getPath());

    String method = exch.getRequestMethod();
    String path = exch.getRequestURI().getPath();

    HttpRes response;

    if (path.startsWith("/paxos/propose")) {
        response = doPaxosPropose(exch);
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

private HttpRes doPaxosPropose (HttpExchange exch)
{
    //extract the KVS request from the HTTP request and add it to the queue
    String reqstr = Client.fromInputStream(exch.getRequestBody());
    POJOReq request = POJOReq.fromJSON(reqstr);

    int seqnum = this.nextProposalNumber();
    int reqindex = this.getNextHistoryIndex();
    HttpRes paxosres = null;

    //do some paxos!
    while (request != null) {
        POJOPaxos prop = new POJOPaxos(seqnum, reqindex, null);
        System.out.println("new proposal: " + prop.toJSON());

        //prepare
        int didprepare = 0;
        POJOReq value = null;
        for (String node : this.getNodeView()) {
            POJOReq req = new POJOReq(node, "POST", "/paxos/prepare", prop.toJSON());
            Client cl = new Client(req);
            this.log("sending prepare to " + node + "...");
            cl.doSync();
            HttpRes res = cl.getResponse();
            if (res.resCode == 200) {
                //this proposal is recent enough
                didprepare += 1;
                POJOPaxos prepres = POJOPaxos.fromJSON(res.resBody);
                //TODO: check acceptor seqnum and choose latest accValue
                if (prepres.accValue != null) {
                    value = prepres.accValue;
                    System.out.println("already value: " + prepres.accValue.toJSON());
                }
            } else if (res.resCode == 408) {
                //this proposal is old--catch up new history if there is any
                POJOPaxos histreq = POJOPaxos.fromJSON(res.resBody);
                if (histreq != null) {
                    //there is new history--propagate historical req
                    this.nextProposalNumber(histreq.seqNum);
                    reqindex = histreq.reqIndex;
                    value = histreq.accValue;
                }
                continue;
            } else {
                //something went wrong--drop the response on the floor
                System.out.println("something went wrong");
            }
        }
        if (didprepare < this.getQuorumSize()) {
            //failed to reach majority on prepare--try again
            this.nextProposalNumber();
            continue;
        }
        this.log("Successfully completed prepare phase.");

        //accept
        System.out.println("setting prop value to: " + value.toJSON());
        //----| begin transaction |----
        prop.accValue = value;
        if (value == null) {
            prop.accValue = request;
        }
        //----|  end transaction  |----
        int didaccept = 0;
        for (String node : this.getNodeView()) {
            POJOReq req = new POJOReq(node, "POST", "/paxos/accept", prop.toJSON());
            Client cl = new Client(req);
            this.log("sending accept to " + node + "...");
            cl.doSync();
            HttpRes res = cl.getResponse();
            if (res.resCode == 200) {
                //this proposal was accepted
                //Lamport's paxos would have us check seqnum, but 200 means OK
                didaccept += 1;
            } else if (res.resCode == 408) {
                //this proposal is old--catch up new history if there is any
                POJOPaxos histreq = POJOPaxos.fromJSON(res.resBody);
                if (histreq != null) {
                    //there is new history--propagate historical request
                    this.nextProposalNumber(histreq.seqNum);
                    reqindex = histreq.reqIndex;
                    value = histreq.accValue;
                }
                continue;
            } else {
                //something went wrong--drop the response on the floor
                System.out.println("something went wrong");
            }
        }
        if (didaccept < this.getQuorumSize()) {
            //failed to reach majority on accept--try again
            this.nextProposalNumber();
            continue;
        }
        this.log("Successfully completed accept phase.");

        //commit
        boolean success = true;
        for (String node : this.getNodeView()) {
            POJOReq req = new POJOReq(node, "POST", "/paxos/commit", prop.toJSON());
            Client cl = new Client(req);
            this.log("sending commit to " + node + "...");
            cl.doSync();
            HttpRes res = cl.getResponse();
            if (res.resCode != 200) {
                System.out.println("failed to commit on " + node);
                success = false;
            }
        }
        if (success) {
            request = null;
            paxosres = new HttpRes(200, Integer.toString(prop.reqIndex));
            paxosres.contentType = null;
        } else {
            request = null;
            paxosres = new HttpRes(513, "commit failed");
            paxosres.contentType = null;
        }
        break;
    }

    //return the result of the consensus
    return paxosres;
}

private HttpRes doPaxosPrepare (HttpExchange exch)
{
    String recvstr = Client.fromInputStream(exch.getRequestBody());
    POJOPaxos recv = POJOPaxos.fromJSON(recvstr);

    HttpRes response;

    POJOReq hist = this.getHistoryAt(recv.reqIndex);
    if (hist == null) {
        hist = this.accValue;
    }
    if (recv.seqNum > this.seqNum) {
        //promise to agree and respond with accepted value at reqindex
        this.log(recv.seqNum + " -> ACK(prepare [" + recv.reqIndex + "]).");
        this.seqNum = recv.seqNum;
        POJOPaxos resbody = new POJOPaxos(recv.seqNum, recv.reqIndex, hist);
        response = new HttpRes(200, resbody.toJSON());
    } else {
        //the proposal is old--refuse it, but help the tardy node catch up
        //XXX: we might be propagating too early here and hit duelling proposers
        this.log(recv.seqNum + " -> NAK(" + this.seqNum + ").");
        POJOPaxos resbody = new POJOPaxos(this.seqNum, recv.reqIndex, hist);
        response = new HttpRes(408, resbody.toJSON());
    }

    return response;
}

private HttpRes doPaxosAccept (HttpExchange exch)
{
    String recvstr = Client.fromInputStream(exch.getRequestBody());
    POJOPaxos recv = POJOPaxos.fromJSON(recvstr);

    HttpRes response;

    if (recv.seqNum >= this.seqNum) {
        //the proposal is recent--accept it
        this.log(recv.seqNum + " -> ACK(accept [" + recv.reqIndex + "]).");
        //----| begin transaction |----
        this.seqNum = recv.seqNum;
        this.accValue = recv.accValue;
        //----|  end transaction  |----
        //respond with this.seqNum, recv.reqIndex, and this.accValue
        response = new HttpRes(200, recv.toJSON());
    } else {
        //the proposal is old--refuse it
        //but send the already-committed request so the tardy node catches up
        this.log(recv.seqNum + " -> NAK(" + this.seqNum + ").");
        //respond with history[recv.reqIndex], or 
        //             request currently being proposed for recv.reqIndex, or 
        //             null if not request is currently being proposed
        POJOReq hist = this.getHistoryAt(recv.reqIndex);
        if (hist == null) {
            hist = this.accValue;
        }
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

    //add the request to history
    System.out.println("history before: " + reqHistory.size());
    this.addToHistoryAt(recv.reqIndex, request);
    System.out.println("history after: " + reqHistory.size());

    //propagate the commit (reliable broadcast)
    this.log("(reliable broadcast not implemented)");

    //reset the proposal value to new proposals can get accepted
    this.accValue = new POJOReq();

    this.log(recv.seqNum + " -> ACK(committed [" + recv.reqIndex + "]).");
    response = new HttpRes(200, "committed");
    response.contentType = null;

    return response;
}

//XXX: the magic number 100 is the max number of processes in the paxos cluster
protected int nextProposalNumber ()
{
    return this.nextProposalNumber(this.monotonicNumber);
}

//XXX: the magic number 100 is the max number of processes in the paxos cluster
protected int nextProposalNumber (int propnum)
{
    this.monotonicNumber = propnum / 100;
    this.monotonicNumber += 1;
    return (this.monotonicNumber * 100) + this.processID;
}

protected ContextPaxos ()
{
    monotonicNumber = 0;
    seqNum = 0;
    accValue = new POJOReq();
}


private int monotonicNumber;
private int seqNum;
private POJOReq accValue;

}
