package Java_Complete_Notes;
import java.util.Scanner;
public class Main {
    public static void main(String[] args){
        ExpenseManager expenseManager = new ExpenseManager();

        while(true){
            System.out.println("1. Add Expense");
            System.out.println("2. Get All Expenses");
            System.out.println("3. Get Total Expenses");
            System.out.println("4. Filter By Category");
            System.out.println("5. Exit");
            Scanner sc = new Scanner(System.in);
            int choice;

            try {
                choice = sc.nextInt();
            } catch (NumberFormatException e) {
                System.out.println("Invalid input");
                continue;
            }
            switch(choice){
                case 1:
                    expenseManager.addExpense("Food", 100, "2026-01-01", Category.FOOD);
                    break;
                case 2:
                    expenseManager.getAllExpenses().forEach(expense -> expense.printSummary());
                    break;
                case 3:
                    System.out.println("Total Expenses: " + expenseManager.getTotalExpenses());
                    break;
                case 4:
                    expenseManager.filterByCategory(Category.FOOD).forEach(expense -> expense.printSummary());
                    break;
                case 5:
                    System.exit(0);
                    break;
            }
        }
    }
}
