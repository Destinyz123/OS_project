import java.io.*;
import java.net.*;
import java.nio.channels.FileChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
public class ZServer {

    static class ClientHandler extends Thread{
        private Socket clientSoc ;
        private SocketChannel clientSocChan ;
        private PrintWriter out ;
        private BufferedReader in ;
        private String serverPath ;

        public ClientHandler(Socket clientSocket)throws IOException {
            this.clientSoc = clientSocket ;
            this.clientSocChan = clientSocket.getChannel();
            this.out = new PrintWriter(clientSocket.getOutputStream(),true);
            this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            this.serverPath = "C:\\Users\\user\\OneDrive\\เดสก์ท็อป\\CS\\OS\\File\\";
        }

        public void sendFile(){
            File files = new File(serverPath);
            if(files.list().length == 0){ // เช็คว่ามีไฟล์อยู่หรือไม่
                out.println(1);
                out.println("No files");
            }
            else{
                out.println(files.list().length); // วนลูป print รายชื่อไฟล์ที่มีอยู่
                for(String name : files.list()){
                    out.println(name);
                }
            }
        }

        public void run(){
            String input ;
            try {
                while (((input = in.readLine()) != null)) { 
                    switch(input){
                        case "1":
                            sendFile();
                            break;
                        case "ZEROCOPY": // ลดการประมวลผลของ cpu
                            String fileName = in.readLine();  // รับชื่อไฟล์มา
                            File fileZ = new File(serverPath + fileName);
                            if (fileZ.exists()) {
                                try(FileChannel fc = new FileInputStream(serverPath + fileName).getChannel()){ // ลด overhead ในการคัดลอกข้อมูล ถ่ายโอนข้อมูลไม่ผ่านหน่วยความจำ
                                    out.println(fc.size());
                                    long transfer = 0 ;
                                    while (transfer < fc.size()) { // วนลูปส่งข้อมูลไป client
                                        long count = fc.transferTo(transfer,fc.size() - transfer,clientSocChan);// para 1 ระบุตำแหน่งเริ่ม 2 ระบุจำนวนไบต์ที่จะส่ง 3 client ที่จะส่ง
                                        transfer += count; // อัพเดทค่า
                                    }
                                } 
                                clientSoc.close(); // ตัดการเชื่อมต่อ
                            }  
                            break;
                        case "ORIGINAL":
                            try {
                                String fileName1 = in.readLine(); 
                                RandomAccessFile raf = new RandomAccessFile(new File(serverPath + fileName1),"r"); // เข้าถึงไฟล์ในโหมดอ่าน
                                out.println(raf.length());
                                byte[] buffer = new byte[4*1024] ; // กำหนดขนาด buffer
                                int bytesRead ;
                                raf.seek(0); // กำหนดจุดเริ่มต้น
                                OutputStream os = clientSoc.getOutputStream();
                                long start = 0 ;
                                long end = raf.length(); // กำหนดจุดสิ้นสุด
                                while (start < end && (bytesRead = raf.read(buffer)) != -1) { // อ่านข้อมูลลงมาใน buffer  
                                    os.write(buffer); // ส่งข้อมูลจาก buffer ไปให้ client
                                    start += bytesRead ; // กำหนดจุดเริ่มถัดไป
                                }
                                raf.close();
                                os.close();

                            } catch (Exception e) {
                                e.printStackTrace();
                            }  
                            break;  
                        default:
                            System.out.println("Wrong input");
                            break ;    
                    }
                }
            } catch (Exception e) {
                System.out.println("Client Disconnect");
            }
        }
    }

    public static void main(String[] args) {
        final int port = 45678 ;
        System.out.println("Start Server");
        try (ServerSocketChannel serverSC = ServerSocketChannel.open();) { // SocketChannel เหมาะใช้งานกับ Zero copy ช่วยลดการรอคอยไม่หยุดตอนมีการเรียก method หรือ อ่านข้อมูล
            ServerSocket serverS = serverSC.socket();
            InetSocketAddress socketAddress = new InetSocketAddress(port); // ระบุport ที่ Socket จะเชื่อม
            serverS.bind(socketAddress); // ผูก Server ไว้กับport 
            System.out.println("Listening to port " + port);
            while (!serverS.isClosed()) { 
                Socket clientSoc = serverS.accept();
                System.out.println("New client connected "+clientSoc);
                ClientHandler handler = new ClientHandler(clientSoc); // เมื่อมี Client เข้ามาแตกเธรดแยกไปรองรับการทำงานของ Client
                handler.start();
            }
        } catch (Exception e) {
            System.err.println("Server error");
        }
    }
}
