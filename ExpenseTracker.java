package project;
import java.sql.*;
import java.util.Scanner;
import java.math.BigDecimal;
import java.time.LocalDate;

public class ExpenseTracker {

    static final String URL =
        "jdbc:mysql://localhost:3306/expense_tracker";

    static final String USER = "root";

    // Replace this with your MySQL root password
    static final String PASSWORD = "surya13";

    static Scanner sc = new Scanner(System.in);

    public static Connection connect() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static void addTransaction(String type) {
        try (Connection con = connect()) {

            System.out.print("Enter category: ");
            String category = sc.nextLine();

            System.out.print("Enter amount: ");
            BigDecimal amount = new BigDecimal(sc.nextLine());

            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                System.out.println("Amount must be greater than zero.");
                return;
            }

            System.out.print("Enter description: ");
            String description = sc.nextLine();

            System.out.print("Enter date (YYYY-MM-DD): ");
            LocalDate date = LocalDate.parse(sc.nextLine());

            String sql = "INSERT INTO transactions " +
                "(type, category, amount, description, transaction_date) " +
                "VALUES (?, ?, ?, ?, ?)";

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, type);
                ps.setString(2, category);
                ps.setBigDecimal(3, amount);
                ps.setString(4, description);
                ps.setDate(5, Date.valueOf(date));

                ps.executeUpdate();
                System.out.println("Transaction added successfully!");
            }

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public static void viewTransactions() {
        String sql = "SELECT * FROM transactions ORDER BY transaction_date DESC";

        try (Connection con = connect();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            System.out.println("\n------ TRANSACTION HISTORY ------");

            while (rs.next()) {
                System.out.println(
                    "ID: " + rs.getInt("id") +
                    " | Type: " + rs.getString("type") +
                    " | Category: " + rs.getString("category") +
                    " | Amount: " + rs.getBigDecimal("amount") +
                    " | Description: " + rs.getString("description") +
                    " | Date: " + rs.getDate("transaction_date")
                );
            }

        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public static void searchTransaction() {
        System.out.print("Enter category or description to search: ");
        String keyword = sc.nextLine();

        String sql = "SELECT * FROM transactions " +
            "WHERE category LIKE ? OR description LIKE ?";

        try (Connection con = connect();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, "%" + keyword + "%");
            ps.setString(2, "%" + keyword + "%");

            try (ResultSet rs = ps.executeQuery()) {
                boolean found = false;

                while (rs.next()) {
                    found = true;
                    System.out.println(
                        "ID: " + rs.getInt("id") +
                        " | " + rs.getString("type") +
                        " | " + rs.getString("category") +
                        " | Rs. " + rs.getBigDecimal("amount") +
                        " | " + rs.getDate("transaction_date")
                    );
                }

                if (!found) {
                    System.out.println("No matching transactions found.");
                }
            }

        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public static void deleteTransaction() {
        System.out.print("Enter transaction ID to delete: ");
        int id = Integer.parseInt(sc.nextLine());

        String sql = "DELETE FROM transactions WHERE id = ?";

        try (Connection con = connect();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);

            int rows = ps.executeUpdate();

            if (rows > 0) {
                System.out.println("Transaction deleted successfully!");
            } else {
                System.out.println("Transaction ID not found.");
            }

        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public static BigDecimal getTotal(String type) throws SQLException {
        String sql = "SELECT COALESCE(SUM(amount), 0) AS total " +
                     "FROM transactions WHERE type = ?";

        try (Connection con = connect();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, type);

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getBigDecimal("total");
            }
        }
    }

    public static void showBalance() {
        try {
            BigDecimal income = getTotal("INCOME");
            BigDecimal expenses = getTotal("EXPENSE");
            BigDecimal balance = income.subtract(expenses);

            System.out.println("\n------ FINANCIAL SUMMARY ------");
            System.out.println("Total Income   : Rs. " + income);
            System.out.println("Total Expenses : Rs. " + expenses);
            System.out.println("Balance        : Rs. " + balance);

        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public static void monthlyReport() {
        try {
            System.out.print("Enter year (e.g. 2026): ");
            int year = Integer.parseInt(sc.nextLine());

            System.out.print("Enter month (1-12): ");
            int month = Integer.parseInt(sc.nextLine());

            if (month < 1 || month > 12) {
                System.out.println("Invalid month.");
                return;
            }

            String sql = "SELECT type, SUM(amount) AS total " +
                "FROM transactions " +
                "WHERE YEAR(transaction_date) = ? " +
                "AND MONTH(transaction_date) = ? " +
                "GROUP BY type";

            BigDecimal income = BigDecimal.ZERO;
            BigDecimal expenses = BigDecimal.ZERO;

            try (Connection con = connect();
                 PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setInt(1, year);
                ps.setInt(2, month);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String type = rs.getString("type");
                        BigDecimal total = rs.getBigDecimal("total");

                        if ("INCOME".equals(type)) {
                            income = total;
                        } else if ("EXPENSE".equals(type)) {
                            expenses = total;
                        }
                    }
                }
            }

            System.out.println("\n------ MONTHLY REPORT ------");
            System.out.println("Income   : Rs. " + income);
            System.out.println("Expenses : Rs. " + expenses);
            System.out.println("Balance  : Rs. " +
                income.subtract(expenses));

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public static void main(String[] args) {

        while (true) {
            System.out.println("\n================================");
            System.out.println("    PERSONAL EXPENSE TRACKER");
            System.out.println("================================");
            System.out.println("1. Add Income");
            System.out.println("2. Add Expense");
            System.out.println("3. View Transactions");
            System.out.println("4. Search Transactions");
            System.out.println("5. Delete Transaction");
            System.out.println("6. Financial Summary");
            System.out.println("7. Monthly Report");
            System.out.println("8. Exit");

            System.out.print("Enter your choice: ");

            String choice = sc.nextLine();

            switch (choice) {
                case "1":
                    addTransaction("INCOME");
                    break;

                case "2":
                    addTransaction("EXPENSE");
                    break;

                case "3":
                    viewTransactions();
                    break;

                case "4":
                    searchTransaction();
                    break;

                case "5":
                    deleteTransaction();
                    break;

                case "6":
                    showBalance();
                    break;

                case "7":
                    monthlyReport();
                    break;

                case "8":
                    System.out.println("Thank you for using Expense Tracker!");
                    sc.close();
                    return;

                default:
                    System.out.println("Invalid choice. Try again.");
            }
        }
    }
}