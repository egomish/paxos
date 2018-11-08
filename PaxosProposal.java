import com.google.gson.Gson;

public class PaxosProposal extends ResponseBody {


public void incrementSequenceNumber ()
{
    sequenceNumber += 1;
}

public void setSequenceNumber (int n)
{
    sequenceNumber = n;
}

public int getSequenceNumber ()
{
    return sequenceNumber;
}

public void setAcceptedValue (String str)
{
    acceptedValue = str;
}

public String getAcceptedValue ()
{
    return acceptedValue;
}

//XXX: test this or remove it
//XXX: untested--what happens is the arg isn't JSON?
//XXX: when the arg isn't JSON, a JsonSyntaxException is throws and an empty ResponseBody object is returned
public static PaxosProposal fromJSON (String json)
{
    System.out.println("parsing JSON: '" + json + "'.");
    PaxosProposal body = new PaxosProposal();
    try {
        Gson gson = new Gson();
        body =  gson.fromJson(json, body.getClass());
    } catch (Exception e) {
        System.out.println("JSON exception: " + e.getMessage());
        body = (PaxosProposal)ResponseBody.serverError();
    }
    return body;
}

public PaxosProposal ()
{
    super();
    sequenceNumber = 0;
    acceptedValue = null;
}

private int sequenceNumber;
private String acceptedValue;


}
