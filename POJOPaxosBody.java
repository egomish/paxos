import com.google.gson.Gson;

public class POJOPaxosBody extends POJO {


//TODO: use Generics in base class
public static POJOPaxosBody fromJSON (String json)
{
    POJOPaxosBody pojo;
    try {
        Gson gson = new Gson();
        pojo = gson.fromJson(json, POJOPaxosBody.class);
    } catch (Exception e) {
        System.err.println("JSON exception: " + e.getMessage());
        //XXX: creating an empty object seems like a bad idea for debugging
        pojo = new POJOPaxosBody();
    }
    return pojo;
}

public POJOPaxosBody ()
{
    this(null, null, null);
}

public POJOPaxosBody (Integer s, Integer r, String a)
{
    seqNum = s;
    reqIndex = r;
    accValue = a;
}

public Integer seqNum;
public Integer reqIndex;
public String accValue;

}
