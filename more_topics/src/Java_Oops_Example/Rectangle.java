package Java_Oops_Example;

public class Rectangle extends Shape implements Drawable {
    private double width;
    private double height;

    Rectangle(double width, double height){
        super("Rectangle");
        this.width = width;
        this.height = height;
    }

    public double getWidth(){
        return width;
    }

    public void setWidth(double width){
        this.width = width;
    }

    public double getHeight(){
        return height;
    }
    
    public void setHeight(double height){
        this.height = height;
    }

    @Override
    public double area() {
        return width * height;
    }

    @Override
    public void draw() {
        System.out.println("Drawing Rectangle");
    }
}
