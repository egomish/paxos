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
    PaxosResponse paxosres = new PaxosResponse();
    while (this.hasProposalQueue()) {
         //sleep a random brief time to reduce the risk of duelling proposers
        try {
            int sleeptime = (int)(Math.random() * 80); //0-80ms
            Thread.sleep(sleeptime);
        } catch (InterruptedException e) {
            //do nothing
        }
        try {
            paxosres = doPaxosProposal(this.popProposal());
            if (paxosres.resCode == 409) {
                //the proposal was refused because it was too old
                this.pushProposal(request);
                POJOHistory history = POJOHistory.fromJSON(paxosres.reqHistory);
                this.replayHistory(history);
            } else if (paxosres.resCode == 522) {
                System.err.println("WARNING: Incomplete commit.\n" + 
                                   "Implement reliable broadcast on commit to resolve this warning.");
            } else {
                System.out.println("paxosres: " + paxosres.toString());
            }
        } catch (TimeoutException e) {
            //push the proposal back on the stack to try again
            System.out.println("proposal timed out: " + paxosres.toString());
            this.pushProposal(request);
            paxosres.resCode = 503;
        } catch (Exception e) {
            System.out.println("Something terrible happened.");
            this.pushProposal(request);
            e.printStackTrace();
            paxosres.resCode = 500;
        }
    }
    if (this.hasProposalQueue()) {
        System.out.println(requestBuffer.size() + " requests not committed!!");
    }
    String history = this.getHistoryAsJSON();
    POJOResHttp res = new POJOResHttp(paxosres.resCode, history);
    sendResponse(exch, res);
}

private PaxosResponse doPaxosProposal (String request) throws TimeoutException
{
    //prepare
    PaxosResponse paxosres;
    int seqnum = this.incrementSequenceNumber();
    int reqindex = this.reqHistory.nextIndex();
    String value = null;

    paxosres = sendPrepare(seqnum, reqindex);
    if (!paxosres.canProceed) {
//        System.out.println("prepare failed");
//        System.out.println("proposal: " + paxosres);
        return paxosres;
    }

    value = paxosres.accValue;
    if (value == null) {
        //no value has been chosen, so it's okay to propose one
        value = request;
    }
    System.err.println(log("agreed on: '" + value.substring(22) + "'"));

    //accept
    paxosres = sendAccept(seqnum, reqindex, value);
    if (!paxosres.canProceed) {
//        System.out.println("accept failed");
        return paxosres;
    }

    if (paxosres.seqNum != seqnum) {
//        System.out.println("no longer leader");
        return paxosres;
    }

    //commit
    boolean finished = sendCommit(seqnum, reqindex, value);
    if (!finished) {
        paxosres.resCode = 522;
        System.out.println("commit failed");
        return paxosres;
    }
    paxosres.resCode = 200;
    System.out.println("consensus!: " + paxosres.toString());
    return paxosres;
}

private PaxosResponse sendPrepare (int seqnum, int reqindex) 
        throws TimeoutException
{
    String value = null;
    POJOPaxosBody prop = new POJOPaxosBody(reqindex, seqnum, value);

    PaxosResponse paxosres = new PaxosResponse();

    System.err.println(log("preparing: " + prop.toJSON()));

    //broadcast prepare to cluster
    Client[] multi = Client.readyMulticast(this.getNodeView(), 
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
        paxosres.resCode = res.resCode;
        if (paxosres.resCode == 200) {
            POJOPaxosBody result = POJOPaxosBody.fromJSON(res.resBody);
            if (result.accValue != null) {
                paxosres.accValue = result.accValue;
                //there's already an accepted value--just propagate that
                return paxosres;
            } else {
                votenum += 1;
            }
        } else if (paxosres.resCode == 409) {
            //the response was old--get the more up-to-date history
            POJOResBody resbody = POJOResBody.fromJSON(res.resBody);
            paxosres.reqHistory = resbody.info;
            paxosres.canProceed = false;
        } else {
            /*
             *  Acceptor only returns 200 or 409, so any other rescode means 
             *  something bad happened. Just drop the response on the floor.
             */
        }
    }

    if (votenum < (multi.length / 2 + 1)) {
        //too many servers are down--fail the proposal
        paxosres.canProceed = false;
    }
    return paxosres;
}

private PaxosResponse sendAccept (int seqnum, int reqindex, String request) 
        throws TimeoutException
{
    POJOPaxosBody prop = new POJOPaxosBody(reqindex, seqnum, request);
    PaxosResponse paxosres = new PaxosResponse();

    Client[] multi = Client.readyMulticast(this.getNodeView(), 
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
        paxosres.resCode = res.resCode;
        if (paxosres.resCode == 200) {
            POJOPaxosBody result = POJOPaxosBody.fromJSON(res.resBody);
            if (result.seqNum > paxosres.seqNum) {
                //accept the newer proposal
                paxosres.seqNum = result.seqNum;
            }
        } else if (paxosres.resCode == 409) {
            //the response was old--get the more up-to-date history
            POJOResBody resbody = POJOResBody.fromJSON(res.resBody);
            paxosres.reqHistory = resbody.info;
            paxosres.canProceed = false;
        } else {
            /*
             *  Acceptor only returns 200 or 409, so any other rescode means 
             *  something bad happened. Just drop the response on the floor.
             */
        }
    }

    return paxosres;
}

private boolean sendCommit (int seqnum, int reqindex, String value)
{
    POJOPaxosBody prop = new POJOPaxosBody(reqindex, seqnum, value);
    Client[] multi = Client.readyMulticast(this.getNodeView(), 
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
    for (Client cl : multi) {
        POJOResHttp res = cl.getResponse();
        if ((res.resCode < 200) || (res.resCode >= 300)) {
            //commit failed--resend
            //TODO: resend until successful
            System.out.println("got rescode " + res.resCode + " from " + cl.getDestIP());
            System.out.println("resbody: " + res.resBody);
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
