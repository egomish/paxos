import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.net.URI;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.util.LinkedList;


public class ContextPaxosProposer extends SmallServer implements HttpHandler
{


public void handle (HttpExchange exch) throws IOException
{
    String path = exch.getRequestURI().getPath();
    System.err.println(this.receiveLog(exch.getRequestMethod(), path));

    //extract the KVS request from the HTTP request and add it to the queue
    String request = Client.fromInputStream(exch.getRequestBody());
    this.queueProposal(request);

    //get consensus about everything that's happened up to and 
    //including this most recent request
    PaxosResponse response = new PaxosResponse();
    while (this.hasProposalQueue()) {
        try {
            int sleeptime = (int)(Math.random() * 80); //0-80ms
            System.out.println("sleeping " + sleeptime + "ms!!");
            Thread.sleep(sleeptime);
        } catch (InterruptedException e) {
            //do nothing
        }
        try {
            response = doPaxosProposal(this.popProposal());
            if (response.resCode == 409) {
                //the proposal was refused because it was too old
                //TODO: use history in the resbody to catch up
                System.out.println("total history: " + response.reqHistory);
                this.pushProposal(request);
            } else if (response.resCode == 200) {
                //the proposal was successful
                System.out.println("success!");
                break;
            } else {
                //a known bad thing happened while doing the request
                System.out.println("proposal failed, rescode: " + response.resCode);
                break;
            }
        } catch (TimeoutException e) {
            //push the proposal back on the stack to try again
            this.pushProposal(request);
            response.resCode = 503;
            System.out.println("proposal timed out: " + response.toString());
        } catch (Exception e) {
            this.pushProposal(request);
            System.out.println("Something terrible happened.");
            e.printStackTrace();
            response.resCode = 500;
        }
    }
    if (this.hasProposalQueue()) {
        System.out.println(requestBuffer.size() + " requests not committed!!");
    }
    POJOResHttp res = new POJOResHttp(response.resCode, this.historyAsJSON());
    sendResponse(exch, res);
}

private PaxosResponse doPaxosProposal (String request) throws TimeoutException
{
    //prepare
    PaxosResponse response;
    int seqnum = this.incrementSequenceNumber();
    String value = null;

    response = sendPrepare(seqnum);
    if (!response.canProceed) {
        System.out.println("prepare failed");
        return response;
    }

    value = response.accValue;
    if (value == null) {
        //no value has been chosen, so it's okay to propose one
        value = request;
    }
    System.out.println(log("agreed on: '" + value.substring(0, 25) + "'"));

    //accept
    response = sendAccept(seqnum, value);
    if (!response.canProceed) {
        System.out.println("accept failed");
        return response;
    }

    if (response.seqNum != seqnum) {
        System.out.println("no longer leader");
        return response;
    }

    //commit
    boolean finished = sendCommit(seqnum, value);
    if (!finished) {
        response.resCode = 522;
        System.out.println("commit failed");
        return response;
    }
    response.resCode = 200;
    System.out.println("consensus!: " + response.toString());
    return response;
}

private PaxosResponse sendPrepare (int seqnum) throws TimeoutException
{
    String value = null;
    POJOPaxosBody prop = new POJOPaxosBody(seqnum, value);

    PaxosResponse paxosres = new PaxosResponse();

    System.out.println(log("preparing: " + prop.toJSON()));

    //broadcast prepare to cluster
    Client[] multi = Client.readyMulticast(this.nodeView, 
                                           "POST", "/paxos/acceptor/prepare", 
                                           prop.toJSON());
    for (Client cl : multi) {
        cl.fireAsync();
    }

    /*
     *  The Paxos algorithm only needs to wait for a majority of responses, 
     *  but realistically because this program is implemented using Java (with 
     *  exception handling for errors) on top of TCP (a reliable protocol), 
     *  we're going to hear back from all of the nodes--although we can't (and 
     *  don't) assume the responses will indicate success.
     */
    /*
     *  Paxos actually needs only a majority. Please see the notes for details.
     */
    //wait until all the nodes respond
    while (!Client.done(multi)) {
        try {
            Thread.sleep(200); //sleep 200ms
        } catch (InterruptedException e) {
            //do nothing
        }
    }

    //process the responses
    int votenum = 0;
    for (Client cl : multi) {
        POJOResHttp res = cl.getResponse();
        if (res.resCode == 200) {
            POJOPaxosBody result = POJOPaxosBody.fromJSON(res.resBody);
            if (result.accValue != null) {
                paxosres.accValue = result.accValue;
                //there's already an accepted value--just propagate that
                return paxosres;
            } else {
                votenum += 1;
            }
        } else if (res.resCode == 409) {
            //the response was old--get the more up-to-date history
            POJOResBody resbody = POJOResBody.fromJSON(res.resBody);
            paxosres.reqHistory = resbody.info;
            paxosres.canProceed = false;
        } else {
            //Acceptor only returns rescode 200 or 409
            paxosres.canProceed = false;
        }
    }

    if (votenum < (multi.length / 2 + 1)) {
        //too many servers are down--fail the proposal
        paxosres.canProceed = false;
    }
    return paxosres;
}

private PaxosResponse sendAccept (int seqnum, String request) throws TimeoutException
{
    POJOPaxosBody prop = new POJOPaxosBody(seqnum, request);
    PaxosResponse paxosres = new PaxosResponse();

    Client[] multi = Client.readyMulticast(this.nodeView, 
                                           "POST", "/paxos/acceptor/accept", 
                                           prop.toJSON());
    for (Client cl : multi) {
        cl.fireAsync();
    }

    /*
     *  The Paxos algorithm only needs to wait for a majority of responses, 
     *  but realistically because this program is implemented using Java (with 
     *  exception handling for errors) on top of TCP (a reliable protocol), 
     *  we're going to hear back from all of the nodes--although we can't (and 
     *  don't) assume the responses will indicate success.
     */
    while (!Client.done(multi)) {
        try {
            Thread.sleep(200); //sleep 200ms
        } catch (InterruptedException e) {
            //do nothing
        }
    }

    //process the responses
    for (Client cl : multi) {
        POJOResHttp res = cl.getResponse();
        if (res.resCode == 200) {
            POJOPaxosBody result = POJOPaxosBody.fromJSON(res.resBody);
            if (result.seqNum > paxosres.seqNum) {
                //accept the newer proposal
                paxosres.seqNum = result.seqNum;
            }
        } else if (res.resCode == 409) {
            //the response was old--get the more up-to-date history
            POJOResBody resbody = POJOResBody.fromJSON(res.resBody);
            paxosres.reqHistory = resbody.info;
            paxosres.canProceed = false;
        } else {
            //Acceptor only returns rescode 200 or 409
            paxosres.canProceed = false;
        }
    }

    return paxosres;
}

private boolean sendCommit (int seqnum, String value)
{
    POJOPaxosBody prop = new POJOPaxosBody(seqnum, value);
    PaxosResponse paxosres = new PaxosResponse();

    Client[] multi = Client.readyMulticast(this.nodeView, 
                                           "POST", "/paxos/acceptor/commit", 
                                           prop.toJSON());

    for (Client cl : multi) {
        cl.fireAsync();
    }

    while (!Client.done(multi)) {
        try {
            Thread.sleep(200); //sleep 200ms
        } catch (InterruptedException e) {
            //do nothing
        }
    }

    //process the responses
    int latestseqnum = 0;
    for (Client cl : multi) {
        POJOResHttp res = cl.getResponse();
        if ((res.resCode < 200) || (res.resCode >= 300)) {
            //commit failed--resend
            //TODO: resend until successful
            System.out.println("got bad rescode " + res.resCode);
            return false;
        }
    }
    return true;
}

private boolean hasProposalQueue ()
{
    if (requestBuffer.size() != 0) {
        return true;
    }
    return false;
}

private String popProposal ()
{
    return requestBuffer.removeFirst();
}

private void pushProposal (String request)
{
    requestBuffer.addFirst(request);
}

private void queueProposal (String request)
{
    requestBuffer.addLast(request);
}

//XXX: the magic number 100 is the max number of processes in the paxos cluster
private int incrementSequenceNumber ()
{
    this.monotonicNumber += 1;
    return (this.monotonicNumber * 100) + this.processID;
}

protected ContextPaxosProposer ()
{
    monotonicNumber = 0;
    requestBuffer = new LinkedList<String>();
}


private int monotonicNumber;
private LinkedList<String> requestBuffer;

}
