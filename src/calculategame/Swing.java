package calculategame;

import java.io.*;
import java.net.*;
import javax.swing.*;
import java.awt.event.*;

public class Swing {
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

    public Swing(String serverAddress) throws IOException {
        socket = new Socket(serverAddress, 12345);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        frame = new JFrame("Calculate Game - Client");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 300);
        frame.setLayout(null);

        playerName = JOptionPane.showInputDialog("Enter your name:");
        out.println(playerName);

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
                if (answer.isEmpty()) {
                    JOptionPane.showMessageDialog(frame, "Answer cannot be empty. Please enter a valid response.");
                } else {
                    out.println(answer);
                    answerField.setText("");
                    submitButton.setEnabled(false); // Disable the submit button until the next question is received
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
                while ((response = in.readLine()) != null) {
                    if (response.startsWith("Question: ")) {
                        questionLabel.setText(response);
                        submitButton.setEnabled(true); // Enable submit button for new question
                    } else if (response.startsWith("Score Update: ")) {
                        scoreArea.setText(response.substring(13));
                    } else if (response.startsWith("Game Over!")) {
                        JOptionPane.showMessageDialog(frame, response);
                        readyButton.setEnabled(false);
                        answerField.setEnabled(false);
                        submitButton.setEnabled(false);
                        break; // Exit the loop since the game is over
                    } else {
                        JOptionPane.showMessageDialog(frame, response);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main(String[] args) {
        String serverAddress = JOptionPane.showInputDialog("Enter server IP address:");
        try {
            new Swing(serverAddress);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}