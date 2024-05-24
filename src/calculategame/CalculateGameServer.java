package calculategame;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;



public class CalculateGameServer {
    private static final int SERVER_PORT = 8888;
    private static List<PlayerHandler> players = new ArrayList<>();
    private static BlockingQueue<String> questionQueue = new LinkedBlockingQueue<>();
    private static volatile String currentQuestion;
    private static final int TOTAL_QUESTION = 100;
    private static final int WINNING_SCORE = 10;
    private static int questionCount = 0;
    private static int counter = 0;
    // private static int wrongAnswersCount = 0;

    public static void main(String[] args) {
        System.out.println("Calculate Game Server is running...");
        resetServer();
    }

    // Reset server when player close connection or endgame.
    private static void resetServer(){
        players.clear();
        questionQueue.clear();
        questionCount = 0;
        counter = 0;
        new Thread(new QuestionGenerator()).start();
        new Thread(new PlayerConnectionManager()).start();
    }

    /* Method synchronized */
    private static synchronized void broadcastQuestion(String question) {
        for (PlayerHandler player : players) {
            player.sendQuestion(question);
        }
    }

    // private static synchronized void updateScores() {
    //     String scores = getScores();
    //     for (PlayerHandler player : players) {
    //         player.sendScoreUpdate(scores);
    //     }
    // }

    // private static synchronized String getScores() {
    //     StringBuilder scores = new StringBuilder();
    //     for (PlayerHandler player : players) {
    //         scores.append(player.getPlayerName()).append(": ").append(player.getScore()).append("\n");
    //     }
    //     return scores.toString();
    // }

    private static synchronized void checkForWinner(PlayerHandler player) {
        if (player.getScore() >= WINNING_SCORE) {
            broadcastEndGame(player.getPlayerName());
        }
    }

    private static synchronized void broadcastEndGame(String winner) {
        for (PlayerHandler player : players) {
            player.endGame(winner);
            player.resetPlayer();
            System.out.print(player.playerName + " " + player.isReady);
            // player.playAgain();
        }
        resetServer();
    }

    // private static synchronized void removePlayer(PlayerHandler playerHandler){
    //     players.remove(playerHandler);
    //     if (players.isEmpty()){
    //         resetServer();
    //     }
    // }

    /*  Handle question: next question will show when any player input correct answer or all player input wrong answer. */
    /*
    private static synchronized void handleAnswer(PlayerHandler player, int answer) {
        String[] questionParts = currentQuestion.split(" ");
        int num1 = Integer.parseInt(questionParts[1]);
        String operator = questionParts[2];
        int num2 = Integer.parseInt(questionParts[3]);
        // int correctAnswer = operator.equals("+") ? num1 + num2 : num1 - num2;

        if (player.checkAnswer(num1, num2, operator, answer)) {
            player.increaseScore();
            wrongAnswersCount = 0;
            updateScores();
            checkForWinner(player);

            try {
                currentQuestion = questionQueue.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            broadcastQuestion(currentQuestion);
        }
        else {
            wrongAnswersCount++;
            if (wrongAnswersCount >= players.size()) {
                wrongAnswersCount = 0;
                try {
                    currentQuestion = questionQueue.take();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                broadcastQuestion(currentQuestion);
            }
        }
    }
    */

    // private static synchronized String determineWinner() {
    //     if (players.isEmpty()) {
    //         return "No players.";
    //     }
    //     PlayerHandler winner = players.get(0);
    //     for (PlayerHandler player : players) {
    //         if (player.getScore() > winner.getScore()) {
    //             winner = player;
    //         }
    //     }
    //     return winner.getPlayerName() + " is the winner with " + winner.getScore() + " points!";
    // }

    // Thread for generate question
    private static class QuestionGenerator implements Runnable {
        private final char[] operators = {'+', '-', 'x', '/'};

        @Override
        public void run(){
            while (questionCount < TOTAL_QUESTION) {
                try {
                    String question = generateNewQuestion();
                    questionQueue.put(question);
                    // System.out.println("Question counter: " + questionCount);
                    questionCount++;
                    // Thread.sleep(30000); // Wait 30 seconds before generating the next question
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        private String generateNewQuestion(){
            Random rand = new Random();
            int currentOperator = rand.nextInt(4);
            int num1 = rand.nextInt(100);
            int num2 = rand.nextInt(100);

            if (currentOperator == 1){
                while (true){
                    if (num1 >= num2) {break;}
                    num1 = rand.nextInt(100);
                }
            }
            else if (currentOperator == 2){
                num1 = rand.nextInt(26);
                num2 = rand.nextInt(26);
            }
            else if (currentOperator == 3) {
                while (true) { 
                    num2 = rand.nextInt(13);
                    if (num2 != 0 && num2 != 1){
                        num1 = num2 * rand.nextInt(26);
                        break;
                    }
                }    
            }
            return num1 + " " + operators[currentOperator] + " " + num2 ;
        }
    }

    // Thread for handles incoming player, manages player stage, broadcast question
    private static class PlayerConnectionManager implements Runnable {
        @Override
        public void run() {
            try (ServerSocket serverSocket = new ServerSocket(SERVER_PORT)) {
                while (true) {
                    Socket playerSocket = serverSocket.accept();
                    PlayerHandler playerHandler = new PlayerHandler(playerSocket);

                    System.out.println(playerSocket.getInetAddress().getHostAddress() + ": " + playerSocket.getPort());

                    players.add(playerHandler);
                    new Thread(playerHandler).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    // Thread for individual player (name, score, connection)
    private static class PlayerHandler implements Runnable {
        private Socket socket;
        private String playerName;
        private PrintWriter out;
        private BufferedReader in;
        private boolean isReady = false;
        private int score = 0;
        // private int answered = 0;

        private PlayerHandler(Socket socket){
            this.socket = socket;
        }

        @Override
        public void run(){
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
                // out.print("Enter your name:");
                playerName = in.readLine();
                // out.println("Welcome to the Calculate Game!");
                out.println("Welcome to the Calculate Game " + playerName + "! Click 'ready' when you are ready to start.");

                String input;
                while ((input = in.readLine()) != null) {
                    if (input.equalsIgnoreCase("ready")) {
                        isReady = true;
                        // out.println("You are ready. Waiting for other players...");
                        if (allPlayersReady()){
                            currentQuestion = questionQueue.take();
                            broadcastQuestion(currentQuestion);
                            System.out.println("start laew ja");
                        }
                    }
                    // else if (input.equalsIgnoreCase("play again")) {
                    //     resetPlayer();
                    //     out.println("Waiting for other players...");
                    //     if (allPlayersReady()) {
                    //         currentQuestion = questionQueue.take();
                    //         broadcastQuestion(currentQuestion);
                    //     }
                    // }
                    else if (isReady) {
                        try{
                            int playerAnswer = Integer.parseInt(input);
                            String[] questionParts = currentQuestion.split(" ");
                            System.out.println("Question:" + Arrays.toString(questionParts));

                            int num1 = Integer.parseInt(questionParts[0]);
                            String operator = questionParts[1];
                            int num2 = Integer.parseInt(questionParts[2]);

                            if (checkAnswer(num1, num2, operator, playerAnswer)){
                                score++;
                                System.out.println("god");
                            }
                            else {
                                System.out.println("noob");
                            }
                            counter++;
                            checkForWinner(this);

                            if (score < WINNING_SCORE) {
                                currentQuestion = questionQueue.take();
                                broadcastQuestion(currentQuestion);
                            }

                            // int playerAnswer = Integer.parseInt(input);
                            // handleAnswer(this, playerAnswer);
                        }
                        catch (NumberFormatException | InterruptedException e){
                            e.printStackTrace();
                        }
                    }
                    
                }

            } 
            catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
            finally{
               try {
                    socket.close();
               }
               catch (IOException e) {
                e.printStackTrace();
               }

            //    removePlayer(this);
            }
        }

        private boolean checkAnswer(int currentNumber1, int currentNumber2, String operator, int answer) {
            int correctAnswer;
            switch(operator){
                case "+": correctAnswer = currentNumber1 + currentNumber2; break;
                case "-": correctAnswer = currentNumber1 - currentNumber2; break;
                case "x": correctAnswer = currentNumber1 * currentNumber2; break;
                case "/": correctAnswer = currentNumber1 / currentNumber2; break;
                default: return false;
            }
            return answer == correctAnswer;
        }

        private boolean allPlayersReady() {
            for (PlayerHandler player : players) {
                if (!player.isReady) {
                    return false;
                }
            }
            return true;
        }

        private void sendQuestion(String question) {
            int currentCount = counter + 1;
            out.println("Question " + currentCount + ": " + question);
            System.out.println("counter: " + counter);
        }

        public String getPlayerName() {
            return playerName;
        }

        // public void increaseScore() {
        //     this.score++;
        // }

        // private void sendScoreUpdate(String scores) {
        //     out.println("Score Update: " + scores);
        // }

        private int getScore(){
            return score;
        }

        private void endGame(String winner) {
            out.println("Game Over! The winner is '" + winner + "'");
            // out.println("Thank you for playing!");
        }

        // private void playAgain() {
        //     out.println("Game Over! Would you like to play again? Type 'play again' to continue or 'exit' to quit.");
        // }

        private void resetPlayer() {
            System.out.print(this.playerName + ": Reseto");
            score = 0;
            isReady = false;
        }
    }
}

/* Old code
public class CalculateGameServer {
    private static final int SERVERPORT = 12345;
    private static Set<PlayerHandler> players = ConcurrentHashMap.newKeySet();
    private static Set<PlayerHandler> readyPlayers = ConcurrentHashMap.newKeySet();
    private static final int TOTAL_QUESTION = 10;

    public static void main(String[] args) {
        // Socket socket = new Socket();
        System.out.println("Server is running...");
        try (ServerSocket serverSocket = new ServerSocket(SERVERPORT)) {
            while (true) {
                Socket playerSocket = serverSocket.accept();

                System.out.println(playerSocket.getInetAddress().getHostAddress() + ": " + playerSocket.getPort());

                PlayerHandler playerHandler = new PlayerHandler(playerSocket);
                players.add(playerHandler);
                new Thread(playerHandler).start();

                System.out.println("what");
            }
        } catch (IOException err) {
            err.printStackTrace();
        }
    }

    private static class PlayerHandler implements Runnable {
        private Socket socket;
        private PrintWriter printOut;
        private BufferedReader in;
        private int playerNo = 1;
        private int scores = 0;

        private PlayerHandler(Socket socket){
            this.socket = socket;
        }
        
        @Override
        public void run() {
            try {
                printOut = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                printOut.println("Welcome ja");

                broadcastMessage("Player" + playerNo++ + "has joined game!");
                printOut.print("Type something' when you are read to play.: ");
                String message;
                while ((message = in.readLine()) != null) {
                    markAsReady(this);
                    break;
                }

                synchronized (readyPlayers) {
                    while (readyPlayers.size() != players.size()) {
                        readyPlayers.wait();
                    }
                }
                

                printOut.println();

                Random rand = new Random();
                int correctAnswer = 0;
                for (int i = 0; i < 5; i++) {
                    int num1 = rand.nextInt(100);
                    int num2 = rand.nextInt(100);
                    char[] operators = {'+', '-', 'x', '/'};
                    char operator = operators[rand.nextInt(4)];

                    switch(operator){
                        case '+': correctAnswer = num1 + num2; break;
                        case '-': correctAnswer = num1 - num2; break;
                        case 'x': correctAnswer = num1 * num2; break;
                        case '/': correctAnswer = num1 / num2; break; // make it have no remainder by 
                    }
                    
                    String problem = String.format("Round %d: %d %c %d =", i+1, num1, operator,num2);
                    broadcastMessage(problem);

                    int playerAns = Integer.parseInt(in.readLine());
                    printOut.printf("\n");

                    if (playerAns == correctAnswer){
                        printOut.println("Good!");
                        scores++;
                    }
                    else {
                        printOut.println("You get better!");
                    }
                }

                printOut.printf("Your final Score(s) : %d.%n", scores);
                printOut.flush();
                socket.close();
            }
            catch(IOException | InterruptedException err){
              err.printStackTrace();
            }
            finally {
                players.remove(this);
                broadcastMessage("Player " + playerNo + " has left the game.");
            }
        }

        private void markAsReady(PlayerHandler PlayerHandler) throws InterruptedException {
            synchronized (readyPlayers) {
                readyPlayers.add(PlayerHandler);
                broadcastMessage("Player " + playerNo + " is ready!");
                if (readyPlayers.size() == players.size()) {
                    readyPlayers.notifyAll();
                }
            }
        }

        private void broadcastMessage(String message) {
            for (PlayerHandler player : players) {
                player.printOut.println(message);
            }
        }

    }
}
*/
