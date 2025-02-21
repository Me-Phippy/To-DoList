import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

/**
 * A comprehensive To-Do List management application with file attachment support
 * and integrated user management capabilities.
 */
public class TodoListManager extends JFrame {
    // UI Components
    private final JTable taskTable;
    private final DefaultTableModel tableModel;
    private final JTextField taskNameField;
    private final JTextArea descriptionArea;
    private final JComboBox<String> userComboBox;
    private final JLabel attachmentLabel;
    private final JButton addButton, deleteButton, attachButton, openAttachmentButton;

    // Data structures
    private final Map<Integer, Task> taskMap = new HashMap<>();
    private File selectedAttachment = null;
    private final String attachmentDir = "attachments";
    private int currentTaskId = 0;

    /**
     * Constructs the TodoListManager application
     */
    public TodoListManager() {
        setTitle("To-Do List Manager");
        setSize(850, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Initialize directories
        createAttachmentDirectory();

        // Initialize table model with columns
        tableModel = new DefaultTableModel(
                new Object[]{"ID", "Task", "User", "Created", "Status", "Attachment"}, 0);

        // Configure task table
        taskTable = new JTable(tableModel);
        taskTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        taskTable.getColumnModel().getColumn(0).setMaxWidth(40);     // ID column
        taskTable.getColumnModel().getColumn(3).setPreferredWidth(120); // Date column
        taskTable.getColumnModel().getColumn(4).setMaxWidth(70);     // Status column
        taskTable.getColumnModel().getColumn(5).setMaxWidth(80);     // Attachment column

        // Initialize form components
        taskNameField = new JTextField(20);
        descriptionArea = new JTextArea(5, 20);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);

        // Get users from UserManagement if available, otherwise use defaults
        userComboBox = new JComboBox<>(getUserList());

        // Initialize buttons
        addButton = new JButton("Add Task");
        deleteButton = new JButton("Delete");
        attachButton = new JButton("Attach");
        openAttachmentButton = new JButton("Open Attachment");
        openAttachmentButton.setEnabled(false);

        attachmentLabel = new JLabel("No file attached");

        // Configure main layout
        JPanel mainPanel = setupMainPanel();
        add(mainPanel);

        // Set up menu bar
        setJMenuBar(createMenuBar());

        // Add event listeners
        setupEventListeners();

        // Load existing tasks if available
        loadTasks();
    }

    /**
     * Creates the main panel with proper layout and components
     * @return The configured main panel
     */
    private JPanel setupMainPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));


        JButton taskManagerButton = new JButton("Switch");
        taskManagerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Close the current UserManagement window
                dispose();

                // Open the TodoListManager
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        new TodoListManager().setVisible(true);
                    }
                });
            }
        });

        // Task list panel (Center)
        JScrollPane tableScrollPane = new JScrollPane(taskTable);
        mainPanel.add(tableScrollPane, BorderLayout.CENTER);

        // Task details panel (East)
        JPanel detailsPanel = new JPanel(new BorderLayout(5, 5));
        detailsPanel.setPreferredSize(new Dimension(280, getHeight()));

        // Create input panel with vertical box layout
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.Y_AXIS));
        inputPanel.setBorder(BorderFactory.createTitledBorder("Task Details"));

        // Task name field
        JPanel namePanel = new JPanel(new BorderLayout(5, 5));
        namePanel.add(new JLabel("Task Name:"), BorderLayout.NORTH);
        namePanel.add(taskNameField, BorderLayout.CENTER);

        // Description field
        JPanel descPanel = new JPanel(new BorderLayout(5, 5));
        descPanel.add(new JLabel("Description:"), BorderLayout.NORTH);
        descPanel.add(new JScrollPane(descriptionArea), BorderLayout.CENTER);

        // User selection
        JPanel userPanel = new JPanel(new BorderLayout(5, 5));
        userPanel.add(new JLabel("Assigned to:"), BorderLayout.NORTH);
        userPanel.add(userComboBox, BorderLayout.CENTER);

        // Attachment section
        JPanel attachPanel = new JPanel(new BorderLayout(5, 5));
        attachPanel.add(new JLabel("Attachment:"), BorderLayout.NORTH);
        JPanel attachmentPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        attachmentPanel.add(attachmentLabel);
        attachPanel.add(attachmentPanel, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        buttonPanel.add(addButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(attachButton);
        buttonPanel.add(openAttachmentButton);
        buttonPanel.add(taskManagerButton);

        // Add all components to input panel with spacing
        inputPanel.add(namePanel);
        inputPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        inputPanel.add(descPanel);
        inputPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        inputPanel.add(userPanel);
        inputPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        inputPanel.add(attachPanel);
        inputPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        inputPanel.add(buttonPanel);

        detailsPanel.add(inputPanel, BorderLayout.CENTER);
        mainPanel.add(detailsPanel, BorderLayout.EAST);

        return mainPanel;
    }

    /**
     * Creates the application menu bar
     * @return The configured JMenuBar
     */
    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // File menu
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);

        JMenuItem saveItem = new JMenuItem("Save Tasks", KeyEvent.VK_S);
        saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
        saveItem.addActionListener(e -> saveTasks());

        JMenuItem loadItem = new JMenuItem("Load Tasks", KeyEvent.VK_L);
        loadItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_DOWN_MASK));
        loadItem.addActionListener(e -> loadTasks());

        JMenuItem exitItem = new JMenuItem("Exit", KeyEvent.VK_X);
        exitItem.addActionListener(e -> System.exit(0));

        fileMenu.add(saveItem);
        fileMenu.add(loadItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        // User menu
        JMenu userMenu = new JMenu("Users");
        userMenu.setMnemonic(KeyEvent.VK_U);

        JMenuItem manageUsersItem = new JMenuItem("Manage Users", KeyEvent.VK_M);
        manageUsersItem.addActionListener(e -> {
            UserManagement.showUserManagement();
            // Refresh user list when user management closes
            SwingUtilities.invokeLater(this::refreshUserComboBox);
        });

        userMenu.add(manageUsersItem);

        // Help menu
        JMenu helpMenu = new JMenu("Help");
        helpMenu.setMnemonic(KeyEvent.VK_H);

        JMenuItem aboutItem = new JMenuItem("About", KeyEvent.VK_A);
        aboutItem.addActionListener(e ->
                JOptionPane.showMessageDialog(this,
                        "To-Do List Manager v1.0\n" +
                                "A task management tool with file attachment capabilities",
                        "About", JOptionPane.INFORMATION_MESSAGE)
        );

        helpMenu.add(aboutItem);

        // Add all menus to menu bar
        menuBar.add(fileMenu);
        menuBar.add(userMenu);
        menuBar.add(helpMenu);

        return menuBar;
    }

    /**
     * Sets up all event listeners for UI components
     */
    private void setupEventListeners() {
        // Add task button
        addButton.addActionListener(e -> addTask());

        // Delete task button
        deleteButton.addActionListener(e -> deleteTask());

        // Attach file button
        attachButton.addActionListener(e -> attachFile());

        // Open attachment button
        openAttachmentButton.addActionListener(e -> openAttachment());

        // Table selection listener
        taskTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                displaySelectedTask();
            }
        });

        // Double-click on table row to open attachment
        taskTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    openAttachment();
                }
            }
        });
    }

    /**
     * Creates the directory for storing attachments
     */
    private void createAttachmentDirectory() {
        File dir = new File(attachmentDir);
        if (!dir.exists()) {
            dir.mkdir();
        }
    }

    /**
     * Gets list of users from UserManagement if available, otherwise uses defaults
     * @return Array of user names
     */
    private String[] getUserList() {
        try {
            File file = new File("users.dat");
            if (file.exists()) {
                try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                    @SuppressWarnings("unchecked")
                    List<UserManagement.User> users = (List<UserManagement.User>) ois.readObject();
                    return users.stream()
                            .map(user -> user.getPrename() + " " + user.getName())
                            .toArray(String[]::new);
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading users: " + e.getMessage());
        }

        // Default users if file not found
        return new String[]{"John", "Alice", "Bob", "Team"};
    }

    /**
     * Refreshes the user dropdown with the latest user list
     */
    private void refreshUserComboBox() {
        String selectedUser = (String) userComboBox.getSelectedItem();
        userComboBox.setModel(new DefaultComboBoxModel<>(getUserList()));
        if (selectedUser != null) {
            userComboBox.setSelectedItem(selectedUser);
        }
    }

    /**
     * Adds a new task based on form input
     */
    private void addTask() {
        String taskName = taskNameField.getText().trim();
        if (taskName.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Task name cannot be empty", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String description = descriptionArea.getText().trim();
        String user = (String) userComboBox.getSelectedItem();
        LocalDateTime now = LocalDateTime.now();
        String timestamp = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        int taskId = currentTaskId++;
        Task task = new Task(taskId, taskName, description, user, now, "New", null);

        // Process attachment if selected
        if (selectedAttachment != null) {
            try {
                String fileName = taskId + "_" + selectedAttachment.getName();
                Path destPath = Paths.get(attachmentDir, fileName);
                Files.copy(selectedAttachment.toPath(), destPath, StandardCopyOption.REPLACE_EXISTING);
                task.setAttachmentPath(destPath.toString());
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this,
                        "Error saving attachment: " + ex.getMessage(),
                        "File Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        // Add to data model and update UI
        taskMap.put(taskId, task);
        tableModel.addRow(new Object[]{
                taskId,
                taskName,
                user,
                timestamp,
                "New",
                task.getAttachmentPath() != null ? "Yes" : "No"
        });

        clearInputFields();
        saveTasks(); // Auto-save after adding
    }

    /**
     * Deletes the currently selected task
     */
    private void deleteTask() {
        int selectedRow = taskTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this,
                    "Please select a task to delete",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int taskId = (Integer) tableModel.getValueAt(selectedRow, 0);
        Task task = taskMap.get(taskId);

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete task '" + task.getName() + "'?",
                "Confirm Deletion", JOptionPane.YES_NO_OPTION);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        // Delete attachment if exists
        if (task.getAttachmentPath() != null) {
            File attachment = new File(task.getAttachmentPath());
            if (attachment.exists()) {
                attachment.delete();
            }
        }

        taskMap.remove(taskId);
        tableModel.removeRow(selectedRow);
        clearInputFields();
        saveTasks(); // Auto-save after deleting
    }

    /**
     * Opens file chooser to attach a file to current task
     */
    private void attachFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select File to Attach");

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            selectedAttachment = fileChooser.getSelectedFile();
            attachmentLabel.setText(selectedAttachment.getName());
        }
    }

    /**
     * Opens the attachment of the selected task
     */
    private void openAttachment() {
        int selectedRow = taskTable.getSelectedRow();
        if (selectedRow < 0) {
            return;
        }

        int taskId = (Integer) tableModel.getValueAt(selectedRow, 0);
        Task task = taskMap.get(taskId);

        if (task.getAttachmentPath() == null) {
            JOptionPane.showMessageDialog(this,
                    "This task has no attachment",
                    "No Attachment", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        try {
            File file = new File(task.getAttachmentPath());
            if (file.exists()) {
                Desktop.getDesktop().open(file);
            } else {
                JOptionPane.showMessageDialog(this,
                        "Attachment file not found at: " + task.getAttachmentPath(),
                        "File Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Error opening file: " + ex.getMessage(),
                    "Open Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Displays details of the selected task in the form
     */
    private void displaySelectedTask() {
        int selectedRow = taskTable.getSelectedRow();
        if (selectedRow < 0) {
            clearInputFields();
            return;
        }

        int taskId = (Integer) tableModel.getValueAt(selectedRow, 0);
        Task task = taskMap.get(taskId);

        taskNameField.setText(task.getName());
        descriptionArea.setText(task.getDescription());
        userComboBox.setSelectedItem(task.getAssignedUser());

        if (task.getAttachmentPath() != null) {
            File file = new File(task.getAttachmentPath());
            attachmentLabel.setText(file.getName());
            openAttachmentButton.setEnabled(true);
        } else {
            attachmentLabel.setText("No file attached");
            openAttachmentButton.setEnabled(false);
        }

        selectedAttachment = null;
    }

    /**
     * Clears all input fields in the form
     */
    private void clearInputFields() {
        taskNameField.setText("");
        descriptionArea.setText("");
        userComboBox.setSelectedIndex(0);
        attachmentLabel.setText("No file attached");
        selectedAttachment = null;
        openAttachmentButton.setEnabled(false);
    }

    /**
     * Saves tasks to a file
     */
    private void saveTasks() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("tasks.dat"))) {
            oos.writeObject(taskMap);
            oos.writeInt(currentTaskId); // Save the current ID counter
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "Error saving tasks: " + e.getMessage(),
                    "Save Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Loads tasks from a file
     */
    @SuppressWarnings("unchecked")
    private void loadTasks() {
        File file = new File("tasks.dat");
        if (!file.exists()) return;

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            taskMap.clear();
            taskMap.putAll((Map<Integer, Task>) ois.readObject());
            currentTaskId = ois.readInt(); // Restore the ID counter

            // Update table UI
            tableModel.setRowCount(0);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

            for (Task task : taskMap.values()) {
                tableModel.addRow(new Object[]{
                        task.getId(),
                        task.getName(),
                        task.getAssignedUser(),
                        task.getCreatedDateTime().format(formatter),
                        task.getStatus(),
                        task.getAttachmentPath() != null ? "Yes" : "No"
                });
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Error loading tasks: " + e.getMessage(),
                    "Load Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Task class to hold task data
     */
    private static class Task implements Serializable {
        private static final long serialVersionUID = 1L;

        private final int id;
        private final String name;
        private final String description;
        private final String assignedUser;
        private final LocalDateTime createdDateTime;
        private String status;
        private String attachmentPath;

        public Task(int id, String name, String description, String assignedUser,
                    LocalDateTime createdDateTime, String status, String attachmentPath) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.assignedUser = assignedUser;
            this.createdDateTime = createdDateTime;
            this.status = status;
            this.attachmentPath = attachmentPath;
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public String getAssignedUser() {
            return assignedUser;
        }

        public LocalDateTime getCreatedDateTime() {
            return createdDateTime;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getAttachmentPath() {
            return attachmentPath;
        }

        public void setAttachmentPath(String attachmentPath) {
            this.attachmentPath = attachmentPath;
        }
    }

    /**
     * Application entry point
     */
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            new TodoListManager().setVisible(true);
        });
    }
}