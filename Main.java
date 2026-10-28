import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class main extends JFrame {

    private final List<String[]> habits = new ArrayList<>();
    private final List<String[]> historyRecords = new ArrayList<>();

    private final String DATA_DIR = "data";
    private final String HABITS_FILE = DATA_DIR + "/habits.txt";
    private final String PROFILE_FILE = DATA_DIR + "/profile.txt";
    private final String HISTORY_FILE = DATA_DIR + "/history.txt";
    private final String PERSONAL_FILE;
    private final String currentUsername;
    private final Set<String> reminderShown = new HashSet<>();
    private javax.swing.Timer reminderTimer;
    private String[] personalDetails = new String[8];
    private final String[] personalFieldNames = {
        "Full Name", "Date of Birth", "Email", "Phone",
        "Address", "City", "State", "PIN Code"
    };

    private JTable habitTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JComboBox<String> categoryFilter;

    private JLabel totalLabel;
    private JLabel completedLabel;
    private JLabel progressLabel;
    private JLabel xpLabel;
    private JLabel levelLabel;
    private JLabel streakLabel;
    private JProgressBar progressBar;

    private int totalXP = 0;
    private int dailyStreak = 0;
    private String lastStreakDate = "";

    private final String[] categories = {
        "Study", "Health", "Fitness", "Personal", "Other"
    };

    public main() { this("User"); }

    public main(String username) {
        this.currentUsername = (username == null || username.trim().isEmpty()) ? "User" : username.trim();
        this.PERSONAL_FILE = DATA_DIR + "/profiles/" + safeFileName(this.currentUsername) + ".txt";
        setTitle("Daily Habit Tracker - " + this.currentUsername);
        setSize(1100, 740);
        setMinimumSize(new Dimension(850, 600));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        new File(DATA_DIR).mkdirs();

        loadHabits();
        loadHistory();
        loadProfile();
        loadPersonalDetails();
        resetOldDailyStatuses();

        buildUI();
        applyDarkPremiumTheme(getContentPane());
        updateDashboard();
        saveAll();
        startReminderTimer();
    }

    private void buildUI() {
        setLayout(new BorderLayout(12, 12));
        getContentPane().setBackground(new Color(241, 245, 249));

        JPanel header = new JPanel(new BorderLayout());
        header.setName("appHeader");
        header.setBackground(BG);
        header.setBorder(
            BorderFactory.createEmptyBorder(18, 22, 18, 22)
        );

        JLabel title = new JLabel("Daily Habit Flow");
        title.setFont(new Font("Arial", Font.BOLD, 27));
        title.setForeground(Color.WHITE);

        JLabel subtitle = new JLabel(
            "Build habits. Earn XP. Become better."
        );
        subtitle.setForeground(new Color(203, 213, 225));
        subtitle.setFont(new Font("Arial", Font.PLAIN, 13));

        JPanel heading = new JPanel(new GridLayout(2, 1, 0, 5));
        heading.setOpaque(false);
        heading.add(title);
        heading.add(subtitle);

        header.add(heading, BorderLayout.WEST);

        JPanel headerActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        headerActions.setOpaque(false);
        JButton profileButton = createButton("My Profile", new Color(124, 58, 237));
        JButton allProfilesButton = createButton("More Profiles", new Color(13, 148, 136));
        JButton remindersButton = createButton("Reminders", new Color(91, 76, 190));
        headerActions.add(profileButton);
        headerActions.add(allProfilesButton);
        headerActions.add(remindersButton);
        profileButton.addActionListener(e -> showPersonalProfile());
        allProfilesButton.addActionListener(e -> showAllProfiles());
        remindersButton.addActionListener(e -> showReminderOverview());
        header.add(headerActions, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 12));
        mainPanel.setOpaque(false);
        mainPanel.setBorder(
            BorderFactory.createEmptyBorder(0, 15, 15, 15)
        );

        JPanel dashboard = new JPanel(new GridLayout(1, 6, 8, 8));
        dashboard.setOpaque(false);

        totalLabel = createStatLabel();
        completedLabel = createStatLabel();
        progressLabel = createStatLabel();
        xpLabel = createStatLabel();
        levelLabel = createStatLabel();
        streakLabel = createStatLabel();

        dashboard.add(createStatCard("TOTAL HABITS", totalLabel));
        dashboard.add(createStatCard("COMPLETED", completedLabel));
        dashboard.add(createStatCard("PROGRESS", progressLabel));
        dashboard.add(createStatCard("TOTAL XP", xpLabel));
        dashboard.add(createStatCard("YOUR LEVEL", levelLabel));
        dashboard.add(createStatCard("DAILY STREAK", streakLabel));

        JPanel topContent = new JPanel(new BorderLayout(0, 10));
        topContent.setOpaque(false);
        topContent.add(dashboard, BorderLayout.CENTER);

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setPreferredSize(new Dimension(100, 22));
        progressBar.setForeground(new Color(16, 185, 129));

        topContent.add(progressBar, BorderLayout.SOUTH);
        mainPanel.add(topContent, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(8, 8));
        center.setBackground(Color.WHITE);
        center.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(226, 232, 240)),
            BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));

        JPanel filterPanel = new JPanel(
            new FlowLayout(FlowLayout.LEFT, 8, 5)
        );
        filterPanel.setBackground(Color.WHITE);

        searchField = new JTextField(15);

        categoryFilter = new JComboBox<>(new String[]{
            "All Categories", "Study", "Health",
            "Fitness", "Personal", "Other"
        });

        JButton searchButton = createButton(
            "Search", new Color(59, 130, 246)
        );
        JButton showAllButton = createButton(
            "Show All", new Color(100, 116, 139)
        );

        searchButton.addActionListener(e -> refreshTable());

        showAllButton.addActionListener(e -> {
            searchField.setText("");
            categoryFilter.setSelectedIndex(0);
            refreshTable();
        });

        filterPanel.add(new JLabel("Search:"));
        filterPanel.add(searchField);
        filterPanel.add(categoryFilter);
        filterPanel.add(searchButton);
        filterPanel.add(showAllButton);

        center.add(filterPanel, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(
            new String[]{
                "Habit Name", "Category", "Status", "Last Completed", "Reminder"
            }, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        habitTable = new JTable(tableModel);
        habitTable.setRowHeight(30);
        habitTable.setFont(new Font("Arial", Font.PLAIN, 13));
        habitTable.getTableHeader().setFont(
            new Font("Arial", Font.BOLD, 13)
        );
        habitTable.setSelectionMode(
            ListSelectionModel.SINGLE_SELECTION
        );

        center.add(new JScrollPane(habitTable), BorderLayout.CENTER);

        // Action buttons
        JPanel buttonPanel = new JPanel(new GridLayout(3, 3, 7, 7));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.setBorder(
            BorderFactory.createEmptyBorder(8, 0, 0, 0)
        );

        JButton addButton = createButton(
            "Add Habit", new Color(16, 185, 129)
        );
        JButton editButton = createButton(
            "Edit", new Color(59, 130, 246)
        );
        JButton completeButton = createButton(
            "Mark Complete", new Color(22, 163, 74)
        );
        JButton deleteButton = createButton(
            "Delete", new Color(239, 68, 68)
        );
        JButton historyButton = createButton(
            "Habit History", new Color(124, 58, 237)
        );
        JButton weeklyButton = createButton(
            "Weekly Report", new Color(217, 119, 6)
        );
        JButton calendarButton = createButton(
            "Calendar View", new Color(8, 145, 178)
        );
        JButton monthlyButton = createButton(
            "Monthly Calendar", new Color(79, 70, 229)
        );
        JButton achievementsButton = createButton(
            "Achievements", new Color(219, 39, 119)
        );
        JButton analyticsButton = createButton(
            "Monthly Analytics", new Color(14, 165, 233)
        );

        addButton.addActionListener(e -> addHabit());
        editButton.addActionListener(e -> editHabit());
        completeButton.addActionListener(e -> completeHabit());
        deleteButton.addActionListener(e -> deleteHabit());
        historyButton.addActionListener(e -> showHabitHistory());
        weeklyButton.addActionListener(e -> showWeeklyReport());
        calendarButton.addActionListener(e -> showCalendarView());
        monthlyButton.addActionListener(e -> showMonthlyCalendar());
        achievementsButton.addActionListener(e -> showAchievements());
        analyticsButton.addActionListener(e -> showMonthlyAnalytics());

        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(completeButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(historyButton);
        buttonPanel.add(weeklyButton);
        buttonPanel.add(calendarButton);
        buttonPanel.add(monthlyButton);
        buttonPanel.add(achievementsButton);
        // Put Analytics in a final row by using a 4 x 3 grid.
        buttonPanel.setLayout(new GridLayout(4, 3, 7, 7));
        buttonPanel.add(analyticsButton);

        center.add(buttonPanel, BorderLayout.SOUTH);
        mainPanel.add(center, BorderLayout.CENTER);

        add(mainPanel, BorderLayout.CENTER);

        searchField.addActionListener(e -> refreshTable());
        categoryFilter.addActionListener(e -> refreshTable());

        refreshTable();
    }

    private JLabel createStatLabel() {
        JLabel label = new JLabel("0", SwingConstants.CENTER);
        label.setFont(new Font("Arial", Font.BOLD, 21));
        label.setForeground(new Color(31, 41, 55));
        return label;
    }

    private JPanel createStatCard(String title, JLabel value) {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(226, 232, 240)),
            BorderFactory.createEmptyBorder(12, 5, 12, 5)
        ));

        JLabel heading = new JLabel(title, SwingConstants.CENTER);
        heading.setFont(new Font("Arial", Font.BOLD, 10));
        heading.setForeground(new Color(100, 116, 139));

        card.add(heading, BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);

        return card;
    }

    private JButton createButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Arial", Font.BOLD, 11));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setMargin(new Insets(9, 8, 9, 8));
        return button;
    }

    private void addHabit() {
        JTextField nameField = new JTextField();
        JComboBox<String> categoryBox = new JComboBox<>(categories);
        JTextField reminderField = new JTextField("20:00");
        JCheckBox reminderEnabled = new JCheckBox("Enable daily reminder");

        JPanel panel = new JPanel(new GridLayout(0, 1, 5, 5));
        panel.add(new JLabel("Habit Name:"));
        panel.add(nameField);
        panel.add(new JLabel("Category:"));
        panel.add(categoryBox);
        panel.add(new JLabel("Reminder Time (HH:mm):"));
        panel.add(reminderField);
        panel.add(reminderEnabled);

        int result = JOptionPane.showConfirmDialog(
            this, panel, "Add New Habit",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        );

        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        String name = nameField.getText().trim();
        String category = (String) categoryBox.getSelectedItem();

        if (name.isEmpty()) {
            showMessage("Please enter a habit name.");
            return;
        }
        String reminderTime = reminderField.getText().trim();
        if (reminderEnabled.isSelected() && !validReminderTime(reminderTime)) {
            showMessage("Enter reminder time as HH:mm, for example 20:30.");
            return;
        }

        habits.add(new String[]{
            name, category, "false", "Pending", "",
            reminderEnabled.isSelected() ? reminderTime : "",
            Boolean.toString(reminderEnabled.isSelected())
        });

        saveAll();
        refreshTable();
        updateDashboard();
    }

    private void editHabit() {
        int index = getSelectedHabitIndex();

        if (index == -1) {
            showMessage("Please select a habit first.");
            return;
        }

        String[] habit = habits.get(index);

        JTextField nameField = new JTextField(habit[0]);
        JComboBox<String> categoryBox = new JComboBox<>(categories);
        categoryBox.setSelectedItem(habit[1]);
        JTextField reminderField = new JTextField(habit.length > 5 ? habit[5] : "");
        JCheckBox reminderEnabled = new JCheckBox(
            "Enable daily reminder", habit.length > 6 && Boolean.parseBoolean(habit[6])
        );

        JPanel panel = new JPanel(new GridLayout(0, 1, 5, 5));
        panel.add(new JLabel("Habit Name:"));
        panel.add(nameField);
        panel.add(new JLabel("Category:"));
        panel.add(categoryBox);
        panel.add(new JLabel("Reminder Time (HH:mm):"));
        panel.add(reminderField);
        panel.add(reminderEnabled);

        int result = JOptionPane.showConfirmDialog(
            this, panel, "Edit Habit",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        );

        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        String name = nameField.getText().trim();

        if (name.isEmpty()) {
            showMessage("Habit name cannot be empty.");
            return;
        }

        String reminderTime = reminderField.getText().trim();
        if (reminderEnabled.isSelected() && !validReminderTime(reminderTime)) {
            showMessage("Enter reminder time as HH:mm, for example 20:30.");
            return;
        }
        habit[0] = name;
        habit[1] = (String) categoryBox.getSelectedItem();
        if (habit.length < 7) habit = Arrays.copyOf(habit, 7);
        habit[5] = reminderEnabled.isSelected() ? reminderTime : "";
        habit[6] = Boolean.toString(reminderEnabled.isSelected());
        habits.set(index, habit);

        saveAll();
        refreshTable();
        updateDashboard();
    }

    private void completeHabit() {
        int index = getSelectedHabitIndex();

        if (index == -1) {
            showMessage("Please select a habit first.");
            return;
        }

        String[] habit = habits.get(index);
        String today = LocalDate.now().toString();

        if ("true".equals(habit[2]) && today.equals(habit[4])) {
            showMessage("This habit is already completed today!");
            return;
        }

        habit[2] = "true";
        habit[3] = "Completed";
        habit[4] = today;

        recordHabitCompletion(habit, today);
        updateStreak(today);
        totalXP += 10;

        saveAll();
        refreshTable();
        updateDashboard();

        JOptionPane.showMessageDialog(
            this,
            "Great job! You earned 10 XP!",
            "Habit Completed",
            JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void deleteHabit() {
        int index = getSelectedHabitIndex();

        if (index == -1) {
            showMessage("Please select a habit first.");
            return;
        }

        int result = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to delete this habit?",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION
        );

        if (result == JOptionPane.YES_OPTION) {
            habits.remove(index);
            saveAll();
            refreshTable();
            updateDashboard();
        }
    }

    private int getSelectedHabitIndex() {
        int row = habitTable.getSelectedRow();

        if (row == -1) {
            return -1;
        }

        String name = tableModel.getValueAt(row, 0).toString();
        String category = tableModel.getValueAt(row, 1).toString();

        for (int i = 0; i < habits.size(); i++) {
            String[] habit = habits.get(i);

            if (habit[0].equals(name) && habit[1].equals(category)) {
                return i;
            }
        }

        return -1;
    }

    private void refreshTable() {
        if (tableModel == null) {
            return;
        }

        tableModel.setRowCount(0);

        String query = searchField == null
            ? ""
            : searchField.getText().trim().toLowerCase();

        String category = categoryFilter == null
            ? "All Categories"
            : (String) categoryFilter.getSelectedItem();

        for (String[] habit : habits) {
            boolean matchesSearch =
                habit[0].toLowerCase().contains(query);

            boolean matchesCategory =
                category == null
                || category.equals("All Categories")
                || category.equals(habit[1]);

            if (matchesSearch && matchesCategory) {
                tableModel.addRow(new Object[]{
                    habit[0],
                    habit[1],
                    habit[3],
                    habit[4].isEmpty()
                        ? "Not completed yet"
                        : habit[4],
                    habit.length > 6 && Boolean.parseBoolean(habit[6])
                        ? habit[5] : "Off"
                });
            }
        }
    }

    private void updateDashboard() {
        if (totalLabel == null) {
            return;
        }

        String today = LocalDate.now().toString();
        int completed = 0;

        for (String[] habit : habits) {
            if ("true".equals(habit[2]) && today.equals(habit[4])) {
                completed++;
            }
        }

        int total = habits.size();
        int percentage = total == 0 ? 0 : completed * 100 / total;
        int level = totalXP / 100 + 1;

        totalLabel.setText(String.valueOf(total));
        completedLabel.setText(String.valueOf(completed));
        progressLabel.setText(percentage + "%");
        xpLabel.setText(String.valueOf(totalXP));
        levelLabel.setText(String.valueOf(level));
        streakLabel.setText(dailyStreak + " days");

        progressBar.setValue(percentage);
        progressBar.setString(percentage + "% completed today");
    }

    private void resetOldDailyStatuses() {
        String today = LocalDate.now().toString();

        for (String[] habit : habits) {
            if (!today.equals(habit[4])) {
                habit[2] = "false";
                habit[3] = "Pending";
            }
        }

        if (!lastStreakDate.isEmpty()) {
            try {
                LocalDate lastDate = LocalDate.parse(lastStreakDate);

                if (ChronoUnit.DAYS.between(
                        lastDate, LocalDate.now()) > 1) {
                    dailyStreak = 0;
                }
            } catch (Exception e) {
                dailyStreak = 0;
                lastStreakDate = "";
            }
        }
    }

    private void updateStreak(String today) {
        if (today.equals(lastStreakDate)) {
            return;
        }

        if (lastStreakDate.isEmpty()) {
            dailyStreak = 1;
        } else {
            LocalDate last = LocalDate.parse(lastStreakDate);
            LocalDate current = LocalDate.parse(today);
            long days = ChronoUnit.DAYS.between(last, current);

            dailyStreak = days == 1 ? dailyStreak + 1 : 1;
        }

        lastStreakDate = today;
    }

    private void recordHabitCompletion(String[] habit, String date) {
        historyRecords.add(new String[]{
            habit[0], habit[1], date, "10 XP"
        });
        saveHistory();
    }

    private void showHabitHistory() {
        DefaultTableModel model = new DefaultTableModel(
            new String[]{"Habit Name", "Category", "Date", "XP Earned"},
            0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        for (String[] record : historyRecords) {
            if (record.length >= 4) {
                model.addRow(new Object[]{
                    record[0], record[1], record[2], record[3]
                });
            }
        }

        JTable table = new JTable(model);
        table.setRowHeight(25);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(600, 300));

        JOptionPane.showMessageDialog(
            this, scrollPane, "Habit History",
            JOptionPane.PLAIN_MESSAGE
        );
    }

    private void showWeeklyReport() {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(6);

        int completions = 0;
        int xpEarned = 0;

        Set<String> activeDays = new HashSet<>();
        Map<String, Integer> categoryCounts = new LinkedHashMap<>();

        for (String[] record : historyRecords) {
            if (record.length < 4) {
                continue;
            }

            try {
                LocalDate date = LocalDate.parse(record[2]);

                if (!date.isBefore(startDate) && !date.isAfter(today)) {
                    completions++;
                    xpEarned += 10;
                    activeDays.add(record[2]);

                    categoryCounts.put(
                        record[1],
                        categoryCounts.getOrDefault(record[1], 0) + 1
                    );
                }
            } catch (Exception ignored) {
                // Ignore invalid dates.
            }
        }

        StringBuilder report = new StringBuilder();

        report.append("WEEKLY REPORT\n\n");
        report.append("Period: ").append(startDate)
              .append(" to ").append(today).append("\n\n");
        report.append("Habit Completions: ").append(completions).append("\n");
        report.append("XP Earned: ").append(xpEarned).append("\n");
        report.append("Active Days: ").append(activeDays.size())
              .append(" / 7\n\n");
        report.append("CATEGORY BREAKDOWN\n");

        if (categoryCounts.isEmpty()) {
            report.append("No completions recorded this week.\n");
        } else {
            for (String category : categoryCounts.keySet()) {
                report.append(category).append(": ")
                      .append(categoryCounts.get(category))
                      .append(" completions\n");
            }
        }

        JTextArea area = new JTextArea(report.toString(), 14, 35);
        area.setEditable(false);
        area.setFont(new Font("Monospaced", Font.PLAIN, 13));

        JOptionPane.showMessageDialog(
            this, new JScrollPane(area), "Weekly Report",
            JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void showCalendarView() {
        if (historyRecords.isEmpty()) {
            showMessage("No history available. Complete a habit first.");
            return;
        }

        JSpinner dateSpinner = new JSpinner(new SpinnerDateModel());

        dateSpinner.setEditor(
            new JSpinner.DateEditor(dateSpinner, "yyyy-MM-dd")
        );

        int choice = JOptionPane.showConfirmDialog(
            this, dateSpinner, "Select a Date",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        );

        if (choice != JOptionPane.OK_OPTION) {
            return;
        }

        String selectedDate = new java.text.SimpleDateFormat(
            "yyyy-MM-dd"
        ).format((java.util.Date) dateSpinner.getValue());

        DefaultTableModel model = new DefaultTableModel(
            new String[]{"Habit Name", "Category", "Date", "XP"},
            0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        for (String[] record : historyRecords) {
            if (record.length >= 4
                    && record[2].equals(selectedDate)) {
                model.addRow(new Object[]{
                    record[0], record[1], record[2], record[3]
                });
            }
        }

        if (model.getRowCount() == 0) {
            showMessage("No completed habits found on " + selectedDate);
            return;
        }

        JTable table = new JTable(model);
        table.setRowHeight(25);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(550, 250));

        JOptionPane.showMessageDialog(
            this, scrollPane, "Calendar View - " + selectedDate,
            JOptionPane.PLAIN_MESSAGE
        );
    }

    private void showMonthlyCalendar() {
        YearMonth[] selectedMonth = {YearMonth.now()};

        JPanel calendarPanel = new JPanel(new BorderLayout(10, 10));
        calendarPanel.setPreferredSize(new Dimension(650, 440));

        JPanel navigation = new JPanel(new BorderLayout(10, 5));

        JButton previousButton = new JButton("< Previous");
        JButton nextButton = new JButton("Next >");
        JLabel monthLabel = new JLabel("", SwingConstants.CENTER);

        monthLabel.setFont(new Font("Arial", Font.BOLD, 20));

        navigation.add(previousButton, BorderLayout.WEST);
        navigation.add(monthLabel, BorderLayout.CENTER);
        navigation.add(nextButton, BorderLayout.EAST);

        JPanel daysPanel = new JPanel(new GridLayout(0, 7, 5, 5));

        JTextArea details = new JTextArea(4, 40);
        details.setEditable(false);
        details.setFont(new Font("Arial", Font.PLAIN, 13));

        JPanel detailsPanel = new JPanel(new BorderLayout());
        detailsPanel.setBorder(
            BorderFactory.createTitledBorder("Selected Date Details")
        );
        detailsPanel.add(new JScrollPane(details), BorderLayout.CENTER);

        calendarPanel.add(navigation, BorderLayout.NORTH);
        calendarPanel.add(daysPanel, BorderLayout.CENTER);
        calendarPanel.add(detailsPanel, BorderLayout.SOUTH);

        Runnable[] renderCalendar = new Runnable[1];

        renderCalendar[0] = () -> {
            daysPanel.removeAll();

            monthLabel.setText(
                selectedMonth[0].getMonth().toString()
                + " " + selectedMonth[0].getYear()
            );

            String[] weekdays = {
                "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"
            };

            for (String weekday : weekdays) {
                JLabel label = new JLabel(weekday, SwingConstants.CENTER);
                label.setFont(new Font("Arial", Font.BOLD, 12));
                daysPanel.add(label);
            }

            LocalDate firstDay = selectedMonth[0].atDay(1);
            int offset = firstDay.getDayOfWeek().getValue() - 1;

            for (int i = 0; i < offset; i++) {
                daysPanel.add(new JLabel(""));
            }

            for (int day = 1;
                 day <= selectedMonth[0].lengthOfMonth();
                 day++) {

                LocalDate date = selectedMonth[0].atDay(day);
                String dateText = date.toString();
                int completedCount = 0;

                for (String[] record : historyRecords) {
                    if (record.length >= 4
                            && record[2].equals(dateText)) {
                        completedCount++;
                    }
                }

                JButton dayButton = new JButton(
                    completedCount > 0 ? day + " *" : String.valueOf(day)
                );

                dayButton.setMargin(new Insets(5, 2, 5, 2));

                if (completedCount > 0) {
                    dayButton.setBackground(new Color(187, 247, 208));
                    dayButton.setToolTipText(
                        completedCount + " habit completion(s)"
                    );
                }

                if (date.equals(LocalDate.now())) {
                    dayButton.setBorder(
                        BorderFactory.createLineBorder(
                            new Color(79, 70, 229), 2
                        )
                    );
                }

                dayButton.addActionListener(e -> {
                    StringBuilder text = new StringBuilder();

                    text.append("Date: ")
                        .append(dateText)
                        .append("\n\nCompleted Habits:\n");

                    int count = 0;

                    for (String[] record : historyRecords) {
                        if (record.length >= 4
                                && record[2].equals(dateText)) {
                            text.append("- ")
                                .append(record[0])
                                .append(" (")
                                .append(record[1])
                                .append(") - ")
                                .append(record[3])
                                .append("\n");

                            count++;
                        }
                    }

                    if (count == 0) {
                        text.append("No completed habits recorded.");
                    }

                    details.setText(text.toString());
                });

                daysPanel.add(dayButton);
            }

            daysPanel.revalidate();
            daysPanel.repaint();
        };

        previousButton.addActionListener(e -> {
            selectedMonth[0] = selectedMonth[0].minusMonths(1);
            details.setText("");
            renderCalendar[0].run();
        });

        nextButton.addActionListener(e -> {
            selectedMonth[0] = selectedMonth[0].plusMonths(1);
            details.setText("");
            renderCalendar[0].run();
        });

        renderCalendar[0].run();

        JOptionPane.showMessageDialog(
            this, calendarPanel, "Monthly Habit Calendar",
            JOptionPane.PLAIN_MESSAGE
        );
    }

    private void showMonthlyAnalytics() {
        YearMonth[] selectedMonth = {YearMonth.now()};

        JPanel panel = new JPanel(new BorderLayout(10, 12));
        panel.setPreferredSize(new Dimension(720, 510));

        JPanel navigation = new JPanel(new BorderLayout(10, 5));

        JButton previousButton = new JButton("< Previous");
        JButton nextButton = new JButton("Next >");
        JLabel monthLabel = new JLabel("", SwingConstants.CENTER);

        monthLabel.setFont(new Font("Arial", Font.BOLD, 20));

        navigation.add(previousButton, BorderLayout.WEST);
        navigation.add(monthLabel, BorderLayout.CENTER);
        navigation.add(nextButton, BorderLayout.EAST);

        JLabel summaryLabel = new JLabel("", SwingConstants.CENTER);
        summaryLabel.setFont(new Font("Arial", Font.BOLD, 14));

        JTextArea categoryArea = new JTextArea(5, 25);
        categoryArea.setEditable(false);
        categoryArea.setFont(new Font("Monospaced", Font.PLAIN, 13));

        JPanel categoryPanel = new JPanel(new BorderLayout());
        categoryPanel.setBorder(
            BorderFactory.createTitledBorder("Category Performance")
        );
        categoryPanel.add(new JScrollPane(categoryArea), BorderLayout.CENTER);

        JPanel chart = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
                );

                int width = getWidth();
                int height = getHeight();

                YearMonth month = selectedMonth[0];
                int days = month.lengthOfMonth();
                int[] counts = new int[days];
                int maxCount = 1;

                for (String[] record : historyRecords) {
                    if (record.length < 4) {
                        continue;
                    }

                    try {
                        LocalDate date = LocalDate.parse(record[2]);

                        if (YearMonth.from(date).equals(month)) {
                            counts[date.getDayOfMonth() - 1]++;
                            maxCount = Math.max(
                                maxCount,
                                counts[date.getDayOfMonth() - 1]
                            );
                        }
                    } catch (Exception ignored) {
                        // Ignore invalid dates.
                    }
                }

                g2.setColor(new Color(71, 85, 105));
                g2.setFont(new Font("Arial", Font.BOLD, 13));
                g2.drawString("Daily Habit Completions", 15, 22);

                int left = 35;
                int right = 15;
                int top = 40;
                int bottom = 35;

                int chartWidth = Math.max(1, width - left - right);
                int chartHeight = Math.max(1, height - top - bottom);
                double slotWidth = chartWidth / (double) days;
                int barWidth = Math.max(
                    3, (int) (slotWidth * 0.65)
                );

                g2.setColor(new Color(226, 232, 240));
                g2.drawLine(
                    left, top + chartHeight,
                    width - right, top + chartHeight
                );

                for (int i = 0; i < days; i++) {
                    int barHeight = (int) (
                        counts[i] / (double) maxCount * (chartHeight - 10)
                    );

                    int x = left + (int) (i * slotWidth)
                        + (int) ((slotWidth - barWidth) / 2);

                    int y = top + chartHeight - barHeight;

                    g2.setColor(counts[i] > 0
                        ? new Color(14, 165, 233)
                        : new Color(203, 213, 225));

                    g2.fillRoundRect(
                        x, y, barWidth, Math.max(2, barHeight), 5, 5
                    );

                    if (i % 5 == 0 || i == days - 1) {
                        g2.setColor(new Color(71, 85, 105));
                        g2.setFont(new Font("Arial", Font.PLAIN, 10));
                        g2.drawString(
                            String.valueOf(i + 1),
                            x, top + chartHeight + 16
                        );
                    }
                }

                g2.dispose();
            }
        };

        chart.setBackground(Color.WHITE);
        chart.setPreferredSize(new Dimension(650, 240));

        JPanel chartPanel = new JPanel(new BorderLayout());
        chartPanel.setBorder(
            BorderFactory.createLineBorder(new Color(226, 232, 240))
        );
        chartPanel.add(chart, BorderLayout.CENTER);

        JPanel topPanel = new JPanel(new BorderLayout(5, 10));
        topPanel.add(navigation, BorderLayout.NORTH);
        topPanel.add(summaryLabel, BorderLayout.SOUTH);

        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(chartPanel, BorderLayout.CENTER);
        panel.add(categoryPanel, BorderLayout.SOUTH);

        Runnable updateAnalytics = () -> {
            YearMonth month = selectedMonth[0];

            monthLabel.setText(
                month.getMonth().toString() + " " + month.getYear()
            );

            int completions = 0;
            int xpEarned = 0;
            Set<String> activeDays = new HashSet<>();
            Map<String, Integer> categoryCounts = new LinkedHashMap<>();

            for (String[] record : historyRecords) {
                if (record.length < 4) {
                    continue;
                }

                try {
                    LocalDate date = LocalDate.parse(record[2]);

                    if (YearMonth.from(date).equals(month)) {
                        completions++;
                        xpEarned += 10;
                        activeDays.add(record[2]);

                        categoryCounts.put(
                            record[1],
                            categoryCounts.getOrDefault(record[1], 0) + 1
                        );
                    }
                } catch (Exception ignored) {
                    // Ignore invalid dates.
                }
            }

            int daysConsidered;

            if (month.equals(YearMonth.now())) {
                daysConsidered = LocalDate.now().getDayOfMonth();
            } else if (month.isAfter(YearMonth.now())) {
                daysConsidered = 0;
            } else {
                daysConsidered = month.lengthOfMonth();
            }

            int possibleCompletions = habits.size() * daysConsidered;

            int percentage = possibleCompletions == 0
                ? 0
                : Math.min(
                    100, completions * 100 / possibleCompletions
                );

            summaryLabel.setText(
                "<html><div style='text-align:center;'>"
                + "Completions: " + completions
                + " &nbsp; | &nbsp; XP Earned: " + xpEarned
                + " &nbsp; | &nbsp; Active Days: " + activeDays.size()
                + "<br>Estimated Completion Rate: " + percentage + "%"
                + "</div></html>"
            );

            StringBuilder breakdown = new StringBuilder();
            breakdown.append("Monthly category totals\n\n");

            for (String category : categories) {
                breakdown.append(String.format(
                    "%-12s : %d%n",
                    category,
                    categoryCounts.getOrDefault(category, 0)
                ));
            }

            categoryArea.setText(breakdown.toString());
            chart.repaint();
        };

        previousButton.addActionListener(e -> {
            selectedMonth[0] = selectedMonth[0].minusMonths(1);
            updateAnalytics.run();
        });

        nextButton.addActionListener(e -> {
            selectedMonth[0] = selectedMonth[0].plusMonths(1);
            updateAnalytics.run();
        });

        updateAnalytics.run();

        JOptionPane.showMessageDialog(
            this,
            panel,
            "Monthly Progress Analytics",
            JOptionPane.PLAIN_MESSAGE
        );
    }

    private void showAchievements() {
        StringBuilder achievements = new StringBuilder();

        achievements.append("YOUR ACHIEVEMENTS\n\n");
        achievements.append(totalXP >= 10
            ? "[UNLOCKED] First 10 XP\n"
            : "[LOCKED] First 10 XP\n");

        achievements.append(totalXP >= 100
            ? "[UNLOCKED] 100 XP Club\n"
            : "[LOCKED] 100 XP Club\n");

        achievements.append(dailyStreak >= 3
            ? "[UNLOCKED] 3-Day Streak\n"
            : "[LOCKED] 3-Day Streak\n");

        achievements.append(dailyStreak >= 7
            ? "[UNLOCKED] 7-Day Streak\n"
            : "[LOCKED] 7-Day Streak\n");

        achievements.append(habits.size() >= 5
            ? "[UNLOCKED] Habit Collector\n"
            : "[LOCKED] Habit Collector\n");

        JOptionPane.showMessageDialog(
            this, achievements.toString(), "Achievements",
            JOptionPane.INFORMATION_MESSAGE
        );
    }


    // ---------------- MIDNIGHT TEAL PREMIUM THEME ----------------
    private static final Color BG = new Color(11, 25, 46);
    private static final Color SURFACE = new Color(20, 39, 64);
    private static final Color SURFACE_ALT = new Color(29, 55, 82);
    private static final Color PURPLE = new Color(45, 212, 191);
    private static final Color TEXT = new Color(237, 247, 255);

    private void applyDarkPremiumTheme(Component component) {
        if (component instanceof JPanel) {
            component.setBackground("appHeader".equals(component.getName()) ? BG : SURFACE);
            component.setForeground(TEXT);
        }
        if (component == getContentPane()) component.setBackground(BG);
        if (component instanceof JLabel) component.setForeground(TEXT);
        if (component instanceof JTextField || component instanceof JTextArea) {
            component.setBackground(SURFACE_ALT);
            component.setForeground(TEXT);
            if (component instanceof JTextField) ((JTextField) component).setCaretColor(TEXT);
            if (component instanceof JTextArea) ((JTextArea) component).setCaretColor(TEXT);
        }
        if (component instanceof JComboBox) {
            component.setBackground(SURFACE_ALT);
            component.setForeground(TEXT);
        }
        if (component instanceof JTable) {
            JTable t = (JTable) component;
            t.setBackground(SURFACE_ALT);
            t.setForeground(TEXT);
            t.setGridColor(new Color(55, 78, 105));
            t.setSelectionBackground(new Color(13, 148, 136));
            t.setSelectionForeground(Color.WHITE);
            t.getTableHeader().setBackground(new Color(16, 75, 91));
            t.getTableHeader().setForeground(Color.WHITE);
        }
        if (component instanceof JProgressBar) {
            ((JProgressBar) component).setBackground(SURFACE_ALT);
            ((JProgressBar) component).setForeground(PURPLE);
            ((JProgressBar) component).setStringPainted(true);
        }
        if (component instanceof JScrollPane) component.setBackground(SURFACE);
        if (component instanceof JButton) {
            JButton b = (JButton) component;
            b.setForeground(Color.WHITE);
            b.setFocusPainted(false);
            b.setBorderPainted(false);
            b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        }
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                applyDarkPremiumTheme(child);
            }
        }
    }

    private void showPersonalProfile() {
        // Keep the colour change limited to the My Profile window.
        Color profileText = new Color(15, 23, 42);       // dark navy text
        Color profileBackground = new Color(248, 250, 252);
        Color fieldBackground = Color.WHITE;
        Color fieldBorder = new Color(100, 116, 139);

        JPanel panel = new JPanel(new GridLayout(0, 2, 10, 10));
        panel.setBackground(profileBackground);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        JTextField[] fields = new JTextField[personalFieldNames.length];
        for (int i = 0; i < personalFieldNames.length; i++) {
            JLabel label = new JLabel(personalFieldNames[i] + ":");
            label.setForeground(profileText);
            label.setFont(new Font("Segoe UI", Font.BOLD, 13));
            panel.add(label);

            fields[i] = new JTextField(personalDetails[i] == null ? "" : personalDetails[i], 18);
            fields[i].setForeground(profileText);
            fields[i].setCaretColor(profileText);
            fields[i].setBackground(fieldBackground);
            fields[i].setFont(new Font("Segoe UI", Font.PLAIN, 13));
            fields[i].setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(fieldBorder),
                BorderFactory.createEmptyBorder(5, 7, 5, 7)
            ));
            panel.add(fields[i]);
        }
        JScrollPane scroll = new JScrollPane(panel);
        scroll.getViewport().setBackground(profileBackground);
        scroll.setPreferredSize(new Dimension(470, 320));
        int result = JOptionPane.showConfirmDialog(
            this, scroll, "My Profile - Personal Details",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE
        );
        if (result == JOptionPane.OK_OPTION) {
            for (int i = 0; i < fields.length; i++) personalDetails[i] = fields[i].getText().trim();
            savePersonalDetails();
            showMessage("Your profile details have been saved on this computer.");
        }
    }

    private static String safeFileName(String username) {
        String safe = username.replaceAll("[^A-Za-z0-9._-]", "_");
        return safe.isEmpty() ? "user" : safe;
    }

    private void showAllProfiles() {
        File usersFile = new File("data/users.txt");
        DefaultTableModel model = new DefaultTableModel(
            new Object[]{"Username", "Full Name", "Email", "Phone", "City", "State"}, 0) {
                @Override public boolean isCellEditable(int row, int column) { return false; }
            };
        if (usersFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(usersFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] account = line.split("\\|", 2);
                    if (account.length == 0 || account[0].trim().isEmpty()) continue;
                    String username = account[0].trim();
                    String[] details = readProfileFor(username);
                    model.addRow(new Object[]{username, details[0], details[2], details[3], details[5], details[6]});
                }
            } catch (IOException ex) {
                showMessage("Could not read registered users: " + ex.getMessage());
                return;
            }
        }
        JTable table = new JTable(model);
        table.setRowHeight(26);
        table.setAutoCreateRowSorter(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JLabel instruction = new JLabel("Select a user and click Open Profile (or double-click a row).");
        panel.add(instruction, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        JButton openProfileButton = new JButton("Open Profile");
        openProfileButton.addActionListener(e -> openSelectedUserProfile(table));
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(openProfileButton);
        panel.add(bottom, BorderLayout.SOUTH);
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) openSelectedUserProfile(table);
            }
        });

        panel.setPreferredSize(new Dimension(760, 380));
        JOptionPane.showMessageDialog(this, panel, "More Profiles / All Users", JOptionPane.PLAIN_MESSAGE);
    }

    private void openSelectedUserProfile(JTable table) {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) {
            showMessage("Please select a user first.");
            return;
        }
        int modelRow = table.convertRowIndexToModel(selectedRow);
        String username = String.valueOf(table.getModel().getValueAt(modelRow, 0));
        String[] details = readProfileFor(username);

        JPanel detailsPanel = new JPanel(new GridBagLayout());
        detailsPanel.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 6, 5, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        for (int i = 0; i < personalFieldNames.length; i++) {
            gbc.gridx = 0; gbc.gridy = i; gbc.weightx = 0;
            JLabel label = new JLabel(personalFieldNames[i] + ":");
            label.setFont(label.getFont().deriveFont(Font.BOLD));
            detailsPanel.add(label, gbc);
            gbc.gridx = 1; gbc.weightx = 1;
            String value = details[i] == null || details[i].trim().isEmpty() ? "Not provided" : details[i];
            JTextField field = new JTextField(value, 24);
            field.setEditable(false);
            detailsPanel.add(field, gbc);
        }
        JOptionPane.showMessageDialog(this, new JScrollPane(detailsPanel),
            "Profile Details - " + username, JOptionPane.INFORMATION_MESSAGE);
    }

    private String[] readProfileFor(String username) {
        String[] details = new String[personalFieldNames.length];
        Arrays.fill(details, "");
        File file = new File(DATA_DIR + "/profiles/" + safeFileName(username) + ".txt");
        if (!file.exists()) return details;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            for (int i = 0; i < details.length; i++) {
                String line = reader.readLine();
                details[i] = line == null ? "" : line;
            }
        } catch (IOException ignored) { }
        return details;
    }

    private void loadPersonalDetails() {
        File file = new File(PERSONAL_FILE);
        // Migrate the old shared profile for the first run under the current login.
        File oldFile = new File(DATA_DIR + "/personal.txt");
        if (!file.exists() && oldFile.exists()) {
            try {
                File parent = file.getParentFile();
                if (parent != null) parent.mkdirs();
                try (BufferedReader oldReader = new BufferedReader(new FileReader(oldFile));
                     PrintWriter newWriter = new PrintWriter(new FileWriter(file))) {
                    String line; while ((line = oldReader.readLine()) != null) newWriter.println(line);
                }
                oldFile.renameTo(new File(DATA_DIR + "/personal.txt.migrated"));
            } catch (IOException ignored) { }
        }
        if (!file.exists()) return;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            for (int i = 0; i < personalDetails.length; i++) {
                personalDetails[i] = reader.readLine();
                if (personalDetails[i] == null) personalDetails[i] = "";
            }
        } catch (IOException ignored) { }
    }

    private void savePersonalDetails() {
        File parent = new File(PERSONAL_FILE).getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();
        try (PrintWriter writer = new PrintWriter(new FileWriter(PERSONAL_FILE))) {
            for (String detail : personalDetails) {
                writer.println(detail == null ? "" : detail.replace("|", "/"));
            }
        } catch (IOException e) {
            showMessage("Could not save personal profile: " + e.getMessage());
        }
    }

    private boolean validReminderTime(String value) {
        try {
            LocalTime.parse(value, DateTimeFormatter.ofPattern("HH:mm"));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void startReminderTimer() {
        reminderTimer = new javax.swing.Timer(15000, e -> checkReminders());
        reminderTimer.start();
    }

    private void checkReminders() {
        String now = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        String today = LocalDate.now().toString();
        for (String[] habit : habits) {
            if (habit.length > 6 && Boolean.parseBoolean(habit[6])
                    && now.equals(habit[5]) && !today.equals(habit[4])) {
                String key = today + "|" + habit[0] + "|" + habit[5];
                if (reminderShown.add(key)) {
                    JOptionPane.showMessageDialog(this,
                        "It's time for your habit:\n\n" + habit[0],
                        "Daily Habit Flow Reminder", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        }
    }

    private void showReminderOverview() {
        StringBuilder text = new StringBuilder("YOUR DAILY REMINDERS\n\n");
        boolean found = false;
        for (String[] habit : habits) {
            if (habit.length > 6 && Boolean.parseBoolean(habit[6])) {
                text.append(habit[0]).append(" — ").append(habit[5]).append("\n");
                found = true;
            }
        }
        if (!found) text.append("No reminders set yet.\nEdit or add a habit to enable one.");
        text.append("\n\nReminder popups work while the app is running.");
        showMessage(text.toString());
    }

    private void loadHabits() {
        File file = new File(HABITS_FILE);

        if (!file.exists()) {
            return;
        }

        try (BufferedReader reader =
                 new BufferedReader(new FileReader(file))) {

            String line;

            while ((line = reader.readLine()) != null) {
                String[] fields = line.split("\\|", -1);

                if (fields.length >= 5) {
                    String[] habit = Arrays.copyOf(fields, 7);
                    for (int i = 0; i < habit.length; i++) {
                        if (habit[i] == null) habit[i] = "";
                    }
                    if (habit[6].isEmpty()) habit[6] = "false";
                    habits.add(habit);
                }
            }
        } catch (IOException e) {
            showMessage("Could not load habits: " + e.getMessage());
        }
    }

    private void saveHabits() {
        try (PrintWriter writer =
                 new PrintWriter(new FileWriter(HABITS_FILE))) {

            for (String[] habit : habits) {
                String[] safeHabit = Arrays.copyOf(habit, 7);
                for (int i = 0; i < safeHabit.length; i++) {
                    if (safeHabit[i] == null) safeHabit[i] = "";
                }
                writer.println(String.join("|", safeHabit));
            }
        } catch (IOException e) {
            showMessage("Could not save habits: " + e.getMessage());
        }
    }

    private void loadHistory() {
        File file = new File(HISTORY_FILE);

        if (!file.exists()) {
            return;
        }

        try (BufferedReader reader =
                 new BufferedReader(new FileReader(file))) {

            String line;

            while ((line = reader.readLine()) != null) {
                String[] fields = line.split("\\|", -1);

                if (fields.length >= 4) {
                    historyRecords.add(fields);
                }
            }
        } catch (IOException e) {
            showMessage("Could not load history: " + e.getMessage());
        }
    }

    private void saveHistory() {
        try (PrintWriter writer =
                 new PrintWriter(new FileWriter(HISTORY_FILE))) {

            for (String[] record : historyRecords) {
                writer.println(
                    record[0] + "|" + record[1] + "|"
                    + record[2] + "|" + record[3]
                );
            }
        } catch (IOException e) {
            showMessage("Could not save history: " + e.getMessage());
        }
    }

    private void loadProfile() {
        File file = new File(PROFILE_FILE);

        if (!file.exists()) {
            return;
        }

        try (BufferedReader reader =
                 new BufferedReader(new FileReader(file))) {

            String xpLine = reader.readLine();
            String streakLine = reader.readLine();
            String dateLine = reader.readLine();

            totalXP = xpLine == null ? 0 : Integer.parseInt(xpLine);
            dailyStreak = streakLine == null
                ? 0 : Integer.parseInt(streakLine);
            lastStreakDate = dateLine == null ? "" : dateLine;

        } catch (Exception e) {
            totalXP = 0;
            dailyStreak = 0;
            lastStreakDate = "";
        }
    }

    private void saveProfile() {
        try (PrintWriter writer =
                 new PrintWriter(new FileWriter(PROFILE_FILE))) {

            writer.println(totalXP);
            writer.println(dailyStreak);
            writer.println(lastStreakDate);

        } catch (IOException e) {
            showMessage("Could not save profile: " + e.getMessage());
        }
    }

    private void saveAll() {
        saveHabits();
        saveHistory();
        saveProfile();
    }

    private void showMessage(String message) {
        JOptionPane.showMessageDialog(this, message);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Cross-platform Swing look-and-feel reliably honors our custom button colors.
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception ignored) {
                // Continue with the available look and feel.
            }

            Color buttonPurple = new Color(13, 148, 136);
            Color buttonHoverPurple = new Color(15, 118, 110);
            UIManager.put("OptionPane.background", BG);
            UIManager.put("OptionPane.messageForeground", TEXT);
            UIManager.put("OptionPane.foreground", TEXT);
            UIManager.put("OptionPane.buttonAreaBackground", BG);
            UIManager.put("Panel.background", SURFACE);
            UIManager.put("TextField.background", SURFACE_ALT);
            UIManager.put("TextField.foreground", TEXT);
            UIManager.put("ComboBox.background", SURFACE_ALT);
            UIManager.put("ComboBox.foreground", TEXT);
            UIManager.put("Button.background", buttonPurple);
            UIManager.put("Button.foreground", Color.WHITE);
            UIManager.put("Button.select", buttonHoverPurple);
            UIManager.put("Button.focus", new Color(153, 246, 228));
            UIManager.put("Button.opaque", Boolean.TRUE);
            UIManager.put("Button.contentAreaFilled", Boolean.TRUE);
            UIManager.put("Button.borderPainted", Boolean.TRUE);
            UIManager.put("Button.font", new Font("Arial", Font.BOLD, 13));
            UIManager.put("OptionPane.buttonFont", new Font("Arial", Font.BOLD, 13));
            new File("data").mkdirs();
            new LoginScreen().setVisible(true);
        });
    }

    // Login and registration window. Accounts are stored locally; no MySQL needed.
    private static class LoginScreen extends JFrame {
        private final JTextField usernameField = new JTextField(18);
        private final JPasswordField passwordField = new JPasswordField(18);
        private final JLabel messageLabel = new JLabel("Create an account or sign in", SwingConstants.CENTER);
        private static final String USERS_FILE = "data/users.txt";
        // Compatibility with accounts created by the earlier misspelled filename.
        private static final String LEGACY_USERS_FILE = "data/usersṇ.txt";

        LoginScreen() {
            setTitle("Daily Habit Flow Pro - Login");
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setSize(470, 540);
            setResizable(false);
            setLocationRelativeTo(null);
            migrateLegacyUsersFile();

            JPanel root = new JPanel(new GridBagLayout());
            root.setBackground(BG);
            root.setBorder(BorderFactory.createEmptyBorder(26, 34, 26, 34));
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.weightx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(7, 0, 7, 0);

            JLabel logo = new JLabel("DAILY HABIT FLOW", SwingConstants.CENTER);
            logo.setFont(new Font("Arial", Font.BOLD, 25));
            logo.setForeground(PURPLE);
            c.gridy = 0;
            root.add(logo, c);

            JLabel heading = new JLabel("Welcome", SwingConstants.CENTER);
            heading.setFont(new Font("Arial", Font.BOLD, 21));
            heading.setForeground(TEXT);
            c.gridy = 1;
            root.add(heading, c);

            JLabel subtitle = new JLabel("Sign in or create your own account", SwingConstants.CENTER);
            subtitle.setFont(new Font("Arial", Font.PLAIN, 13));
            subtitle.setForeground(new Color(180, 187, 211));
            c.gridy = 2;
            root.add(subtitle, c);

            JLabel userLabel = new JLabel("Username");
            userLabel.setForeground(TEXT);
            userLabel.setFont(new Font("Arial", Font.BOLD, 13));
            c.gridy = 3;
            root.add(userLabel, c);

            styleLoginField(usernameField);
            c.gridy = 4;
            root.add(usernameField, c);

            JLabel passLabel = new JLabel("Password");
            passLabel.setForeground(TEXT);
            passLabel.setFont(new Font("Arial", Font.BOLD, 13));
            c.gridy = 5;
            root.add(passLabel, c);

            styleLoginField(passwordField);
            c.gridy = 6;
            root.add(passwordField, c);

            messageLabel.setForeground(new Color(196, 181, 253));
            messageLabel.setFont(new Font("Arial", Font.PLAIN, 12));
            c.gridy = 7;
            c.insets = new Insets(6, 0, 6, 0);
            root.add(messageLabel, c);

            JButton loginButton = makeLoginButton("LOGIN");
            c.gridy = 8;
            c.insets = new Insets(7, 0, 6, 0);
            root.add(loginButton, c);

            JButton registerButton = makeLoginButton("CREATE ACCOUNT / SIGN UP");
            registerButton.setBackground(new Color(55, 65, 100));
            registerButton.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                    registerButton.setBackground(new Color(76, 86, 130));
                }
                @Override public void mouseExited(java.awt.event.MouseEvent e) {
                    registerButton.setBackground(new Color(55, 65, 100));
                }
            });
            c.gridy = 9;
            root.add(registerButton, c);

            JLabel hint = new JLabel("New user? Create an account first.", SwingConstants.CENTER);
            hint.setForeground(new Color(180, 187, 211));
            hint.setFont(new Font("Arial", Font.PLAIN, 12));
            c.gridy = 10;
            c.insets = new Insets(7, 0, 0, 0);
            root.add(hint, c);

            loginButton.addActionListener(e -> attemptLogin());
            registerButton.addActionListener(e -> registerAccount());
            passwordField.addActionListener(e -> attemptLogin());
            setContentPane(root);
        }

        private void migrateLegacyUsersFile() {
            File currentFile = new File(USERS_FILE);
            File legacyFile = new File(LEGACY_USERS_FILE);
            File parent = currentFile.getParentFile();

            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            if (!legacyFile.exists()) {
                return;
            }

            // Keep existing accounts and merge accounts from the old filename.
            Set<String> knownUsernames = new HashSet<>();
            try {
                if (currentFile.exists()) {
                    try (BufferedReader reader = new BufferedReader(new FileReader(currentFile))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            String[] parts = line.split("\\|", 2);
                            if (parts.length == 2) knownUsernames.add(parts[0].toLowerCase(java.util.Locale.ROOT));
                        }
                    }
                }

                try (BufferedReader reader = new BufferedReader(new FileReader(legacyFile));
                     PrintWriter writer = new PrintWriter(new FileWriter(currentFile, true))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] parts = line.split("\\|", 2);
                        if (parts.length == 2
                                && knownUsernames.add(parts[0].toLowerCase(java.util.Locale.ROOT))) {
                            writer.println(line);
                        }
                    }
                }
            } catch (IOException ex) {
                // Keep the legacy file untouched; login will show a useful message if needed.
                System.err.println("Could not migrate old account file: " + ex.getMessage());
            }
        }

        private JButton makeLoginButton(String text) {
            JButton button = new JButton(text);
            button.setFont(new Font("Arial", Font.BOLD, 13));
            button.setForeground(Color.WHITE);
            button.setBackground(new Color(124, 58, 237));
            button.setOpaque(true);
            button.setContentAreaFilled(true);
            button.setBorderPainted(false);
            button.setFocusPainted(false);
            button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(167, 139, 250), 1),
                BorderFactory.createEmptyBorder(11, 12, 11, 12)
            ));
            button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            button.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                    if (button.getText().startsWith("LOGIN")) button.setBackground(new Color(139, 92, 246));
                }
                @Override public void mouseExited(java.awt.event.MouseEvent e) {
                    if (button.getText().startsWith("LOGIN")) button.setBackground(new Color(124, 58, 237));
                }
            });
            return button;
        }

        private void styleLoginField(JTextField field) {
            field.setFont(new Font("Arial", Font.PLAIN, 15));
            field.setForeground(TEXT);
            field.setBackground(SURFACE_ALT);
            field.setCaretColor(TEXT);
            field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(78, 83, 120)),
                BorderFactory.createEmptyBorder(10, 11, 10, 11)
            ));
        }

        private void registerAccount() {
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword());
            if (username.isEmpty() || password.isEmpty()) {
                messageLabel.setForeground(new Color(248, 113, 113));
                messageLabel.setText("Enter a username and password first.");
                return;
            }
            if (username.contains("|") || username.contains("\n") || username.contains("\r")) {
                messageLabel.setForeground(new Color(248, 113, 113));
                messageLabel.setText("Username cannot contain | or a line break.");
                return;
            }
            if (password.length() < 4) {
                messageLabel.setForeground(new Color(248, 113, 113));
                messageLabel.setText("Password must be at least 4 characters.");
                return;
            }
            File file = new File(USERS_FILE);
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            try {
                if (file.exists()) {
                    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            String[] parts = line.split("\\|", 2);
                            if (parts.length == 2 && parts[0].equalsIgnoreCase(username)) {
                                messageLabel.setForeground(new Color(248, 113, 113));
                                messageLabel.setText("Username already exists. Choose another.");
                                return;
                            }
                        }
                    }
                }
                try (PrintWriter writer = new PrintWriter(new FileWriter(file, true))) {
                    writer.println(username + "|" + hashPassword(password));
                }
                messageLabel.setForeground(new Color(74, 222, 128));
                messageLabel.setText("Account created! Now click LOGIN.");
                passwordField.setText("");
            } catch (IOException ex) {
                messageLabel.setForeground(new Color(248, 113, 113));
                messageLabel.setText("Could not save account. Check folder permissions.");
            }
        }

        private void attemptLogin() {
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword());
            if (username.isEmpty() || password.isEmpty()) {
                messageLabel.setForeground(new Color(248, 113, 113));
                messageLabel.setText("Enter your username and password.");
                return;
            }
            File file = new File(USERS_FILE);
            if (!file.exists()) {
                messageLabel.setForeground(new Color(248, 113, 113));
                messageLabel.setText("No account yet. Click CREATE ACCOUNT first.");
                return;
            }
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                String passwordHash = hashPassword(password);
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split("\\|", 2);
                    if (parts.length == 2 && parts[0].equalsIgnoreCase(username)
                            && parts[1].equals(passwordHash)) {
                        dispose();
                        main dashboard = new main(username);
                        dashboard.setVisible(true);
                        return;
                    }
                }
                messageLabel.setForeground(new Color(248, 113, 113));
                messageLabel.setText("Incorrect details. Try again or create an account.");
                passwordField.setText("");
                passwordField.requestFocusInWindow();
            } catch (IOException ex) {
                messageLabel.setForeground(new Color(248, 113, 113));
                messageLabel.setText("Could not read account data.");
            }
        }

        private String hashPassword(String password) {
            try {
                java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
                byte[] bytes = digest.digest(password.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                StringBuilder result = new StringBuilder();
                for (byte b : bytes) result.append(String.format("%02x", b));
                return result.toString();
            } catch (java.security.NoSuchAlgorithmException ex) {
                throw new IllegalStateException("SHA-256 is not available", ex);
            }
        }
    }

}
