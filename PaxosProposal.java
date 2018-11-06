import java.io.InputStream;

public class PaxosProposal extends ResponseBody {


public boolean isLaterThan (PaxosProposal that)
{
    if (that == null) {
        return true;
    }

    String[] thisarr = this.sequenceNumber.split(".");
    String[] thatarr = that.getSequenceNumber().split(".");
    try {
        int thisseq = Integer.parseInt(thisarr[0]);
        int thatseq = Integer.parseInt(thatarr[0]);
        if (thisseq > thatseq) {
            return true;
        }

        int thispid = Integer.parseInt(thisarr[1]);
        int thatpid = Integer.parseInt(thatarr[1]);
        if (thispid > thatpid) {
            return true;
        }
    } catch (NumberFormatException e) {
        //it would be very strange if this ever happened
        e.printStackTrace();
        System.out.println("number format exception for paxos message " + sequenceNumber);
    }
    return false;
}

public String getSequenceNumber ()
{
    return sequenceNumber;
}

public boolean getVote ()
{
    return voteOfNode;
}

public String toJSON ()
{
    return "{'tag': 'valid JSON'}";
}

public PaxosProposal (InputStream in)
{
    //convert in to json
    //convert json to POJO

    sequenceNumber = null;
    proposalUnderVote = null;
    voteOfNode = null;
}

public PaxosProposal (String json)
{
    //convert json to POJO

    sequenceNumber = null;
    proposalUnderVote = null;
    voteOfNode = null;
}

public PaxosProposal (String seq, PaxosMessage prop)
{
    this(seq, prop, null);
}

public PaxosProposal (String seq, PaxosMessage prop, Boolean vote)
{
    sequenceNumber = seq;
    proposalUnderVote = prop;
    voteOfNode = vote;
}


private String sequenceNumber;
private PaxosMessage proposalUnderVote;
private Boolean voteOfNode;

}
