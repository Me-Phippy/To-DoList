import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

public class UserManagement extends JFrame {
    private final JTextField prenameField;
    private final JTextField nameField;
    private final JComboBox<String> genderComboBox;
    private final JFormattedTextField birthdateField;
    private final JTextField creditCardField;
    private final JTable userTable;
    private final DefaultTableModel tableModel;
    private final List<User> users = new ArrayList<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    // Credit card validation pattern (basic check for 16 digits)
    private final Pattern creditCardPattern = Pattern.compile("\\d{16}");

    public UserManagement() {
        setTitle("User Management");
        setSize(700, 500);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        // Main panel with border layout
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Create form components
        prenameField = new JTextField(20);
        nameField = new JTextField(20);
        String[] genderOptions = {"Woman", "Man", "Insane"};
        genderComboBox = new JComboBox<>(genderOptions);
        birthdateField = new JFormattedTextField(dateFormat);
        birthdateField.setColumns(10);
        birthdateField.setToolTipText("Format: YYYY-MM-DD");
        creditCardField = new JTextField(20);

        // Create the user table
        tableModel = new DefaultTableModel(
                new Object[]{"Prename", "Name", "Gender", "Birthdate", "Credit Card"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table non-editable
            }
        };
        userTable = new JTable(tableModel);


        // Form panel (West)
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBorder(BorderFactory.createTitledBorder("New User"));

        // Create form components arranged in panels
        addFormField(formPanel, "Prename:", prenameField);
        addFormField(formPanel, "Name:", nameField);
        addFormField(formPanel, "Gender:", genderComboBox);
        addFormField(formPanel, "Birthdate (YYYY-MM-DD):", birthdateField);
        addFormField(formPanel, "Credit Card Number:", creditCardField);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton addButton = new JButton("Add User");
        JButton clearButton = new JButton("Clear Form");
        JButton deleteButton = new JButton("Delete Selected");
        buttonPanel.add(addButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(deleteButton);
        formPanel.add(buttonPanel);

        // Add actions to buttons
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addUser();
            }
        });

        clearButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clearForm();
            }
        });

        deleteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deleteSelectedUser();
            }
        });

        // Add components to main panel
        mainPanel.add(formPanel, BorderLayout.WEST);
        mainPanel.add(new JScrollPane(userTable), BorderLayout.CENTER);

        // Add menu bar
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenuItem saveItem = new JMenuItem("Save Users");
        JMenuItem loadItem = new JMenuItem("Load Users");
        JMenuItem exitItem = new JMenuItem("Exit");

        saveItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveUsers();
            }
        });

        loadItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loadUsers();
            }
        });

        exitItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        fileMenu.add(saveItem);
        fileMenu.add(loadItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);
        menuBar.add(fileMenu);

        // Set up the frame
        setJMenuBar(menuBar);
        add(mainPanel);

        // Load existing users if file exists
        loadUsers();
    }

    // Helper method to add form fields consistently
    private void addFormField(JPanel panel, String labelText, JComponent field) {
        JPanel fieldPanel = new JPanel(new BorderLayout(5, 2));
        fieldPanel.add(new JLabel(labelText), BorderLayout.NORTH);
        fieldPanel.add(field, BorderLayout.CENTER);
        panel.add(fieldPanel);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
    }

    // Add a new user based on form input
    private void addUser() {
        // Validate fields
        if (!validateFields()) {
            return;
        }

        try {
            String prename = prenameField.getText().trim();
            String name = nameField.getText().trim();
            String gender = (String) genderComboBox.getSelectedItem();
            Date birthdate = dateFormat.parse(birthdateField.getText().trim());
            String creditCard = creditCardField.getText().trim();

            // Create and add user
            User newUser = new User(prename, name, gender, birthdate, creditCard);
            users.add(newUser);

            // Update table
            updateUserTable();

            // Clear form
            clearForm();

            // Save users to file
            saveUsers();

            JOptionPane.showMessageDialog(this,
                    "User " + prename + " " + name + " added successfully.",
                    "Success", JOptionPane.INFORMATION_MESSAGE);

        } catch (ParseException e) {
            JOptionPane.showMessageDialog(this,
                    "Invalid date format. Please use YYYY-MM-DD format.",
                    "Input Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Validate all input fields
    private boolean validateFields() {
        // Check prename
        if (prenameField.getText().trim().isEmpty()) {
            showValidationError("Prename cannot be empty.");
            return false;
        }

        // Check name
        if (nameField.getText().trim().isEmpty()) {
            showValidationError("Name cannot be empty.");
            return false;
        }

        // Check birthdate
        String birthdateStr = birthdateField.getText().trim();
        if (birthdateStr.isEmpty()) {
            showValidationError("Birthdate cannot be empty.");
            return false;
        }

        try {
            Date date = dateFormat.parse(birthdateStr);
            Date currentDate = new Date();
            if (date.after(currentDate)) {
                showValidationError("Birthdate cannot be in the future.");
                return false;
            }
        } catch (ParseException e) {
            showValidationError("Invalid date format. Please use YYYY-MM-DD format.");
            return false;
        }

        // Check credit card
        String creditCard = creditCardField.getText().trim();
        if (creditCard.isEmpty()) {
            showValidationError("Credit card number cannot be empty.");
            return false;
        }

        if (!creditCardPattern.matcher(creditCard).matches()) {
            showValidationError("Credit card must be exactly 16 digits.");
            return false;
        }

        return true;
    }

    private void showValidationError(String message) {
        JOptionPane.showMessageDialog(this, message, "Validation Error", JOptionPane.ERROR_MESSAGE);
    }

    // Clear all form fields
    private void clearForm() {
        prenameField.setText("");
        nameField.setText("");
        genderComboBox.setSelectedIndex(0);
        birthdateField.setText("");
        creditCardField.setText("");
    }

    // Delete the selected user
    private void deleteSelectedUser() {
        int selectedRow = userTable.getSelectedRow();
        if (selectedRow >= 0) {
            User user = users.get(selectedRow);
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to delete user " + user.getPrename() + " " + user.getName() + "?",
                    "Confirm Deletion", JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                users.remove(selectedRow);
                updateUserTable();
                saveUsers();
            }
        } else {
            JOptionPane.showMessageDialog(this, "Please select a user to delete.",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
        }
    }

    // Update the user table with current users
    private void updateUserTable() {
        tableModel.setRowCount(0);
        for (User user : users) {
            tableModel.addRow(new Object[]{
                    user.getPrename(),
                    user.getName(),
                    user.getGender(),
                    dateFormat.format(user.getBirthdate()),
                    maskCreditCard(user.getCreditCard())
            });
        }
    }

    // Mask credit card for display
    private String maskCreditCard(String creditCard) {
        if (creditCard.length() <= 4) return creditCard;
        return "****-****-****-" + creditCard.substring(creditCard.length() - 4);
    }

    // Save users to file
    private void saveUsers() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("users.dat"))) {
            oos.writeObject(users);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "Error saving users: " + e.getMessage(),
                    "Save Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Load users from file
    @SuppressWarnings("unchecked")
    private void loadUsers() {
        File file = new File("users.dat");
        if (!file.exists()) return;

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            users.clear();
            users.addAll((List<User>) ois.readObject());
            updateUserTable();
        } catch (IOException | ClassNotFoundException e) {
            JOptionPane.showMessageDialog(this,
                    "Error loading users: " + e.getMessage(),
                    "Load Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Integration method for TodoListManager
    public static void showUserManagement() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new UserManagement().setVisible(true);
            }
        });
    }

    // Main method for standalone testing
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new UserManagement().setVisible(true);
            }
        });
    }

    // User class to store user data
    public static class User implements Serializable {
        private static final long serialVersionUID = 1L;

        private final String prename;
        private final String name;
        private final String gender;
        private final Date birthdate;
        private final String creditCard;

        public User(String prename, String name, String gender, Date birthdate, String creditCard) {
            this.prename = prename;
            this.name = name;
            this.gender = gender;
            this.birthdate = birthdate;
            this.creditCard = creditCard;
        }

        public String getPrename() {
            return prename;
        }

        public String getName() {
            return name;
        }

        public String getGender() {
            return gender;
        }

        public Date getBirthdate() {
            return birthdate;
        }

        public String getCreditCard() {
            return creditCard;
        }

        @Override
        public String toString() {
            return prename + " " + name;
        }
    }
}