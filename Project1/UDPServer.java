import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.file.*;

class UDPServer {
    public static void main(String[] args) throws Exception {
  
        final int PORT_NUMBER = 8080;
        final int PACKET_SIZE = 1024;
        
        DatagramSocket serverSocket = new DatagramSocket(PORT_NUMBER);
        byte[] receiveData = new byte[PACKET_SIZE];
        String nullByte = "\0";
        String errorCode = "";

        while(true) {
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            serverSocket.receive(receivePacket);

            InetAddress IPAddress = receivePacket.getAddress();
            int port = receivePacket.getPort();

            String sentence = new String(receivePacket.getData());
            System.out.println("Data received is: " + sentence);

            String[] arr = sentence.split(" ");
            String fileNameRequested = arr[1];

            try {
            BufferedReader newFile = new BufferedReader(new FileReader(fileNameRequested));
            StringBuilder fileData = new StringBuilder();
            String temp = newFile.readLine();
            System.out.println("line: " + temp);
            
            while (temp != null) {
                System.out.println(temp);
                fileData.append(temp);
                temp = newFile.readLine();
            }
             newFile.close();  
        } 
            catch(FileNotFoundException e) { 
                System.err.println("File not found.");
                errorCode = "404 - File Not Found";
            }
         
            if (errorCode.isEmpty()) {errorCode = "200 - Document follows";}

            byte[] bytes = null;
            try {
                bytes = Files.readAllBytes(Paths.get(fileNameRequested));
            } catch(NoSuchFileException e) {
                bytes = Files.readAllBytes(Paths.get("Error.html"));
            }
            
            String HTTPHeader = "\n" + arr[2] + " " + errorCode + "\r\nContent-Type: text/plain\r\nContent-Length: " + bytes.length + " bytes\r\n";
           
            byte[] headerPacket = HTTPHeader.getBytes();
            DatagramPacket sendedPacket = new DatagramPacket(headerPacket, headerPacket.length, IPAddress, port);
            serverSocket.send(sendedPacket); 

            boolean complete = false;
            int start = 0;
            int end = 1024;
            if (end > headerPacket.length) { end = headerPacket.length; }

            while (complete == false) {
                byte[] temp = new byte[1024];
                int dataIndex = 0;
                for(int i = start; i < end; i++) {
                    temp[dataIndex] = headerPacket[i];
                    dataIndex++;
                    if(i == headerPacket.length - 1) { complete = true; }
                }
                start = end;
                if (end + 1024 < headerPacket.length) {
                    end+= 1024;
                } else {
                    end = headerPacket.length;
                }

            DatagramPacket sendPacket = new DatagramPacket(temp, temp.length, IPAddress, port);
            serverSocket.send(sendPacket); 
            }

            complete = false;
            start = 0;
            end = 1024;
            if (end > bytes.length) {
                end = bytes.length;
            }

            while (complete == false) {
                byte[] data = new byte[1024];
                int dataIndex = 0;
                for(int i = start; i < end; i++) {
                
                    data[dataIndex] = bytes[i];
                    dataIndex++;
                    if(i == bytes.length - 1) {
                        complete = true;
                    }
                }
                start = end;
                if (end + 1024 < bytes.length) {
                    end+= 1024;
                } 
                else { end = bytes.length; }

            DatagramPacket sendPacket = new DatagramPacket(data, data.length, IPAddress, port);
            serverSocket.send(sendPacket); 
            }

            byte[] nullBytePacket = nullByte.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(nullBytePacket, nullBytePacket.length, IPAddress, port);
            serverSocket.send(sendPacket); 
            System.exit(0); 
        }
    }
}