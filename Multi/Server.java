import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    static class ClientHandler extends Thread {
        private final Socket clientSoc;
        private PrintWriter out;
        private BufferedReader in;
        private String path;

        public ClientHandler(Socket clientSoc) throws IOException {
            this.clientSoc = clientSoc;
            this.out = new PrintWriter(clientSoc.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(clientSoc.getInputStream()));
            this.path = "C:\\Users\\user\\OneDrive\\เดสก์ท็อป\\CS\\OS\\File\\"; 
        }

        public void sendList() { 
            File file = new File(path);
            String[] fileList = file.list();
            if (fileList == null || fileList.length == 0) {
                out.println("List file: No file");
            } else {
                out.println(fileList.length);
                for (String fileName : fileList) {
                    out.println(fileName);
                }
            }
        }

        public void run() {
            String cInput;
            try {
                while ((cInput = in.readLine()) != null) { 
                    switch (cInput) {
                        case "1":
                            sendList(); 
                            break;
                        case "2":
                            String fname = in.readLine();    
                            File file = new File(path + fname); 
                            if (file.exists()) {   
                                out.println(file.length());
                            } else {
                                out.println(-1);
                            }
                            break;
                        case "DOWNLOAD":
                            String filed = in.readLine(); 
                            long start = Long.parseLong(in.readLine());
                            long end = Long.parseLong(in.readLine());

                            try (RandomAccessFile ac = new RandomAccessFile(new File(path + filed), "r");  
                                 OutputStream os = clientSoc.getOutputStream()) { 

                                byte[] buff = new byte[1024]; 
                                int bytesRead; 
                                ac.seek(start); 

                                while (start < end && (bytesRead = ac.read(buff)) != -1) { 
                                    os.write(buff, 0, bytesRead);  
                                    start += bytesRead;
                                }
                                os.flush(); 
                            } catch (IOException e) {
                                System.err.println("File transfer error: " + e.getMessage());
                            }
                            break;
                        default:
                            System.out.println("Wrong command");
                            out.println("Invalid command");
                            break;
                    }
                }
            } catch (IOException e) {
                System.out.println("Client disconnected");
            } finally {
                try {
                    clientSoc.close();
                } catch (IOException e) {
                    System.err.println("Error closing client socket: " + e.getMessage());
                }
            }
        }
    }

    public static void main(String[] args) {
        final int port = 45678;
        System.out.println("Starting server");

        try (ServerSocket servSoc = new ServerSocket(port)) {
            System.out.println("Listening on port: " + port);

            while (!servSoc.isClosed()) {
                Socket clientSoc = servSoc.accept();  
                System.out.println("New Client: " + clientSoc);  
                new ClientHandler(clientSoc).start(); 
            }
        } catch (Exception e) {
            System.out.println("Server error: " + e.getMessage());
        }
    }
}
