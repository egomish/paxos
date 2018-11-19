//XXX: this class might be better off extending POJO
public class PaxosResponse {


public String toString ()
{
    String str = "";
    str += "{";
    str += "resCode: " + resCode + ", ";
    str += "seqNum: " + seqNum + ", ";
    str += "accValue: " + accValue + ", ";
    str += "reqHistory: " + reqHistory + ", ";
    str += "canProceed: " + canProceed;
    str += "}";
    return str;
}

public PaxosResponse ()
{
    resCode = 0;
    seqNum = 0;
    accValue = null;
    reqHistory = null;
    canProceed = true;
}


public int resCode;
public int seqNum;
public String accValue;
public String reqHistory;
public boolean canProceed;

}
