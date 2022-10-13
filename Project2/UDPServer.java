import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.file.*;

class UDPServer {
    public static void main(String[] args) throws Exception {
    
        final int PORT_NUM = 8080;
        final int PACKET_SIZE = 1024;
        int startIndex = 0;
        int endIndex = 7;
        int seqNum = 0;
        boolean complete = false;
        DatagramSocket server = new DatagramSocket(PORT_NUM);
        byte[] receiveData = new byte[1024];
        String nullByte = "\0";
        String errorCode = "";
        List <byte[]> packets = new ArrayList<byte[]>();

        while(true) {
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            server.receive(receivePacket);
            InetAddress IPAddress = receivePacket.getAddress();
            int port = receivePacket.getPort();

            String sentence = new String(receivePacket.getData());
            System.out.println("Data received is: " + sentence);

            String[] splitArr = sentence.split(" ");
            String fileNameRequested = splitArr[1];

            try {
            BufferedReader importFile = new BufferedReader(new FileReader(fileNameRequested));
            StringBuilder content = new StringBuilder();
            String x = importFile.readLine();
            System.out.println("Line Number: " + x);
            while (x != null) {
                System.out.println(x);
                content.append(x);
                x = importFile.readLine();
            }
             importFile.close();
        } 
            catch(FileNotFoundException e) {  
                System.err.println("File does not exist. ");
                errorCode = "Error 404 - File Not Found";
            }
            
            byte[] bytes = null;
            try {bytes = Files.readAllBytes(Paths.get(fileNameRequested));} 
            catch(NoSuchFileException e) {bytes = Files.readAllBytes(Paths.get("Error.html"));}
 
            String HTTPHeader = "\n" + splitArr[2] + " " + errorCode + "\r\nContent-Type: text/plain\r\nContent-Length: " + bytes.length + " bytes\r\n";
           
            byte[] headerPack = HTTPHeader.getBytes();
            DatagramPacket packetSent = new DatagramPacket(headerPack, headerPack.length, IPAddress, port);
            server.send(packetSent); 

            int start = 0;
            int end = PACKET_SIZE;
            if (end > headerPack.length) {end = headerPack.length;}

            while (complete == false) {
                byte[] t = new byte[PACKET_SIZE];
                int dataIndex = 0;
                for(int i = start; i < end; i++) 
                {
                    t[dataIndex] = headerPack[i];
                    dataIndex++;
                if(i == headerPack.length - 1) {complete = true;}
                }
               start = end;
               if (end + PACKET_SIZE < headerPack.length) {end+= PACKET_SIZE;} 
               else {end = headerPack.length;}
          
            DatagramPacket sendPacket = new DatagramPacket(t, t.length, IPAddress, port);
            server.send(sendPacket); 
            }

            complete = false;
            start = 0;
            end = PACKET_SIZE;
            if (end > bytes.length) {end = bytes.length;}

            while (complete == false) {
                byte[] data = new byte[1025];

                int dataIndex = 0;
                for(int i = start; i < end; i++) {
                
                    data[dataIndex] = bytes[i];
                    dataIndex++;
                    if(i == bytes.length - 1) {complete = true;}
                }
                start = end;
                if (end + PACKET_SIZE < bytes.length) {end+= PACKET_SIZE;}
                else {end = bytes.length;}

                data[PACKET_SIZE] = (byte)seqNum;
                seqNum++;
                System.out.println("Sequence Number = " + data[PACKET_SIZE]);
                packets.add(data);
            }
        
            ArrayList <Integer> packResend = new ArrayList<Integer>();
            boolean processing = true;
        while(processing) {
                byte[] x = packets.get(startIndex);

                DatagramPacket sendPacket = new DatagramPacket(x, x.length, IPAddress, port);
                server.send(sendPacket); 

                DatagramPacket ackOrNakPacket = new DatagramPacket(receiveData, receiveData.length);
                server.receive(ackOrNakPacket);

                String anData = new String(ackOrNakPacket.getData());
                String[] input = anData.split(" ");
                String a = input[1];
                int b = Character.getNumericValue(a.charAt(0));
                String compare = input[0];
                int nIndex = Character.getNumericValue(a.charAt(0));
                
                if(compare.compareTo("NAK") == 0) {
                    packResend.add(nIndex);
                    startIndex++;
                } 
                else {
                    startIndex++;
                    endIndex++;
                }
            if (startIndex == packets.size()) {break;}
        }
        
        for (int anData : packResend) {
            byte[] x = packets.get(anData);
            DatagramPacket sendPacket = new DatagramPacket(x, x.length, IPAddress, port);
            server.send(sendPacket); 
        }
            byte[] nullPack = nullByte.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(nullPack, nullPack.length, IPAddress, port);
            server.send(sendPacket); 
            System.out.println("\nPackets Resent: \n" + packResend);
            System.exit(0);
        }
    }
}