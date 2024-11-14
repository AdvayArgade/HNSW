package utils;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class DataManager {
    public static void main(String[] args) {
        int[][] points =  DataGenerator.generateRandomPoints(100000, 2, -2000, 2000);
        try{
            DataSaver.savePointsToCSV(points, "dataset100k.csv");

        }
        catch(IOException e){
            e.printStackTrace();
        }
        try {
            points =  CSVLoader.loadPointsFromCSV("dataset1M.csv");
            for (int i = 0; i < 20; i++) {
                System.out.println(Arrays.toString(points[i]));
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class DataGenerator{
    public static int[][] generateRandomPoints(int numPoints, int dimension, int origin, int bound) {
        Random random = new Random();
        int[][] points = new int[numPoints][dimension];

        for (int i = 0; i < numPoints; i++) {
            int x = random.nextInt(origin, bound);  // random value between 0.0 and 1.0
            int y = random.nextInt(origin, bound);
            points[i] = new int[]{x, y};
        }
        return points;
    }
}

class DataSaver {
    public static void savePointsToCSV(int[][] points, String filename) throws IOException {
        FileWriter writer = new FileWriter(filename);

        for (int[] point : points) {
            writer.write(point[0] + "," + point[1] + "\n");  // Write x,y coordinates
        }
        writer.close();
    }
}



