package co.com.simon.models;

import org.openqa.selenium.Point;

public class Coordinates {

    private final int xCoordinate;
    private final int yCoordinate;

    public Coordinates(int xCoordinate, int yCoordinate) {
        this.xCoordinate = xCoordinate;
        this.yCoordinate = yCoordinate;
    }

    public static Point withCoordinates(int xCoordinate , int yCoordinate){
        return new Point(xCoordinate, yCoordinate);
    }

    public int getxCoordinate() {
        return xCoordinate;
    }

    public int getyCoordinate() {
        return yCoordinate;
    }
}
