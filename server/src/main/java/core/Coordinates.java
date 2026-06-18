package core;

/**
 * Класс, описывающий координаты для класса Ticket.
 */
public class Coordinates implements Comparable<Coordinates>{
    private Double x;
    private double y;
    public Coordinates(){

    }

    @Override
    public String toString(){
        return "x: "+x+"\t|\ty: "+y;
    }

    public void setX(Double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }

    public Double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    @Override
    public int compareTo(Coordinates c) {
        if ((this.x == c.x) && (this.y == c.y)) return 0;
        else if ((this.x >= c.x && this.y >= c.y) || (this.x >= c.x && this.y < c.y)) return 1;
        return -1;
    }
}
