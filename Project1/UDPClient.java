import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.Math;

/*
 * Author: Shanti Upadhyay (spu0004@auburn.edu)
 * COMP 4320 Project 1
 * Date: July 23, 2022
 */

class UDPClient {
    public static void main(String[] args) throws Exception {
    
        Scanner scan = new Scanner(System.in);
        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
        InetAddress IPAddress = null;
        DatagramSocket clientSocket = new DatagramSocket();
        byte[] sendData = new byte[1024];
        byte [] receiveData = new byte[1024];
        String request;
        String[] checkup;

        System.out.println("Enter server IP Adress to connect to.");
        String serverIp = inFromUser.readLine();
        try { IPAddress = InetAddress.getByName(serverIp); } 
        catch (UnknownHostException e) {
            System.out.println("Invalid host name. Please try again with a valid IP Address.");
        }

        System.out.println("Enter the packet damage probability: ");
        double gremlinProb = Double.parseDouble(scan.nextLine());

        do {
        System.out.println("Type HTTP GET Request, then press Enter:");
        request = inFromUser.readLine();
        checkup = request.split(" ");
        } 
        while (checkup.length != 3);

        sendData = request.getBytes();
        
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 8080);
        clientSocket.send(sendPacket); 

        //Read the datagram from the server until nullbyte packet is received
        boolean complete =false;
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        int count = 0;
        ArrayList<Integer> damagedPackets = new ArrayList<>();

        while(complete == false) {
        
         clientSocket.receive(receivePacket);
        if (receivePacket.getData()[0] == (byte) '\u0000')
            break;
        
        String modifiedSentence = "";
        if (count!=0) {
           modifiedSentence = new String(receivePacket.getData());
           System.out.print(modifiedSentence);
        }

        if (count > 1) {
            byte[] uncorrupted = receivePacket.getData();
            int uncorruptedCheckSum = checkSum(uncorrupted);
            byte[] corrupted = Gremlin(gremlinProb, uncorrupted);
            int corruptedCheckSum = checkSum(corrupted);

            if(corruptedCheckSum != uncorruptedCheckSum) {
                int num = count - 2;
                damagedPackets.add(num);
            }
        }
        count++;
    } 
        if (damagedPackets.isEmpty()) { System.out.println("No packets have errors"); } 
        else {
            System.out.println("There are errors in the following packets: \n" + damagedPackets);
        }
        clientSocket.close();
    }

    public static byte[] Gremlin(double p, byte[] pack) {
    
        if (Math.random() < p) {
            double x = Math.random();
            if (x <= 0.5) {
                Random rand = new Random();
                int z = rand.nextInt(1024);
                pack[z] /= 2;
            }

            else if (x <= 0.8) {
                Random rand = new Random();
                int z = rand.nextInt(1024);
                int y = rand.nextInt(1024);
                if (x == y) { y = rand.nextInt(1024); }
                pack[z] /= 2;
                pack[y] /= 2;
            }

            else {
                Random rand = new Random();
                int z = rand.nextInt(1024);
                int y = rand.nextInt(1024);
                int q = rand.nextInt(1024);
                if (x == y || x == q || y == q) {
                    y = rand.nextInt(1024);
                    q = rand.nextInt(1024);
                }
                pack[z] /= 2;
                pack[y] /= 2;
                pack[q] /= 2;
            }
        }
        return pack;
    }

    //Calculates and returns the value of checksum for the packet
    public static int checkSum(byte[] data) {
        int sum = 0; 
        for (byte b: data) {
            sum += b;
        }
        return sum;
    }
}