import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.Math;

/*
 * Author: Shanti Upadhyay (spu0004@auburn.edu)
 * COMP 4320 Project 2
 * Date: July 31, 2022
 * Note: This is an updated version of my Project 1.
 *       Added code to adhere to the guidelines stated in the rubric for Project 2. 
 */

class UDPClient {
    public static void main(String[] args) throws Exception {
    
        final int PORT_NUM = 8080;
        final int PACKET_SIZE = 1024;
       
        Scanner scan = new Scanner(System.in);
        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
        InetAddress IPAddress = null;
        DatagramSocket clientSocket = new DatagramSocket();
        Stack <Integer> sequence = new Stack<>();
        byte[] sendData = new byte[PACKET_SIZE];
        byte [] receiveData = new byte[PACKET_SIZE];
        String request = "";
        String[] checkup;
        String modifiedSentence = "";
        int seqNum = 0;
        int corruptedCheckSum = 0;
        double probDamage;
        double probLoss;

        System.out.println("Enter server IP Address to connect to.");
        String serverIp = inFromUser.readLine();
        try { IPAddress = InetAddress.getByName(serverIp); } 
        catch (UnknownHostException e) {
            System.out.println("Invalid host name. Please try again with a valid IP Address.");
        }

        do {
            System.out.println("Enter your packet damage probability: ");
            probDamage = Double.parseDouble(scan.nextLine());
            System.out.println("Enter your packet loss probability: ");
            probLoss = Double.parseDouble(scan.nextLine());
            if ((probDamage + probLoss >= 1.0) || (probDamage + probLoss <= 0.0)) {
                System.out.println("Invalid probabilities. Total must be between 0 & 1");
            }
            else {break;}
         }  
         
        while(true);
        do {
        System.out.println("Type HTTP GET Request, then press Enter:");
        request = inFromUser.readLine();
        checkup = request.split(" ");
        } 
        while (checkup.length != 3);

        sendData = request.getBytes();

        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, PORT_NUM);
        clientSocket.send(sendPacket); 

        int count = 0;
        boolean complete = false;
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        ArrayList<Integer> damagedPackets = new ArrayList<>();
        ArrayList<Integer> lostPackets = new ArrayList<>();

        while(complete == false) {
        clientSocket.receive(receivePacket);
        if (receivePacket.getData()[0] == (byte) '\u0000')
            break;

        if (count != 0 && count <= 2) {
         byte[] temp = Arrays.copyOf(receivePacket.getData(), 1024);
         modifiedSentence = new String(temp);
         System.out.print(modifiedSentence);
        }

        else if (count > 2) {
            seqNum = (int)receivePacket.getData()[PACKET_SIZE];
            sequence.push(seqNum);
            
            byte[] temp = Arrays.copyOf(receivePacket.getData(), 1024);
            modifiedSentence = new String(temp);
            System.out.print(modifiedSentence);

            byte[] uncorrupted = receivePacket.getData();
            int uncorruptedChecksum = checkSum(uncorrupted);

            byte[] corrupted = Gremlin(probDamage, probLoss, uncorrupted);
            if(corrupted != null) {corruptedCheckSum = checkSum(corrupted);}

            if (corrupted == null) {
                lostPackets.add(seqNum);
                String nak = "NAK " + seqNum;
                sendData = nak.getBytes();
                DatagramPacket nakPacket = new DatagramPacket(sendData, sendData.length, IPAddress, PORT_NUM);
                clientSocket.send(nakPacket);
            }
            
            else if(corruptedCheckSum != uncorruptedChecksum) {
                damagedPackets.add(seqNum);
                String nak = "NAK " + seqNum;
                sendData = nak.getBytes();
                DatagramPacket nakPacket = new DatagramPacket(sendData, sendData.length, IPAddress, PORT_NUM);
                clientSocket.send(nakPacket);
            } 
            
            else {
                String ack = "ACK " + seqNum;
                sendData = ack.getBytes();
                DatagramPacket ackPacket = new DatagramPacket(sendData, sendData.length, IPAddress, PORT_NUM);
                clientSocket.send(ackPacket);
            }
        }
        count++;
    } 
        if (damagedPackets.isEmpty()) {System.out.println("No packets have errors");} 
        else {
            System.out.println("There are errors in the following packets: \n" + damagedPackets);
        }

        if (lostPackets.isEmpty()) {System.out.println("No packets have errors");} 
        else {System.out.println("The following packets were lost: \n" + lostPackets);}
        System.out.println("Sequence numbers received: " + sequence); 
        clientSocket.close();
    }

    public static byte[] Gremlin(double loss, double p, byte[] packet) {
      
        final int PACKET_SIZE = 1024;
        
        if (Math.random() < p) {
            double x = Math.random();
            if (x <= 0.5) {
                Random rand = new Random();
                int z = rand.nextInt(PACKET_SIZE);
                packet[z] /= 2;
            }
            else if (x <= 0.8) {
                Random rand = new Random();
                int z = rand.nextInt(PACKET_SIZE);
                int y = rand.nextInt(PACKET_SIZE);
                if (x == y) {y = rand.nextInt(PACKET_SIZE);}
                packet[z] /= 2;
                packet[y] /= 2;
            }
            else {
                Random rand = new Random();
                int z = rand.nextInt(PACKET_SIZE);
                int y = rand.nextInt(PACKET_SIZE);
                int q = rand.nextInt(PACKET_SIZE);
                if (x == y || x == q || y == q) {
                    y = rand.nextInt(PACKET_SIZE);
                    q = rand.nextInt(PACKET_SIZE);
                }
                packet[z] /= 2;
                packet[y] /= 2;
                packet[q] /= 2;
            }
        }
       
        else if ((Math.random()) < (p + loss)) {packet = null;}
        return packet;
    }

    public static int checkSum(byte[] data) {
        int sum = 0;
        for (byte b: data) {
         sum += b;
        }
        return sum;
    }
}