import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.util.Scanner;
public class Client {

    public static class Downloader extends Thread{
        private long start ;
        private long end ;
        private Socket client ;
        private String fileN ;

        public Downloader(Socket client , long start , long end , String fileN) throws IOException{
            this.client = client;
            this.start = start;
            this.end = end;
            this.fileN = fileN;
        }

        public void run(){
            try{
                PrintWriter out = new PrintWriter(client.getOutputStream(), true);
                out.println("DOWNLOAD"); 
                out.println(fileN);
                out.println(start);
                out.println(end);

                byte[] buff = new byte[1024]; 
                RandomAccessFile ac = new RandomAccessFile(".//" + fileN, "rw"); 
                int bytesRead ; 

                InputStream in = client.getInputStream(); 

                while (start < end && (bytesRead = in.read(buff)) != -1) { 
                    ac.seek(start);
                    ac.write(buff);
                    start += bytesRead;
                }
                ac.close(); 
                client.close(); 
            }
            catch(IOException e){
                e.printStackTrace();
            }
        }
    }

    public static void printSelect(){
        System.out.println("Choice 1 : See List Files");
        System.out.println("Choice 2 : Choose File Download");
        System.out.println("Exit");
    }
    public static void main(String[] args) {
        try {
            String IP = "127.0.0.1"; 
            System.out.println("Connect Server");
            Socket clientSoc = new Socket(IP , 45678); 
            System.out.println("Connect Success");

            Scanner sc = new Scanner(System.in);
            PrintWriter out = new PrintWriter(clientSoc.getOutputStream() , true); 
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSoc.getInputStream())); 

            String line ;
            while(!clientSoc.isClosed()) {
                printSelect();
                line = sc.nextLine();
                switch (line){
                    case "Exit":
                        System.out.println("Good luck");
                        clientSoc.close(); 
                        break ;
                    case "1":
                        out.println(line);
                        int size = Integer.parseInt(in.readLine()); 
                        for (int i = 0 ; i < size; i++){
                            System.out.println(in.readLine());
                        }
                        break ;
                    case "2":
                        out.println(line);
                        System.out.println("Input File name");
                        String fileN = sc.nextLine(); 
                        out.println(fileN); 
                        
                        long fsize = Long.parseLong(in.readLine());  
                        int threadNumbs = 10 ; 
                        if (fsize != -1){
                            for(int i = 0 ; i < threadNumbs ; i++){ 
                                long start = (i*fsize) / threadNumbs ; 
                                long end = ((i+1)*fsize) / threadNumbs ; 
                                Socket clientSoc2 = new Socket(IP , 45678); 
                                new Downloader(clientSoc2 , start , end , fileN).start();
                            }
                        } else{
                            System.out.println("Invalid");
                        }
                        break;
                    default:
                    System.out.println("Wrong");
                    break ;
                }
            }
        } catch (Exception e) {
            System.out.println("Connect fail");
        }
    }
}
