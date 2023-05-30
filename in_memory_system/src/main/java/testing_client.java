import redis.clients.jedis.Jedis;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class testing_client extends Thread {
    private JButton button1;
    private JTextField textField1;
    private JLabel l;
    JPanel panel1;
    private JLabel reading_message_client;
    private JButton button_quit;
    private JTextArea textArea1;
    private final Socket socket;
    private Jedis jedis;

    public void redis_log(String s_or_c,int counter){
        List<String> time_redis_c = jedis.lrange("time_array_c", 0, -1);
        List<String> reading_redis_c = jedis.lrange("reading_array_c", 0, -1);
        List<String> sending_redis_c = jedis.lrange("sending_array_c", 0, -1);
        String log_client = "log_client";
        if (Objects.equals(s_or_c, "s")){
            String message_field = "Message "+(counter+1)+" sent from Client";
            String message_value = sending_redis_c.get(counter);
            jedis.hset(log_client, message_field, message_value);

            String time_field_s = "Time "+(counter+1);
            String value = time_redis_c.get(counter);
            jedis.hset(log_client, time_field_s, value);
        }
        else if(Objects.equals(s_or_c, "r")){
            String message_field_c = "Message "+(counter+1)+" sent from Server";
            String message_value_c = reading_redis_c.get(counter);
            jedis.hset(log_client,message_field_c,message_value_c);

            String time_field_c = "Time "+(counter+1);
            String time_value = time_redis_c.get(counter);
            jedis.hset(log_client,time_field_c,time_value);
        }
    }
    public void log_adding_client(String message,String flag_value){

        LocalDateTime date_time = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String time_now = date_time.format(formatter);
        jedis.rpush("time_array_c", time_now);
        jedis.rpush("flag_array_c", flag_value);
        if (Objects.equals(flag_value,"r")){
            jedis.rpush("reading_array_c", message);
        }
        else {
            jedis.rpush("sending_array_c", message);
        }
    }
    public void log_writer(){
        try {
            FileWriter my_writer = new FileWriter("/home/vely/IdeaProjects/in_memory_system/src/main/java/client_redis_log.txt");
            List<String> time_redis_c = jedis.lrange("time_array_c", 0, -1);
            List<String> flag_redis_c = jedis.lrange("flag_array_c", 0, -1);
            List<String> reading_redis_c = jedis.lrange("reading_array_c", 0, -1);
            List<String> sending_redis_c = jedis.lrange("sending_array_c", 0, -1);
            int counter_server =0;
            int counter_client =0;
            int j =0;
            for (String flag : flag_redis_c) {

                if (flag.equals("s")) {
                    my_writer.write(time_redis_c.get(j)+"["+jedis.get("ip_c")+"]"+"\tMe: "+sending_redis_c.get(counter_server)+"\n");
                    redis_log("s",counter_server);
                    counter_server++;
                }
                else if (flag.equals("r")) {
                    my_writer.write(time_redis_c.get(j)+"["+jedis.get("ip_c")+"]"+"\tServer: "+reading_redis_c.get(counter_client)+"\n");
                    redis_log("r",counter_client);
                    counter_client++;
                }
                j++;
            }
            my_writer.close();
            System.out.println("Dosya başarıyla oluşturuldu ve yazıldı.");
        } catch (IOException z) {
            System.out.println("Bir hata oluştu.");
            z.printStackTrace();
        }
        System.out.println("\n\nClient closed...");
        System.exit(0);
    }
    public void chat_setting (){
        textArea1.setText("");
        List<String> time_redis_c = jedis.lrange("time_array_c", 0, -1);
        List<String> flag_redis_c = jedis.lrange("flag_array_c", 0, -1);
        List<String> reading_redis_c = jedis.lrange("reading_array_c", 0, -1);
        List<String> sending_redis_c = jedis.lrange("sending_array_c", 0, -1);
        int counter_server =0;
        int counter_client =0;
        int j =0;
        for (String flag : flag_redis_c) {
            if (flag.equals("s")) {
                textArea1.append(time_redis_c.get(j)+"["+jedis.get("ip_c")+"]"+"\tMe: "+sending_redis_c.get(counter_server)+"\n");
                counter_server++;
            }
            else if (flag.equals("r")) {
                textArea1.append(time_redis_c.get(j)+"["+jedis.get("ip_c")+"]"+"\tServer: "+reading_redis_c.get(counter_client)+"\n");
                counter_client++;
            }
        }
    }

    public void run(){
        System.out.println("Thread is running...");
        try {
            DataInputStream in = new DataInputStream(
                    new BufferedInputStream(socket.getInputStream()));
            String line = "";
            while (!line.equals("Over"))
            {
                line = in.readUTF();
                log_adding_client(line,"r");
                reading_message_client.setText("Reading Message: "+line);
                chat_setting();

            }
        }
        catch(IOException i)
        {
            log_writer();
        }
    }
    public testing_client() {

        DataOutputStream out;
        jedis = new Jedis("localhost", 6379);
        jedis.set("test3","deneme");
        jedis.get("test3");
        jedis.get("test2");

        try {
            socket = new Socket("127.0.0.1", 5000);
            System.out.println("Connected");
            l.setText("Connection Successful! Please enter the message:");
            DataInputStream input = new DataInputStream(System.in);
            out = new DataOutputStream(
                    socket.getOutputStream());
            InetAddress clientAddress = socket.getLocalAddress();
            jedis.set("ip_c",clientAddress.getHostAddress());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        DataOutputStream finalOut = out;
        button1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String message = textField1.getText();
                try {
                    finalOut.writeUTF(message);
                    log_adding_client(message,"s");
                    l.setText("Sending:\t'"+textField1.getText()+"'");
                    textField1.setText("");
                    chat_setting();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
        button_quit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("Quit button");
                log_writer();
                System.exit(0);
            }
        });
    }
    public static void main(String[] args) {
        Jedis jedis = new Jedis("localhost", 6379);

        // establish a connection
        JFrame frame = new JFrame("Client GUI");
        testing_client t = new testing_client();
        t.start();
        frame.setContentPane(t.panel1);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
        frame.addWindowListener(new WindowAdapter()
        {
            public void windowClosing(WindowEvent e)
            {
                System.out.println("Closed");
                t.log_writer();
                e.getWindow().dispose();
            }
        });
    }
}