public class DockerTester {


public static String[] view = {"10.0.0.2:4002", 
                               "10.0.0.3:4003", "10.0.0.4:4004", 
//                               "10.0.0.5:4005", "10.0.0.6:4006", 
//                               "10.0.0.7:4007", "10.0.0.8:4008"
                               };

public static String leader_node = view[0];
public static String challenger_node = view[1];
public static String new_node = "10.0.0.9:4009";

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

//on <node>, <key>=?
public static void test_get_key (String node, String key)
{
    POJOReq request = new POJOReq(node, "GET", "/keyValue-store/" + key, null);
    Client cl = new Client(request);
    cl.doSync();
    System.out.println(node + ": " + cl.getResponse().resBody);
}

//propose <nodenum> messages concurrently
public static void test_concurrent_put (int nodenum)
{
    System.out.println("test not complete");
    return;
/*
    Client[] clients = new Client[nodenum];
    for (int i = 0; i < clients.length; i += 1) {
        key_index += 1;
        Client cl = new Client(view[i], "POST", 
                                        "/keyValue-store/" + keys[key_index], 
                                        "{val: '" + vals[key_index] + "'}");
        clients[i].fireAsync();
    }

    while (!Client.done(clients)) {
        try {
            Thread.sleep(200); //sleep 200ms
        } catch(InterruptedException e) {
            //do nothing
        }
    }

    for (int i = 0; i < clients.length; i += 1) {
        Client cl = clients[i];
        System.out.println(cl.getDestIP() + ": " + cl.getResponse().resBody);
    }
*/
}

//get current view
public static void test_get_view ()
{
    POJOReq request = new POJOReq(leader_node, "GET", "/view", null);
    Client cl = new Client(request);
    cl.doSync();
    System.out.println(leader_node + ": " + cl.getResponse().resBody);
}

//add node <ipport> to view
public static void test_add_view (String ipport)
{
    String reqbody = "{ip_port: '" + ipport + "'}";
    POJOReq reqput = new POJOReq(leader_node, "PUT", "/view", reqbody);
    Client put = new Client(reqput);
    put.doSync();

    POJOReq reqview = new POJOReq(leader_node, "GET", "/view", null);
    Client view = new Client(reqview);
    view.doSync();
    System.out.println(leader_node + ": " + view.getResponse().resBody);
}

//delete node <ipport> from view
public static void test_delete_view (String ipport)
{
    String reqbody = "{ip_port: '" + ipport + "'}";
    POJOReq reqdelete = new POJOReq(leader_node, "DELETE", "/view", reqbody);
    Client delete = new Client(reqdelete);
    delete.doSync();

    POJOReq reqview = new POJOReq(leader_node, "GET", "/view", null);
    Client view = new Client(reqview);
    view.doSync();
    System.out.println(leader_node + ": " + view.getResponse().resBody);
}

//make sure new nodes stay consistent with the cluster's history
public static void test_put_add_put ()
{
    key_index += 1;
    test_put_key(leader_node, keys[key_index], vals[key_index]);
    test_add_view(new_node);
    key_index += 1;
    test_put_key(leader_node, keys[key_index], vals[key_index]);
}

public static void
main (String[] args)
{
    //smoke test
//    test_hello();

    //put foo=bar
//    test_put_key(leader_node, "foo", "bar");
    //get foo
//    test_get_key(leader_node, "foo");

    //add to view
//    test_add_view(new_node);

    //make 2 concurrent requests
    test_concurrent_put(2);

    //TODO: sequence of concurrent requests
    //TODO: delete from view
    //TODO: node crashes and immediately restarts
    //TODO: node crashes and never recovers
}


}
