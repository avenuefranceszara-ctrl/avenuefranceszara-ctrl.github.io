package com.mycompany.minesweeper;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import javax.swing.Timer;

public class MINESWEEPER {

    private static final String USERS_FILE = "users.txt";
    private static final String LEADERBOARD_FILE = "leaderboard.txt";

    private JFrame frame;
    private String currentUser;
    private String currentDifficulty = "Unknown";

    // NEW COUNTERS
    private int minesDefused = 0;
    private int boxesOpened = 0;

    // TIMER
    private JLabel timerLabel;
    private Timer gameTimer;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MINESWEEPER().init());
    }

    private void init() {
        ensureFile(USERS_FILE);
        ensureFile(LEADERBOARD_FILE);
        frame = new JFrame("MATH MINESWEEPER");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(700, 700);
        showMainMenu();
        frame.setVisible(true);
    }

    // ------------------ Main menu ------------------
    private void showMainMenu() {
        resetFrameContent();
        frame.setLayout(new GridLayout(6, 1, 8, 8));

        JLabel title = new JLabel("Math Minesweeper", SwingConstants.CENTER);
        title.setFont(new Font("Algeria", Font.BOLD, 50));
        frame.add(title);

        JButton signup = new JButton("Sign Up");
        JButton login  = new JButton("Log In");
        JButton deleteUser  = new JButton("Delete Username");
        JButton lb     = new JButton("Leaderboard");
        JButton exit   = new JButton("Exit");

        frame.add(signup);
        frame.add(login);
        frame.add(deleteUser);
        frame.add(lb);
        frame.add(exit);

        signup.addActionListener(e -> doSignup());
        login.addActionListener(e -> doLogin());
        deleteUser.addActionListener(e -> deleteUsername());
        lb.addActionListener(e -> showLeaderboardDialog());

        exit.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(
                frame,
                "Are you sure you want to exit?",
                "Confirm Exit",
                JOptionPane.YES_NO_OPTION
            );
            if (confirm == JOptionPane.YES_OPTION) System.exit(0);
        });

        frame.revalidate();
        frame.repaint();
    }

    // ------------------ Signup / Login ------------------
    private void doSignup() {
        while (true) {
            String u = JOptionPane.showInputDialog(frame, "Enter username:");
            if (u == null) return;
            u = u.trim();

            if (u.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Username cannot be empty.");
                continue;
            }

            try {
                Path p = Paths.get(USERS_FILE);
                List<String> users = Files.exists(p) ? Files.readAllLines(p) : new ArrayList<>();

                // Check if username exists (file stores lines like "username:password")
                boolean exists = false;
                for (String line : users) {
                    if (line.startsWith(u + ":")) {
                        exists = true;
                        break;
                    }
                }

                if (exists) {
                    JOptionPane.showMessageDialog(frame,
                            "USERNAME ALREADY EXISTS!\nReturning to SIGN UP menu.");
                    continue; // balik sign up
                }

                // Ask password
                String pw1 = JOptionPane.showInputDialog(frame, "Enter password:");
                if (pw1 == null) return;

                String pw2 = JOptionPane.showInputDialog(frame, "Re-enter password:");
                if (pw2 == null) return;

                if (!pw1.equals(pw2)) {
                    JOptionPane.showMessageDialog(frame,
                            "Passwords do not match!\nPlease re-enter.");
                    continue; // balik password input
                }

                // Save USERNAME:password
                Files.write(p, Collections.singletonList(u + ":" + pw1),
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);

                currentUser = u;
                JOptionPane.showMessageDialog(frame,
                        "ACCOUNT SUCCESSFULLY CREATED!\nWELCOME " + u.toUpperCase() + "!");

                // RETURN TO MAIN MENU
                showMainMenu();
                return;

            } catch (IOException ex) {
                JOptionPane.showMessageDialog(frame, "Failed to sign up: " + ex.getMessage());
                return;
            }
        }
    }

    private void doLogin() {
        String u = JOptionPane.showInputDialog(frame, "Enter username:");
        if (u == null) return;
        u = u.trim();

        try {
            Path p = Paths.get(USERS_FILE);
            if (!Files.exists(p)) {
                JOptionPane.showMessageDialog(frame, "No users found.");
                return;
            }

            List<String> users = Files.readAllLines(p);
            String found = null;

            // Look for username (line format username:password)
            for (String line : users) {
                if (line.startsWith(u + ":")) {
                    found = line;
                    break;
                }
            }

            if (found == null) {
                JOptionPane.showMessageDialog(frame, "User not found.");
                return;
            }

            String[] parts = found.split(":", 2);
            String correctPw = parts.length > 1 ? parts[1] : "";

            // Keep asking password until correct or canceled
            while (true) {
                String pw = JOptionPane.showInputDialog(frame, "Enter password:");
                if (pw == null) return; // user canceled

                if (pw.equals(correctPw)) {
                    currentUser = u;
                    JOptionPane.showMessageDialog(frame,
                        "WELCOME BACK " + u.toUpperCase() + "!");
                    chooseDifficulty(); // difficulty now appears only after success
                    return;
                } else {
                    JOptionPane.showMessageDialog(frame,
                        "WRONG PASSWORD, PLEASE RE-ENTER");
                }
            }

        } catch (IOException ex) {
            JOptionPane.showMessageDialog(frame, "Failed to read users: " + ex.getMessage());
        }
    }

    // ------------------ Delete Username ------------------
    private void deleteUsername() {
        String u = JOptionPane.showInputDialog(frame, "Enter username to delete:");
        if (u == null) return;
        u = u.trim();
        if (u.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Username cannot be empty.");
            return;
        }

        try {
            Path userPath = Paths.get(USERS_FILE);
            List<String> users = Files.exists(userPath) ? Files.readAllLines(userPath) : new ArrayList<>();

            // Check if username exists (lines stored as username:password)
            boolean exists = false;
            for (String line : users) {
                if (line.startsWith(u + ":")) {
                    exists = true;
                    break;
                }
            }

            if (!exists) {
                JOptionPane.showMessageDialog(frame, "Username not found.");
                return;
            }

            List<String> updatedUsers = new ArrayList<>();
            for (String line : users) {
                if (!line.startsWith(u + ":")) updatedUsers.add(line);
            }

            Files.write(userPath, updatedUsers, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);

            Path lbPath = Paths.get(LEADERBOARD_FILE);
            if (Files.exists(lbPath)) {
                List<String> scores = Files.readAllLines(lbPath);
                List<String> updatedScores = new ArrayList<>();

                for (String s : scores) {
                    // leaderboard lines are username,time,minesDefused,boxesOpened,difficulty
                    if (!s.startsWith(u + ",")) updatedScores.add(s);
                }

                Files.write(lbPath, updatedScores, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
            }

            // If the deleted user is currently logged in, log them out.
            if (currentUser != null && currentUser.equals(u)) {
                currentUser = null;
            }

            JOptionPane.showMessageDialog(frame, "User \"" + u + "\" and related leaderboard entries deleted.");

        } catch (IOException ex) {
            JOptionPane.showMessageDialog(frame, "Error deleting user: " + ex.getMessage());
        }
    }

    // ------------------ Difficulty ------------------
    private void chooseDifficulty() {
        Object[] opts = {"Easy (8x8, 10)", "Medium (12x12, 25)", "Hard (16x16, 40)", "Back to Menu"};
        int ch = JOptionPane.showOptionDialog(frame, "Choose difficulty", "Difficulty",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, opts, opts[0]);
        if (ch == JOptionPane.CLOSED_OPTION || ch == 3) {
            return;
        } else if (ch == 0) {
            currentDifficulty = "Easy";
            startGame(8, 8, 10);
        } else if (ch == 1) {
            currentDifficulty = "Medium";
            startGame(12, 12, 25);
        } else if (ch == 2) {
            currentDifficulty = "Hard";
            startGame(16, 16, 40);
        }
    }

    // ------------------ Game data ------------------
    class Cell {
        boolean mine = false;
        int num = 0;
        boolean revealed = false;
        boolean flagged = false;
        JButton btn;
    }

    private Cell[][] board;
    private int rows, cols, mines;
    private int revealedCount = 0;
    private long startTime;
    private final Random rnd = new Random();

    // ------------------ Start Game ------------------
    private void startGame(int r, int c, int m) {

        minesDefused = 0;
        boxesOpened = 0;

        this.rows = r;
        this.cols = c;
        this.mines = m;
        this.revealedCount = 0;

        resetFrameContent();
        frame.setLayout(new BorderLayout(6, 6));

        JPanel topBar = new JPanel(new BorderLayout());
        JLabel info = new JLabel();
        updateInfoLabel(info);
        info.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        topBar.add(info, BorderLayout.WEST);

        timerLabel = new JLabel(" Time: 0s ");
        timerLabel.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        topBar.add(timerLabel, BorderLayout.CENTER);

        JButton back = new JButton("Back to Menu");
        back.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(frame, "Leave game and return to menu?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                if (gameTimer != null) gameTimer.stop();
                showMainMenu();
            }
        });
        topBar.add(back, BorderLayout.EAST);

        frame.add(topBar, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(r, c));
        board = new Cell[r][c];

        for (int i = 0; i < r; i++) {
            for (int j = 0; j < c; j++) {
                board[i][j] = new Cell();
                JButton btn = new JButton();
                btn.setMargin(new Insets(0,0,0,0));
                btn.setFont(new Font("Arial", Font.BOLD, Math.max(12, 36 - Math.max(r,c))));
                board[i][j].btn = btn;

                final int R = i, C = j;

                btn.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        if (SwingUtilities.isRightMouseButton(e)) {
                            toggleFlag(R, C, info);
                        } else if (SwingUtilities.isLeftMouseButton(e)) {
                            clickCell(R, C, info);
                        }
                    }
                });

                grid.add(btn);
            }
        }

        frame.add(grid, BorderLayout.CENTER);
        frame.revalidate();
        frame.repaint();

        placeMines(m);
        computeNumbers();
        startTime = System.currentTimeMillis();

        gameTimer = new Timer(1000, e -> {
            long seconds = (System.currentTimeMillis() - startTime) / 1000;
            timerLabel.setText(" Time: " + seconds + "s ");
        });
        gameTimer.start();
    }

    private void updateInfoLabel(JLabel info) {
        info.setText(
            " Player: " + (currentUser == null ? "(guest)" : currentUser) +
            "   Mines: " + mines +
            "   Mines Defused: " + minesDefused +
            "   Boxes Opened: " + boxesOpened +
            "   Difficulty: " + currentDifficulty
        );
    }

    private void resetFrameContent() {
        if (frame == null) return;
        frame.getContentPane().removeAll();
        frame.getContentPane().repaint();
    }

    // ------------------ Mines ------------------
    private boolean inBounds(int r, int c) { return r >= 0 && r < rows && c >= 0 && c < cols; }

    private void placeMines(int m) {
        int placed = 0;
        while (placed < m) {
            int r = rnd.nextInt(rows);
            int c = rnd.nextInt(cols);
            if (!board[r][c].mine) {
                board[r][c].mine = true;
                placed++;
            }
        }
    }

    private void computeNumbers() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (board[r][c].mine) {
                    board[r][c].num = -1;
                    continue;
                }
                int cnt = 0;
                for (int dr = -1; dr <= 1; dr++)
                    for (int dc = -1; dc <= 1; dc++)
                        if (inBounds(r + dr, c + dc) && board[r + dr][c + dc].mine) cnt++;
                board[r][c].num = cnt;
            }
        }
    }

    // ------------------ Interactions ------------------
    private void toggleFlag(int r, int c, JLabel info) {
        Cell cell = board[r][c];
        if (cell.revealed) return;
        cell.flagged = !cell.flagged;
        cell.btn.setText(cell.flagged ? "F" : "");
    }

    private void clickCell(int r, int c, JLabel info) {
        Cell cell = board[r][c];
        if (cell.revealed || cell.flagged) return;

        if (cell.mine) {
            boolean correct = askMathQuestionAdaptive();
            if (correct) {
                cell.mine = false;
                minesDefused++;
                JOptionPane.showMessageDialog(frame, "Correct! Mine defused.");
                computeNumbers();
                revealCell(r, c, info);
            } else {
                JOptionPane.showMessageDialog(frame, "Wrong! Game Over.");
                if (gameTimer != null) gameTimer.stop();
                showMainMenu();
                return;
            }
        } else {
            revealCell(r, c, info);
        }

        updateInfoLabel(info);
        checkWinCondition(info);
    }

    private void revealCell(int r, int c, JLabel info) {
        if (!inBounds(r, c)) return;
        Cell cell = board[r][c];
        if (cell.revealed) return;

        cell.revealed = true;
        revealedCount++;
        boxesOpened++;

        cell.btn.setEnabled(false);

        if (cell.mine) cell.btn.setText("M");
        else if (cell.num == 0) cell.btn.setText("");
        else cell.btn.setText(Integer.toString(cell.num));

        updateInfoLabel(info);

        if (!cell.mine && cell.num == 0) {
            for (int dr = -1; dr <= 1; dr++)
                for (int dc = -1; dc <= 1; dc++)
                    revealCell(r + dr, c + dc, info);
        }
    }

    // ------------------ Math Question ------------------
    private boolean askMathQuestionAdaptive() {
        int mode = 1;
        if (rows >= 16) mode = 3;
        else if (rows >= 12) mode = 2;

        int a, b, ans;
        String op;
        Random r = rnd;

        if (mode == 1) {
            a = r.nextInt(10) + 1;
            b = r.nextInt(10) + 1;
            ans = a + b;
            op = "+";
        } else if (mode == 2) {
            int t = r.nextInt(3);
            if (t == 0) { a = r.nextInt(30)+1; b = r.nextInt(30)+1; ans = a + b; op = "+"; }
            else if (t == 1) { a = r.nextInt(40)+1; b = r.nextInt(20)+1; ans = a - b; op = "-"; }
            else { a = r.nextInt(12)+2; b = r.nextInt(12)+2; ans = a * b; op = "*"; }
        } else {
            int t = r.nextInt(4);
            if (t == 0) { a = r.nextInt(80)+1; b = r.nextInt(80)+1; ans = a + b; op = "+"; }
            else if (t == 1) { a = r.nextInt(100)+1; b = r.nextInt(80)+1; ans = a - b; op = "-"; }
            else if (t == 2) { a = r.nextInt(20)+2; b = r.nextInt(15)+2; ans = a * b; op = "*"; }
            else { b = r.nextInt(9)+2; ans = r.nextInt(12)+1; a = ans * b; op = "/"; }
        }

        String prompt = "You stepped on a MATH MINE!\nSolve:\n   " + a + " " + op + " " + b + " = ?";
        String input = JOptionPane.showInputDialog(frame, prompt);
        if (input == null) return false;

        try {
            int user = Integer.parseInt(input.trim());
            return user == ans;
        } catch (Exception ex) {
            return false;
        }
    }

    // ------------------ Win Condition ------------------
    private void checkWinCondition(JLabel info) {
        int totalCells = rows * cols;
        if (revealedCount >= totalCells - mines) {

            if (gameTimer != null) gameTimer.stop();

            long seconds = (System.currentTimeMillis() - startTime) / 1000;

            JOptionPane.showMessageDialog(frame,
                "YOU WIN!\n" +
                "Time: " + seconds + " seconds\n" +
                "Mines Defused: " + minesDefused + "\n" +
                "Boxes Opened: " + boxesOpened + "\n" +
                "Difficulty: " + currentDifficulty
            );

            saveScore();
            showMainMenu();
        }
    }

    private void saveScore() {
        // Save only if a real user is logged in (not guest)
        if (currentUser == null || currentUser.equals("(guest)")) return;
        try {
            long seconds = (System.currentTimeMillis() - startTime) / 1000;
            // Format: username,time,minesDefused,boxesOpened,difficulty
            String line = currentUser + "," + seconds + "," + minesDefused + "," + boxesOpened + "," + currentDifficulty;
            Path p = Paths.get(LEADERBOARD_FILE);
            Files.write(p, Collections.singletonList(line),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ex) {
            System.out.println("Error saving score: " + ex.getMessage());
        }
    }

    // ------------------ Leaderboard ------------------
    private void showLeaderboardDialog() {
        try {
            Path p = Paths.get(LEADERBOARD_FILE);
            List<String> lines = Files.exists(p) ? Files.readAllLines(p) : Collections.emptyList();
            if (lines.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "No scores yet.");
                return;
            }

            List<String[]> parsed = new ArrayList<>();
            for (String line : lines) {
                String[] parts = line.split(",");
                // Accept entries of length >= 4 (we expect 5: user,time,mines,boxes,diff)
                if (parts.length >= 4) parsed.add(parts);
            }

            // Sort by time (parts[1]) with safe parsing
            parsed.sort((a, b) -> {
                int ta = tryParseInt(a, 1, Integer.MAX_VALUE);
                int tb = tryParseInt(b, 1, Integer.MAX_VALUE);
                return Integer.compare(ta, tb);
            });

            StringBuilder sb = new StringBuilder("LEADERBOARD\n\n");
            int limit = Math.min(10, parsed.size());
            for (int i = 0; i < limit; i++) {
                String[] e = parsed.get(i);
                String user = e.length > 0 ? e[0] : "unknown";
                String time = e.length > 1 ? e[1] : "N/A";
                String mines = e.length > 2 ? e[2] : "0";
                String boxes = e.length > 3 ? e[3] : "0";
                String diff = e.length > 4 ? e[4] : "Unknown";

                sb.append(String.format(
                    "%d) %s - %s sec | Mines Defused: %s | Boxes: %s | %s%n",
                    i + 1, user, time, mines, boxes, diff
                ));
            }

            JOptionPane.showMessageDialog(frame, sb.toString());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(frame, "Failed to read leaderboard: " + ex.getMessage());
        }
    }

    private int tryParseInt(String[] arr, int idx, int fallback) {
        try {
            if (arr.length > idx) return Integer.parseInt(arr[idx]);
            else return fallback;
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private void ensureFile(String filename) {
        try {
            Path p = Paths.get(filename);
            if (!Files.exists(p)) Files.createFile(p);
        } catch (IOException e) {}
    }

}
