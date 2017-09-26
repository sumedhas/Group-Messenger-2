package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;

import edu.buffalo.cse.cse486586.groupmessenger2.OnPTestClickListener;
import edu.buffalo.cse.cse486586.groupmessenger2.R;

import static android.R.attr.port;
import static android.R.attr.priority;
import static android.R.id.message;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {
    static int seq_no = 0;
    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    static final String REMOTE_PORT0 = "11108";
    static final String REMOTE_PORT1 = "11112";
    static final String REMOTE_PORT2 = "11116";
    static final String REMOTE_PORT3 = "11120";
    static final String REMOTE_PORT4 = "11124";
    static final int SERVER_PORT = 10000;
    int S = 0;
    int pr = 0;
    String port;
    String server_port;
    String client_port;
    String server_message;
    String client_message;

    Comparator<String> comparator = new PriorityComparator();
    PriorityQueue<String> final_queue = new PriorityQueue<String>(10, comparator);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        port = myPort;

        setContentView(R.layout.activity_group_messenger);
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        findViewById(R.id.button1).setOnClickListener(new OnPTestClickListener(tv, getContentResolver()));
        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        }
        catch (IOException e) {
            Log.e(TAG, "Can't create a ServerSocket");
            return;
        }


        final EditText editText = (EditText) findViewById(R.id.editText1);
        final Button Send_Button = (Button) findViewById(R.id.button4);

        Send_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = editText.getText().toString() + "\n";
                editText.setText(""); // This is one way to reset the input box.
                TextView localTextView = (TextView) findViewById(R.id.textView1);
                localTextView.append("\t" + msg); // This is one way to display a string.
                localTextView.append("\n");

                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg);
            }
        });


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }


    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {



        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            try{
                while(true){
                    try {
                        Socket socket = serverSocket.accept();
                        DataInputStream in = new DataInputStream(socket.getInputStream());
                        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                        Iterator<String> it = final_queue.iterator();
                        String message_from_client = in.readUTF();
                        Log.d("Server_msg", "First message from client: " + message_from_client);
                        String[] total_client_msg = message_from_client.split("##");
                        Log.d("Server_msg", total_client_msg[4]);
                        int client_prior = Integer.parseInt(total_client_msg[1]);
                        server_port = total_client_msg[2];
                        client_port = total_client_msg[3];
                        server_message = total_client_msg[0];
                        Log.d("Server_msg", Integer.toString(client_prior));
                        if (total_client_msg[4].equals("u_new")) {

                            total_client_msg[4] = "u_pr";
                            //Reply with proposed priority (sequence no.)
                            if (client_prior > S) {
                                String client_msg_pr = total_client_msg[0] + "##" + total_client_msg[1] + "##" + total_client_msg[2] + "##" + total_client_msg[3] + "##" + total_client_msg[4];
                                Log.d("Server_msg", "proposed priority from server.. :" + client_msg_pr);
                                //Larger than all observed agreed priorities
                                //Larger than any previously proposed (by self) priority

                                //Store message in priority queue

                                out.writeUTF(client_msg_pr);
                                out.flush();
                                final_queue.add(client_msg_pr);
                                Log.d("Server_msg size", Integer.toString(final_queue.size()));
                                S = client_prior + 1;
                            } else if (client_prior <= S) {
                                String client_msg_pr = total_client_msg[0] + "##" + S + "##" + total_client_msg[2] + "##" + total_client_msg[3] + "##" + total_client_msg[4];
                                Log.d("Server_msg", "proposed priority from server.. :" + client_msg_pr);
                                //Larger than all observed agreed priorities
                                //Larger than any previously proposed (by self) priority

                                //Store message in priority queue

                                out.writeUTF(client_msg_pr);
                                out.flush();
                                Log.d("Server_msg","Proposed written to client");
                                final_queue.add(client_msg_pr);
                                Log.d("Server_msg size", Integer.toString(final_queue.size()));
                                S = S + 1;
                            }
                        }

                        //Upon receiving agreed (final) priority
                        String final_agreed_clpr = in.readUTF();
                        Log.d("Server_msg", final_agreed_clpr);
                        Log.d("Server_msg size", Integer.toString(final_queue.size()));

                        String total_final_agr_pr[] = final_agreed_clpr.split("##");
                        //Mark message as deliverable
                        total_final_agr_pr[4] = "deliverable";
                        String final_message = total_final_agr_pr[0] + "##" + total_final_agr_pr[1] + "##" + total_final_agr_pr[2] + "##" + total_final_agr_pr[3] + "##" + total_final_agr_pr[4];
                        while (it.hasNext()) {
                            String message = it.next();
                            String[] total_message = message.split("##");
                            if (total_final_agr_pr[0].equals(total_message[0]) && total_final_agr_pr[2].equals(total_message[2]) && total_final_agr_pr[3].equals(total_message[3])) {
                                final_queue.remove(message);
                                final_queue.add(final_message);
                            }
                        }
                        Log.d("Server_msg size", Integer.toString(final_queue.size()));
                        //Deliver any deliverable messages at the front of priority queue
                        Iterator<String> it1 = final_queue.iterator();
                        while (it1.hasNext()) {
                            String pub_message = it1.next();
                            String[] total_pub_message = pub_message.split("##");
                            if (total_pub_message[4].equals("deliverable")) {
                                Log.d("Server_msg","Publishing message : "+pub_message);
                                final_queue.remove(pub_message);
                                publishProgress(total_pub_message[0]);
                            }
                            else
                                break;
                        }
                        Log.d("Server_msg size", Integer.toString(final_queue.size()));

                        socket.close();

                    }catch(Exception e){
                        Log.e("Server_msg", e.toString());
                        Iterator<String> it2 = final_queue.iterator();
                        while (it2.hasNext()){
                            String message=it2.next();
                            Log.e("Server_msg","Checking message.. "+message);
                            Log.e("Server_msg","Port.. "+port);
                            String total_message[]=message.split("##");
                            if(total_message[3].equals(client_port)){
                                Log.e("Server_msg","removing message from queue.. : "+message);
                                final_queue.remove(message);
                            }

                        }
                        continue;
                    }

                }
            }
            catch(Exception e){
                Log.e(TAG,"IO Exception!");

            }
            return null;
        }

        protected void onProgressUpdate(String...strings) {
            String strReceived = strings[0].trim();
            TextView remoteTextView = (TextView) findViewById(R.id.textView1);
            remoteTextView.append(strReceived + "\t\n");
            remoteTextView.append("\n");

            //Content provider URI builder
            //Referred from https://developer.android.com/reference/android/net/Uri.Builder.html
            Uri.Builder uriBuilder = new Uri.Builder();
            uriBuilder.authority("edu.buffalo.cse.cse486586.groupmessenger2.provider");
            uriBuilder.scheme("content");
            Uri uri = uriBuilder.build();

            //Content values object
            ContentValues cv = new ContentValues();
            //Inserting Key-Value pairs
            cv.put("key", Integer.toString(seq_no));
            cv.put("value",strReceived);
            //Content resolver
            //Referred from https://developer.android.com/reference/android/content/Context.html
            getContentResolver().insert(uri,cv);
            seq_no=seq_no+1;
        }




    }

    /***
     * ClientTask is an AsyncTask that should send a string over the network.
     * It is created by ClientTask.executeOnExecutor() call whenever OnKeyListener.onKey() detects
     * an enter key press event.
     *
     * @author stevko
     *
     */
    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {
            try {
                String[] remote_ports = {REMOTE_PORT0, REMOTE_PORT1, REMOTE_PORT2, REMOTE_PORT3, REMOTE_PORT4};
                String msgToSend = msgs[0];
                pr ++;
                int max = 0;
                Socket[] socket_arr = new Socket[5];
                //Sender multicasts message to everyone

                for(int i = 0; i<remote_ports.length; i++){
                    try {

                        Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(remote_ports[i]));
                        socket_arr[i] = socket;
                        DataInputStream in = new DataInputStream(socket.getInputStream());
                        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                        client_message = msgToSend;
                        server_port = remote_ports[i];
                        String message_to_send = msgToSend + "##" + pr + "##" + remote_ports[i] + "##" + port + "##" + "u_new";
                        Log.d("Client_msg", "Sending message to server..: " + message_to_send);

                        out.writeUTF(message_to_send);
                        out.flush();
                        String message_from_server = in.readUTF();
                        Log.d("Client_msg", "Proposed priority from server..:: " + message_from_server);
                        String[] total_msg_from_server = message_from_server.split("##");
                        int server_pr = Integer.parseInt(total_msg_from_server[1]);
                        // Maximum of all proposed priorities

                        if (max < server_pr) {
                            max = server_pr;
                        }
                        Log.d("Client_msg", "Maximum priority is : " + max);
                    }catch(Exception e){
                        Log.e(TAG, e.toString());
                        continue;
                    }
                }

                //Sender chooses agreed priority, re-multicasts message with agreed priority
                for(int i = 0; i<remote_ports.length; i++){
                    try {
                        DataOutputStream out = new DataOutputStream(socket_arr[i].getOutputStream());
                        String final_msg_agr_pr = msgToSend + "##" + max + "##" + remote_ports[i] + "##" + port + "##" + "u_new";
                        Log.d("Client_msg","Sending agreed priority to client"+final_msg_agr_pr);
                        out.writeUTF(final_msg_agr_pr);
                        out.flush();
                        socket_arr[i].close();
                        out.close();
                    }catch(Exception e){
                        Log.e(TAG,e.toString());
                        continue;
                    }

                }

            } catch (Exception e) {
                Log.e(TAG, "ClientTask UnknownHostException");
            }
            return null;
        }
    }
}

class PriorityComparator implements Comparator<String>
{
    @Override
    public int compare(String x, String y)
    {
        String[] s1 = x.split("##");
        String[] s2 = y.split("##");
        Integer p1 = Integer.parseInt(s1[1]);
        Integer p2 = Integer.parseInt(s2[1]);
        if (p1<p2){
            return -1;
        }
        if (p1>p2){
            return 1;
        }
        return 0;
    }
}
