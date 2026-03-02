package Java_Complete_Notes;
import java.util.List;
import java.util.ArrayList;
public class ExpenseManager {

    private List<Expense> expenses = new ArrayList<>();
    static int nextId = 0;

    void addExpense(String description, double amount, String date, Category category){
        Expense expense = new Expense(nextId++, description, amount, date, category);
        expenses.add(expense);
    }

    List<Expense> getAllExpenses(){
        return new ArrayList<>(expenses);
    }

    double getTotalExpenses(){
        double total = 0;
        for(Expense expense : expenses){
            total += expense.getAmount();
        }
        return total;
    }

    List<Expense> filterByCategory(Category category){
        List<Expense> filteredExpenses = new ArrayList<>();
        for(Expense expense : expenses){
            if(expense.getCategory() == category){
                filteredExpenses.add(expense);
            }
        }
        return filteredExpenses;
    }

    List<Expense> filterByCategory(String CategoryName){
        List<Expense> filteredExpenses = new ArrayList<>();
        for(Expense expense : expenses){
            if(expense.getCategory().toString().equals(CategoryName)){
                filteredExpenses.add(expense);
            }
        }
        return filteredExpenses;
    }

}
