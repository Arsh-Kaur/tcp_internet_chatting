# tcp_internet_chatting

We have created a chat program which creates two threads- reading thread and writing thread. Once the connection is established, the reading thread listens to the connection socket and prints output on the screen. On the other hand, the writing thread takes input from keyboard and writes message into the connection socket. The files can be transferred between them and any messages are printed with the name prefix given at the start.

Instructions:
1)	Unzip the files to local directory
2)	Open two command prompts and go to the local directory where project is unzipped.
3)	Start the server using the following command, create if this is the first time you are running this class to let the program know that a server needs to be created, else join:
java chat.java <create/join>

Connection Establishment:
1. Run the command java chat.java create on one terminal. 
2. It will print the port at which it is running and asks for the name. Enter the name for example Bob.
3. Run the command java chat.java join on another terminal. 
4. It will again print the port for this user and asks for the name. Enter the name for example Alice.
5. Now enter the target port on both the terminals and the connection will be established. \
Start chatting / Transfer File:
6. Once the connection is established, any message sent from one user will appear on the terminal for another user with its name as prefix.
