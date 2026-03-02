package Java_Oops_Example;
import java.util.List;
import java.util.ArrayList;
public class Main {
    public static void main(String[] args){
        List<Shape> shapes = new ArrayList<>();
        
        shapes.add(new Circle(10));
        shapes.add(new Rectangle(10, 20));

        for(Shape shape : shapes){
            System.out.println(shape.toString());
            System.out.println("Area: " + shape.area());
            if(shape instanceof Drawable){
                ((Drawable) shape).draw();
            }
        }
        System.out.println("Total shapes: " + Shape.getShapeCount());
    }
    
}
