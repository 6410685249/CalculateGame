/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package calculategame;

/**
 *
 * @author Siriphatson Sringamphon 6410685249
 * @author Chalisa Thummarat 6410685041
 */


 import java.awt.event.*;
 import java.io.*;
 import java.net.*;
 import javax.swing.*;
 
 public class CalculateGame {
     private Socket socket;
     private PrintWriter out;
     private BufferedReader in;
     private JFrame frame;
     private JTextField answerField;
     private JLabel questionLabel;
     private JTextArea scoreArea;
     private JButton submitButton;
     private JButton readyButton;
     private String playerName;
     private final int SERVER_PORT = 8888;
 
     public CalculateGame(String serverAddress) throws IOException {
         socket = new Socket(serverAddress, SERVER_PORT);
         out = new PrintWriter(socket.getOutputStream(), true);
         in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
 
         // Enter player name
         playerName = JOptionPane.showInputDialog("Enter your name:");
         if (playerName == null) {
             frame.dispose();
         }
         out.println(playerName);
 
         // Application frame
         frame = new JFrame("Calculate Game - Player: " + playerName);
         frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
         frame.setSize(400, 300);
         frame.setLayout(null);
 
         questionLabel = new JLabel("Waiting for question...");
         questionLabel.setBounds(50, 50, 300, 20);
         frame.add(questionLabel);
 
         answerField = new JTextField();
         answerField.setBounds(50, 80, 100, 20);
         frame.add(answerField);
         answerField.setEnabled(false);
 
         submitButton = new JButton("Submit");
         submitButton.setBounds(160, 80, 80, 20);
         frame.add(submitButton);
         submitButton.setEnabled(false);
 
         readyButton = new JButton("Ready");
         readyButton.setBounds(50, 110, 80, 20);
         frame.add(readyButton);
 
         scoreArea = new JTextArea();
         scoreArea.setBounds(50, 140, 300, 100);
         scoreArea.setEditable(false);
         frame.add(scoreArea);
 
         readyButton.addActionListener(new ActionListener() {
             @Override
             public void actionPerformed(ActionEvent e) {
                 out.println("ready");
                 readyButton.setEnabled(false);
                 answerField.setEnabled(true);
             }
         });
 
         submitButton.addActionListener(new ActionListener() {
             @Override
             public void actionPerformed(ActionEvent e) {
                 String answer = answerField.getText().trim();
                 if (answer.isEmpty()){
                     JOptionPane.showMessageDialog(frame, "Please enter a valid response.");
                 }
                 else{
                     out.println(answerField.getText());
                     answerField.setText("");
                     submitButton.setEnabled(false);
                 }
             }
         });
 
         new Thread(new ServerListener()).start();
         frame.setVisible(true);
     }
 
     private class ServerListener implements Runnable {
         @Override
         public void run() {
             try {
                 String response;
                 // while ((response = in.readLine()) != null) {
                 while (true) {
                     // submitButton.setEnabled(true);
                     response = in.readLine();
                     System.out.println("response: " + response);
                     System.out.println("check: " + response.startsWith("Question"));
                     if (response.startsWith("Waiting")) {
                         System.out.println("waiting for new round");
                         questionLabel.setText(response);
                         readyButton.setEnabled(true); // Enable submit button for new question
                     }
                     else if (response.startsWith("Question")) {
                         System.out.println("eiei");
                         questionLabel.setText(response);
                         submitButton.setEnabled(true); // Enable submit button for new question
                         System.out.println("eiei2");
                     }
                     else if (response.startsWith("Score Update:")) {
                         System.out.println(response);
                         // scoreArea.setText(response.substring(13));
 
                         scoreArea.setText("");
                         String scores = response.substring(13);
                         String[] lines = scores.split(",");
                         for (String line : lines) {
                             scoreArea.append(line + "\n");
                         }
                     }
                     else if (response.startsWith("Game Over!")) {
                         JOptionPane.showMessageDialog(frame, response);
                         readyButton.setEnabled(false);
                         answerField.setEnabled(false);
                         submitButton.setEnabled(false);
                         // break; // Exit the loop since the game is over
 
                        //  int playAgain = JOptionPane.showConfirmDialog(frame, "Would you like to play again?", "Play Again", JOptionPane.YES_NO_OPTION);
                         // String option = JOptionPane.showInputDialog(frame, "\nWould you like to play again? (yes/no)");
                        //  if (playAgain == JOptionPane.YES_OPTION) {
                        //      questionLabel.setText("Waiting for question...");
                        //      out.println("play again");
                        //      readyButton.setEnabled(true);
                        //  } else {
                             // out.println("Thank you for playing!");
                             JOptionPane.showMessageDialog(frame, "Thank you for playing!", "End Game", JOptionPane.INFORMATION_MESSAGE);
                             frame.dispose();
                             break; // Exit the loop and close the client
                        //  }
                     }
                     else {
                         JOptionPane.showMessageDialog(frame, response);
                     }
                 }
             }
             catch (IOException e) {
                 e.printStackTrace();
             }
             finally {
                 try {
                     socket.close();
                 } catch (IOException e) {
                     e.printStackTrace();
                 }
             }
         }
     }
 
     public static void main(String[] args) {
         JOptionPane.showMessageDialog(null, 
             "Welcome to Calculate Game!\n" +
             "Here are the rules:\n" +
             "- Answer math questions as quickly as possible.\n" +
             "- Compete with others, the one who got 10 first is the winner.\n" +
             "- Please enter the server IP to connect and start playing.\n \n" +
             "Recommended: \n" +
             "You can open cmd and search with 'ipconfig' to find you IP Address \n" + 
             "Or click OK to skip this part",
             "Game Information",
             JOptionPane.INFORMATION_MESSAGE
         );
         String serverAddress = JOptionPane.showInputDialog("Enter server IP address:");
        // String serverAddress = "localhost";
         try {
             new CalculateGame(serverAddress);
         } catch (IOException e) {
             e.printStackTrace();
         }
     }
 }
 
  /* Old code
 public class CalculateGame {
     private static final String SERVER_ADDRESS = "localhost";
     private static final int SERVER_PORT = 12345;
 
     public static void main(String[] args) {
         try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
              PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
              BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
              BufferedReader consoleInput = new BufferedReader(new InputStreamReader(System.in))) {
 
             String response;
             while ((response = in.readLine()) != null) {
                 System.out.println(response);  // Display server messages
 
                 if (response.contains("Round")) {
                     String playerAnswer = consoleInput.readLine();
                     out.println(playerAnswer);
                     out.flush();  // Ensure the message is sent immediately
                 }
             }
         } catch (IOException e) {
             e.printStackTrace();
         }
     }
 }
 */
 