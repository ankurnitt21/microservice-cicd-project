package Java_Complete_Notes;

public class Expense implements Printable {
    private int id;
    private String description;
    private double amount;
    private String date;
    private Category category;

    Expense(int id, String description, double amount, String date, Category category){
        this.id = id;
        this.description = description;
        this.amount = amount;
        this.date = date;
        this.category = category;
    }

    public int getId(){
        return id;
    }

    public String getDescription(){
        return description;
    }

    public double getAmount(){
        return amount;
    }

    public String getDate(){
        return date;
    }

    public Category getCategory(){
        return category;
    }

    public void setId(int id){
        this.id = id;
    }

    public void setDescription(String description){
        this.description = description;
    }

    public void setAmount(double amount){
        this.amount = amount;
    }
    
    public void setDate(String date){
        this.date = date;
    }

    public void setCategory(Category category){
        this.category = category;
    }

    @Override
    public String toString(){
        return "Expense [id=" + id + ", description=" + description + ", amount=" + amount + ", date=" + date + ", category=" + category + "]";
    }

    @Override
    public void printSummary(){
        System.out.println("Expense [id=" + id + ", description=" + description + ", amount=" + amount + ", date=" + date + ", category=" + category + "]");
    }
}
