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

//propose two messages concurrently
public static void test_concurrent_put_two ()
{
    POJOReq req1 = new POJOReq(leader_node, "PUT", 
                                            "/keyValue-store/key1", 
                                            "{val: 'val1'}");
    POJOReq req2 = new POJOReq(challenger_node, "PUT", 
                                                "/keyValue-store/key2", 
                                                "{val: 'val2'}");
    Client cl1 = new Client(req1);
    Client cl2 = new Client(req2);
    cl1.fireAsync();
    cl2.fireAsync();
    while ((!cl1.done()) || (!cl2.done())) {
        try {
            Thread.sleep(200); //sleep 200ms
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    System.out.println(leader_node + ": " + cl1.getResponse().resBody);
    System.out.println(challenger_node + ": " + cl2.getResponse().resBody);
}

//propose 2k messages concurrently, alternating between leader and challenger
public static void test_concurrent_put_k_leader_challenger (int k)
{
    boolean is_challenger = false;
    Client[] clients = new Client[k * 2];
    for (int i = 0; i < clients.length; i += 1) {
        String node = leader_node;
        if (is_challenger) {
            node = challenger_node;
        }
        String key = "key" + i;
        String val = "val" + i;
        POJOReq req = new POJOReq(node, "PUT", "/keyValue-store/" + key, 
                                        "{val: '" + val + "'}");
        clients[i] = new Client(req);
        clients[i].fireAsync();
        is_challenger = !is_challenger;
    }
    int donecount = 0;
    while (donecount != clients.length) {
        for (int i = 0; i < clients.length; i += 1) {
            if (clients[i].done()) {
                donecount += 1;
            }
        }
        if (donecount != clients.length) {
            donecount = 0;
            try {
                Thread.sleep(200); //sleep 200ms
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    for (int i = 0; i < clients.length; i += 1) {
        HttpRes response = clients[i].getResponse();
        System.out.println("key" + i + ": " + response.resBody);
    }
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

public static void test_change_shard (String node, int number)
{
    String reqbody = "{num: '" + number + "'}";
    POJOReq req = new POJOReq(node, "POST", "/shard/changeShardNumber", reqbody);
    Client cl = new Client(req);
    cl.doSync();
    System.out.println(node + ": " + cl.getResponse().resBody);
}

public static void test_shard_get_this (String node)
{
    POJOReq req = new POJOReq(node, "GET", "/shard/my_id", null);
    Client cl = new Client(req);
    cl.doSync();
    System.out.println(node + ": " + cl.getResponse().resBody);
}

public static void test_shard_get_all (String node)
{
    POJOReq req = new POJOReq(node, "GET", "/shard/all_ids", null);
    Client cl = new Client(req);
    cl.doSync();
    System.out.println(node + ": " + cl.getResponse().resBody);
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

//    test_put_key(leader_node, "foo", "bar");
//    test_get_key(leader_node, "foo");

//    test_concurrent_put_k_leader_challenger(4);

//    test_add_view(new_node);

//    test_delete_view("10.0.0.3:4003");
//    test_delete_view("10.0.0.4:4004");

    //TODO: node crashes and immediately restarts
    //TODO: node crashes and never recovers

//    test_put_key(leader_node, "key1", "val1");
//    test_put_key(leader_node, "key2", "val2");
    test_change_shard(leader_node, 2);
//    for (String node : view) {
//        test_get_key(node, "key2");
//    }

    for (String node : view) {
        test_shard_get_this(leader_node);
        test_shard_get_all(leader_node);
    }
}


}
