import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

import redis.clients.jedis.*;

// redis log name --> hgetall log_server          / hkeys log_server        / hget log_server message_3
// redis time list -> lindex time_array_s +index  / lrange time_array_s 0 -1


public class testing_server extends Thread {
    private JLabel label_s;
    private JLabel label_s2;
    private JPanel panel_s;
    private JLabel message_label;
    private JButton button1;
    private JTextField input_text_server;
    private JButton button_quit;
    private JTextArea textArea1;
    DataInputStream in;
    DataOutputStream out;
    private Jedis jedis;
    public void redis_log(String s_or_c,int counter){
        List<String> time_redis_s = jedis.lrange("time_array_s", 0, -1);
        List<String> message_redis_s = jedis.lrange("messages_array_s", 0, -1);
        String log_server = "log_server";
        if (Objects.equals(s_or_c, "s")){
            String message_field = "Message "+(counter+1)+" sent from Server";
            String message_value = message_redis_s.get(counter);
            jedis.hset(log_server, message_field, message_value);

            String time_field_s = "Time "+(counter+1);
            String value = time_redis_s.get(counter);
            jedis.hset(log_server, time_field_s, value);

        }
        else if(Objects.equals(s_or_c, "r")){
            String message_field_s = "Message "+(counter+1)+" sent from Client";
            String message_value_s= message_redis_s.get(counter);
            jedis.hset(log_server,message_field_s,message_value_s);

            String time_field_s = "Time "+(counter+1);
            String time_value = time_redis_s.get(counter);
            jedis.hset(log_server,time_field_s,time_value);
        }
    }
    public void log_adding_server(String message,String flag_value){

        LocalDateTime date_time = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String time_now = date_time.format(formatter);
        jedis.rpush("time_array_s", time_now);
        jedis.rpush("flag_array_s", flag_value);
        jedis.rpush("messages_array_s", message);
    }
    public void log_writter_server(){
        try {
            FileWriter myWriter = new FileWriter("/home/vely/IdeaProjects/in_memory_system/src/main/java/server_redis_log.txt");

            List<String> time_redis_s = jedis.lrange("time_array_s", 0, -1);
            List<String> flag_redis_s = jedis.lrange("flag_array_s", 0, -1);
            List<String> message_redis_s = jedis.lrange("messages_array_s", 0, -1);
            int flag_counter=0;

            for (String flag : flag_redis_s) {


                if (flag.equals("s")) {
                    myWriter.write(time_redis_s.get(flag_counter)+"["+jedis.get("ip")+"]"+"\tMe: "+ message_redis_s.get(flag_counter)+"\n");
                    redis_log("s",flag_counter);
                }
                else if (flag.equals("r")) {
                    myWriter.write(time_redis_s.get(flag_counter)+"["+jedis.get("ip")+"]"+"\tClient: "+ message_redis_s.get(flag_counter)+"\n");
                    redis_log("r",flag_counter);

                }
                flag_counter++;
            }
            /*for (int i = 0; i < flag_array.size(); i++) {
                String flag = flag_array.get(i);
                if (flag.equals("s")) {
                    myWriter.write(time_array.get(i)+"["+ip+"]"+"\tMe: "+ messages_array.get(i)+"\n");
                }
                else if (flag.equals("r")) {
                    myWriter.write(time_array.get(i)+"["+ip+"]"+"\tClient: "+ messages_array.get(i)+"\n");
                }
            }*/

            myWriter.close();
            System.out.println("Dosya başarıyla oluşturuldu ve yazıldı.");
        } catch (IOException z) {
            System.out.println("Dosya yazdırılırken Bir hata oluştu.");
            z.printStackTrace();
        }
    }
    public void set_message_label(String message_label) {

        this.label_s.setText("Reading Message: "+message_label);
    }
    public void set_label_s (String label_s){
        this.message_label.setText("Sending Message: "+label_s);
    }
    public void chat_setting_server(){
        List<String> time_redis_list = jedis.lrange("time_array_s", 0, -1);
        List<String> flag_redis_list = jedis.lrange("flag_array_s", 0, -1);
        List<String> message_redis_list = jedis.lrange("messages_array_s", 0, -1);
        textArea1.setText("");
        int j =0;
        for (String flag : flag_redis_list) {

            if (flag.equals("s")) {
                textArea1.append(time_redis_list.get(j)+"["+jedis.get("ip")+"]"+"\tMe: "+ message_redis_list.get(j)+"\n");

            }
            else if (flag.equals("r")) {
                textArea1.append(time_redis_list.get(j)+"["+jedis.get("ip")+"]"+"\tClient: "+ message_redis_list.get(j)+"\n");
            }
            j ++;
        }
    }
    public void run() {

        String line = "";

        while (!line.equals("Over")) {
            try {
                line = in.readUTF();
                log_adding_server(line,"r");
                set_message_label(line);
                chat_setting_server();
            }
            catch (IOException i) {
                System.out.println(i + "\n\nServer closed...");
                    log_writter_server();
                    System.exit(0);
                    }
                }
    }
    public testing_server() {
        JFrame frame = new JFrame("Server GUI");
        jedis = new Jedis("localhost", 6379);
        frame.add(panel_s);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
        //Pencere kapandı ve log kaydedilecek
        frame.addWindowListener(new WindowAdapter()
        {
            public void windowClosing(WindowEvent e)
            {
                System.out.println("Closed");
                log_writter_server();
                e.getWindow().dispose();
            }
        });
        try {
            Socket socket;
            try (ServerSocket server = new ServerSocket(5000)) {
                System.out.println("Server started");
                System.out.println("Waiting for a client ...");
                socket = server.accept();
            }
            System.out.println("Client accepted");
            in = new DataInputStream(
                    new BufferedInputStream(socket.getInputStream()));

            out = new DataOutputStream(
                    socket.getOutputStream());
            InetAddress clientAddress = socket.getInetAddress();
            jedis.set("ip",clientAddress.getHostAddress());
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        button1.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                DataOutputStream finalOut = out;
                String sending_message = input_text_server.getText();
                input_text_server.setText("");
                try {
                    finalOut.writeUTF(sending_message);
                    log_adding_server(sending_message,"s");
                    set_label_s(sending_message);
                    chat_setting_server();

                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
        button_quit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("Quit button");
                log_writter_server();
                System.exit(0);
            }
        });
    }
    public static void main(String[] args) {
        Jedis jedis = new Jedis("localhost", 6379);
        jedis.flushAll();
        testing_server thread = new testing_server();
        thread.start();
    }
}