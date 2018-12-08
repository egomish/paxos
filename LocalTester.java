public class LocalTester {


public static String[] view = {"localhost:8080"};

public static String leader_node = view[0];

public static String[] keys = {"key0", "key1", "key2", "key3", "key4", "key5"};
public static String[] vals = {"val0", "val1", "val2", "val3", "val4", "val5"};
public static int key_index = -1;


//get "Hello world!" from all nodes in the cluster
public static void test_hello ()
{
    for (String node : view) {
        POJOReq request = new POJOReq(node, "GET", "/hello", null);
        Client cl = new Client(request);
        cl.doSync();
        System.out.println(node + ": " + cl.getResponse().resBody);
    }
}

//on <node>, <key>=<val>
public static void test_put_key (String node, String key, String val)
{
    String reqbody = "{val: '" + val + "'}";
    POJOReq request = new POJOReq(node, "POST", "/keyValue-store/" + key, reqbody);
    Client cl = new Client(request);
    cl.doSync();
    System.out.println(node + ": " + cl.getResponse().resBody);
}

public static void test_delete_key (String node, String key)
{
    POJOReq request = new POJOReq(node, "DELETE", "/keyValue-store/" + key, null);
    Client cl = new Client(request);
    cl.doSync();
    System.out.println(node + ": " + cl.getResponse().resBody);
}

//on <node>, <key>=?
public static void test_get_key (String node, String key)
{
    POJOReq request = new POJOReq(node, "GET", "/keyValue-store/" + key, null);
    Client cl = new Client(request);
    cl.doSync();
    System.out.println(node + ": " + cl.getResponse().resBody);
}

public static void
main (String[] args)
{
    //smoke test
//    test_hello();

    //put key on [0], put key on [1], delete key from [0], put key on [0], get key from [1]
    test_put_key(leader_node, "foo", "bar");
    test_put_key(leader_node, "baz", "bat");
    test_delete_key(leader_node, "foo");
    test_put_key(leader_node, "blah", "blahval");
    test_get_key(leader_node, "baz");
}


}
