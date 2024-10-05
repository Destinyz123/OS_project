
import java.io.*;
import java.net.*;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.util.Scanner;

public class ZClient {

    public static void printChoice(){ // print choice ให้เลือก
        System.out.println("1 : All file list");
        System.out.println("2 : ZERO COPY ");
        System.out.println("3 : ORIGINAL");
        System.out.println("4 : Exit");
        System.out.println();
    }

    public static class Downloader extends Thread{
        private long start ;
        private long end ;
        private SocketChannel client ;
        private Socket clientI ;
        private String fileName ;
        private String choice ;

        public Downloader(SocketChannel client , String fileName , String choice)throws IOException{ // constructor กำหนดค่าตัวแปรต่างๆ
            this.client = client;
            this.clientI = client.socket();
            this.fileName = fileName;
            this.choice = choice;
        }

        public static String getUniqueFileName(String fileName) {
            File file = new File(".//" + fileName);
            String newFileName = fileName;
            int count = 1;
            
            
            while (file.exists()) {
                int dotIndex = fileName.lastIndexOf('.');
                if (dotIndex != -1) {
                    
                    String name = fileName.substring(0, dotIndex);
                    String extension = fileName.substring(dotIndex);
                    newFileName = name + "(" + count + ")" + extension;
                } else {
                    
                    newFileName = fileName + "(" + count + ")";
                }
                file = new File(".//" + newFileName);
                count++;
            }
            return newFileName;
        }

        public void run(){
            try {
                PrintWriter out = new PrintWriter(clientI.getOutputStream(),true);
                BufferedReader in = new BufferedReader(new InputStreamReader(clientI.getInputStream()));
                String uniqueFileName = getUniqueFileName(fileName);
                FileOutputStream fos = new FileOutputStream(".//"+uniqueFileName); // ใช้สร้างไฟล์ใหม่ใน directory ปัจจุบันและเขียนข้อมูลลงไป
                switch(choice){
                    case "ORIGINAL":
                        out.println("ORIGINAL"); // ส่งคำสั่งไป Server
                        out.println(fileName); // ส่งชื่อไฟล์ไป Server
                        long ends = Long.parseLong(in.readLine()); // รับจุดสิ้นสุดของไฟล์มา
                        byte[] buffer = new byte[4*1024] ; // สร้าง buffer
                        start = System.currentTimeMillis(); // บันทึกเวลาตอนเริ่ม
                        RandomAccessFile raf = new RandomAccessFile(".//"+uniqueFileName,"rw"); // เข้าถึงไฟล์ในโหมดอ่านเขียน
                        int bytesRead;
                        InputStream in2 = clientI.getInputStream();
                        long starts = 0 ; // กำหนดจุดเริ่ม
                        while(starts < ends && (bytesRead = in2.read(buffer)) != -1){ // รับข้อมูลจาก Server ลงมาใน buffer
                            raf.seek(starts); // หาจุดเริ่มต้น
                            raf.write(buffer); // เขียนข้อมูลจาก buffer ลงในเครื่องของตัว Client
                            starts += bytesRead; // หาจุดถัดไป

                        }
                        end = System.currentTimeMillis(); // บันทึกเวลาตอนจบ
                        raf.close();
                        clientI.close(); // ตัดการเชื่อมต่อ
                        System.out.println("ORIGINAL TIME : "+(end-start)); // เวลาที่ใช้ทั้งหมด
                        break;

                    case"ZEROCOPY":
                        out.println("ZEROCOPY"); // ส่งคำสั่งไป Server
                        out.println(fileName); // ส่งชื่อไฟล์ไป
                        long fileSize = Long.parseLong(in.readLine()); //รับขนาดไฟล์มา
                        FileChannel fileChan = fos.getChannel(); // ใช้เข้าถึงไฟล์อ่านเขียนโดยตรง ไม่ต้องแปลงเป็น byte
                        long transfer = 0 ; // จุดเริ่ม
                        start = System.currentTimeMillis(); // บันทุึกเวลาเริ่ม
                        while (transfer < fileSize) { // วนลูปเขียนข้อมูลลงในเครื่อง 
                            long count = fileChan.transferFrom(client, transfer, fileSize-transfer); // ตัวที่จะอ่านข้อมูล , ตำแหน่งเริ่ม , จำนวนทีต้องการอ่าน ทำการถ่ายโอนข้อมูลโดยตรงผ่าน OS ไม่ต้องผ่าน buffer
                            transfer += count; // อัพเดทค่า
                        }
                        end = System.currentTimeMillis(); // บันทึกเวลาจบ

                        System.out.println("ZERO COPY TIME : "+(end-start)); // เวลาที่ใช้ทั้งหมด
                        
                        fileChan.close();
                        fos.close();
                        
                        break;
                    default:
                        break;    
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {          
    try {
        String serverIP = "127.0.0.1"; // กำหนด IP เชื่อมกับ Server
        System.out.println("Connect Server...");
        InetSocketAddress address = new InetSocketAddress(serverIP,45678); 
        SocketChannel skChannel = SocketChannel.open(address); // เชื่อมไป Server
        Socket clientSock = skChannel.socket(); 
        System.out.println("Connect Success");

        Scanner sc = new Scanner(System.in);
        PrintWriter out = new PrintWriter(clientSock.getOutputStream(),true);
        BufferedReader in = new BufferedReader(new InputStreamReader(clientSock.getInputStream()));
        String line;
        while (!clientSock.isClosed()){
            printChoice();
            line = sc.nextLine();
            switch(line){
                case"4":
                    System.out.println("Good Bye!");
                    sc.close(); // ตัดการเชื่อมต่อกับ Server
                    clientSock.close();
                    break;
                case"1":
                    out.println(line);// ส่งคำสั่งไป Server
                    int size = Integer.parseInt(in.readLine());
                    System.out.println("File List : ");
                    for (int i = 0 ; i < size ; i++) {
                        System.out.println(in.readLine()); // วนลูป print รายชื่อไฟล์ออกมา
                    }
                    break;
                case"2":
                    try {
                        System.out.println("Zero Copy");
                        System.out.println("Please input file name : ");
                        String filename = sc.nextLine();
                        System.out.println("Waiting ...");
                        System.out.println();

                        InetSocketAddress addressD = new InetSocketAddress(serverIP,45678);
                        SocketChannel skChannelD = SocketChannel.open(address); // สร้างการเชื่อมต่อไป Server
                        Downloader d = new Downloader(skChannelD , filename , "ZEROCOPY"); // ส่งข้อมูลไปเริ่มทำการดาวโหลดที่คลาส Download
                        d.start();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }        
                    break;
                case"3":
                    try{
                        System.out.println("ORIGINAL");
                        System.out.println("Please input file name : ");
                        String filename = sc.nextLine();
                        System.out.println("Waiting ...");
                        System.out.println();

                        InetSocketAddress addressD = new InetSocketAddress(serverIP,45678);
                        SocketChannel skChannelD = SocketChannel.open(address); // สร้างการเชื่อมต่อไปยัง server
                        Downloader d = new Downloader(skChannelD , filename , "ORIGINAL"); // ส่งข้อมูลไปคลาส Download เพื่อจัดการการดาวโหลด
                        d.start();
                    }
                    catch (Exception e){
                        e.printStackTrace();
                    }    
                    break;
                default:
                    System.out.println("Wrong Input");
                    break;    
            }
        }
    }
    catch(Exception e){
        e.printStackTrace();
        System.err.println("Connect failed");
    }
    }
}
