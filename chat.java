package proj3;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class chat implements Runnable {

    private static final int PORT_MIN = 1024; // minimum port number
    private static final int PORT_MAX = 1200; // maximum port number

    // class variables to define any chat connection 
    private final String name;          // name of the user
    private final boolean isServer;     // boolean variable to indicate if the current user is a server or not
    private final int port;             // stores to port number of the current user
    private final int targetPort;       // stores the port number to which the user connects for chat

    // class-level variables to read data from an input stream/write data to an output stream
    static DataInputStream input;
    static DataOutputStream output;

    // Constructor for chat class to initiate the class variables
    public chat(String name, boolean isServer, int port, int targetPort) {
        this.name = name;
        this.isServer = isServer;
        this.port = port;
        this.targetPort = targetPort;
    }

    public static void main(String[] args) {
        
        // Parse command line arguments
        if (args.length != 1 || (args.length == 1 && !(args[0].equals("create") || args[0].equals("join")))) {
            System.out.println("Usage: java chat2 <create/join>");
            return;
        }

        boolean isServer = false;
        if(args[0].equals("create")) isServer = true;

        Scanner sc = new Scanner(System.in);
        System.out.println("Welcome to CN-Chat!");
        System.out.print("Enter your name: ");
        String name = sc.nextLine();
        int port = getRandomPort();
        System.out.println("===================");
        System.out.println(name + " is active on port " + port);
        System.out.print("Enter target port: ");
        int targetPort = sc.nextInt();
        System.out.println("===================");

        // Create and run the chat application
        chat app = new chat(name, isServer, port, targetPort);
        app.run();
    }

    @Override
    public void run() {
        ServerSocket serverSocket = null;
        Socket socket = null;

        try {
            Thread writingThread;
            Thread readingThread;

            if (isServer) {
                // Run as server
                serverSocket = new ServerSocket(port);
                // Wait for incoming connections
                socket = serverSocket.accept();
            } else {
                // Run as client
                socket = new Socket("localhost", targetPort);
            }

            output = new DataOutputStream(socket.getOutputStream());
            input = new DataInputStream(socket.getInputStream());

            // Send the name to the connection
            output.writeUTF(name);
            // Get the name from the connection
            String visitor = input.readUTF();

            if (isServer)
                System.out.println("\n" + visitor + " joined the chat!\n");
            else
                System.out.println("\nConnected to " + visitor +"\n");

            // Start writing and reading threads
            writingThread = new Thread(new WritingThread(socket));
            readingThread = new Thread(new ReadingThread(socket,visitor));
            writingThread.start();
            readingThread.start();

            // Wait for threads to finish
            writingThread.join();
            readingThread.join();

            // Close serversocket and socketsocket
            if(serverSocket!=null)
                serverSocket.close();
            socket.close();
        } catch (IOException | InterruptedException e) {
            System.out.println("Error in initiating the chat because of "+ e.getMessage() + ". \nPlease try again.\n ");
        } catch (Exception e) {
            System.out.println("Error in initiating the chat because of "+ e.getMessage()+ ". \nPlease try again.\n ");
        } finally{
            try{
                if(serverSocket!=null)
                    serverSocket.close();
                if(socket != null)
                    socket.close();
                if(isServer)
                    main(new String[]{"create"});
                else
                    main(new String[]{"join"});
            }catch(Exception e){
                System.out.println("Unable to close connections");   
            }
            
        }
    }

    private static class WritingThread implements Runnable {
        private final Socket socket;

        public WritingThread(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                // write to the remote endpoint of the socket 
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                // to read message from the keyboard 
                BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

                //loop of reading  a message from the keyboard
                while (true) {
                    String line = in.readLine();

                    // Check to handle any abrupt closure of the socket connection
                    if (line == null) { 
                        System.out.println("Connection closed.");
                        break;
                    }

                    // If the message is <transfer filename>, the file is transmitted through the connection
                    // else write the message to the connection (socket)

                    if(line.length() > 0 && line.contains("transfer") && line.split(" ").length == 2 && line.split(" ")[0].equals("transfer")) {
                        out.println(line);
                        transmitFile(line, socket);
                    }else{
                        out.println(line);
                    }     
                }
            } catch (IOException e) {
                System.out.println(e.getMessage()); 
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private static class ReadingThread implements Runnable {
        private final Socket socket;
        private String name;

        public ReadingThread(Socket socket, String name) {
            this.socket = socket;
            this.name = name;
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))){
                // to read text data from the socket's input stream sent by the other end of the connection.

                while (true) {
                    // read messages from the connection socket
                    String line = in.readLine();

                    // Check to handle any abrupt closure of the socket connection
                    if (line == null) { 
                        System.out.println("Connection closed.");
                        break;
                    }

                    // If the message is <transfer filename>, the the other end of the connection is sending file, we will start reading
                    // else read messages from the connection socket and print the messages on the screen

                    if(line.length() > 0 && line.contains("transfer") && line.split(" ").length == 2 && line.split(" ")[0].equals("transfer")) {
                        readFile(line, socket, name);
                    }else
                        System.out.println(name + ": " + line);
                }
            } catch (IOException e) {
                System.out.println( e.getMessage());
            } catch (Exception e) {
                System.out.println( e.getMessage());
            }
        }
    }

    private static int getRandomPort() {
        // Generate a random port number between PORT_MIN and PORT_MAX
        return (int) (Math.random() * (PORT_MAX - PORT_MIN + 1) + PORT_MIN);
    }

    private static void transmitFile(String line, Socket socket){
        try{
            File file = new File(line.split(" ")[1]);

            // check if file exists at server to transfer
            if (file.exists() && !file.isDirectory()) {
                output.writeUTF("upload " + line.split(" ")[1] + " " + file.length());

                //byteArray is an array with a length of 1024, 
                //in each pass, the code reads 1024 bytes from the inputFile and writes to output
                byte[] byteArray = new byte[1024];
                int inputBytes;  
                FileInputStream fis = new FileInputStream(file);
                while ((inputBytes = fis.read(byteArray)) > 0) {
                    output.write(byteArray, 0, inputBytes);
                }
                fis.close();
                System.out.println("Transfer successful!");
            }else {
                output.writeUTF("no_file_exists");
                System.out.println("Could not locate the file");
            }
        }
        catch(Exception e){
            System.out.println("Error while transmitting file because of "+ e.getMessage());
        }
    }

    private static void readFile(String line, Socket socket, String visitorName){
        try{
            String fileName = line.split(" ")[1];
            String message = (String) input.readUTF();
            String[] tokens = message.split(" ");
    
            if(tokens.length==3 && tokens[0].equals("upload")){
                long size = Long.parseLong(message.split(" ")[2]);
                FileOutputStream fos = new FileOutputStream(new File("new"+fileName));

                // byteArray is an array with a length of 1024
                // in each pass, the code reads 1024 bytes or less from the socket input stream and writes to output file stream
                // terminate when all the data has been read from the input stream
                byte[] byteArray = new byte[1024];
                int inputBytes;            
                while (size > 0 && (inputBytes = input.read(byteArray, 0, (int) Math.min(byteArray.length, size))) != -1) {
                    fos.write(byteArray, 0, inputBytes);
                    size -=inputBytes;
                }
                fos.close();
                System.out.println("File received successfully from " + visitorName);
            }else if(tokens[0].equals("no_file_exists")){
                // System.out.println("Could not locate to tranfer");
            }
        }
        catch(Exception e){
            System.out.println("Error while reading file because of "+ e.getMessage());
        }
    }

}