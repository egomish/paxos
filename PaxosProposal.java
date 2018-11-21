//XXX: this class might be better off extending POJO
public class PaxosProposal {


public String toString ()
{
    String str = "";
    str += "{";
    str += "seqNum: " + seqNum + ", ";
    str += "reqIndex: " + reqIndex + ", ";
    str += "accValue: " + accValue + ", ";
    str += "canProceed: " + canProceed;
    str += "}";
    return str;
}

public PaxosProposal ()
{
    seqNum = 0;
    reqIndex = 0;
    accValue = null;
    canProceed = true;
}


public int seqNum;
public int reqIndex;
public String accValue;
public boolean canProceed;

}
