package utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class CSVLoader {
    public static int[][] loadPointsFromCSV(String filename) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filename));
        ArrayList<int[]> points = new ArrayList<>();
        String line;

        while ((line = reader.readLine()) != null) {
            String[] coords = line.split(",");
            int x = Integer.parseInt(coords[0]);
            int y = Integer.parseInt(coords[1]);
            points.add(new int[]{x, y});  // Add the (x, y) pair to the list
        }
        reader.close();

        // Convert the ArrayList to a 2D int array
        return points.toArray(new int[0][0]);
    }
}
