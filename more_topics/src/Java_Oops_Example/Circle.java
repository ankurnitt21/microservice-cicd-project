package Java_Oops_Example;

public class Circle extends Shape implements Drawable {
    private double radius;

    Circle(double radius){
        super("Circle");
        this.radius = radius;
    }

    public double getRadius(){
        return radius;
    }

    public void setRadius(double radius){
        this.radius = radius;
    }

    @Override
    public double area() {
          return Math.PI * radius * radius;
    }

    @Override
    public void draw() {
        System.out.println("Drawing Circle");
    }

    public void resize(double factor){
        radius *= factor;
    }

    public void resize(double newRadius, boolean printOldValue){
        double oldRadius = radius;
        radius = newRadius;
        if(printOldValue){
            System.out.println("Old radius: " + oldRadius);
        }
    }
}
