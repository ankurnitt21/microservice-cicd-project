package Java_Oops_Example;

abstract class Shape {

    private String name;
    private static int shapeCount = 0;

    Shape(String name){
        this.name = name;
        shapeCount++;
    }

    public String getName(){
        return name;
    }

    public abstract double area();

    @Override
    public String toString() {
        return "Shape: " + name;
    }

    public static final int getShapeCount(){
        return shapeCount;
    }
    
}
