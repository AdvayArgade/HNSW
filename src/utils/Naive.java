package utils;

import java.util.Comparator;
import java.util.PriorityQueue;

public class Naive {
        // Method to calculate squared Euclidean distance between two points
        private static int euclideanDistanceSquared(int[] point1, int[] point2) {
            int dx = point1[0] - point2[0];
            int dy = point1[1] - point2[1];
            return dx * dx + dy * dy;
        }

        // Naive KNN search using a max-heap (PriorityQueue)
        public static int[][] knn(int[][] points, int[] queryPoint, int k) {
            // Max-heap to store the k nearest neighbors based on distance
            PriorityQueue<int[]> maxHeap = new PriorityQueue<>(k, new Comparator<int[]>() {
                @Override
                public int compare(int[] a, int[] b) {
                    // Compare based on Euclidean distance squared
                    int distA = euclideanDistanceSquared(a, queryPoint);
                    int distB = euclideanDistanceSquared(b, queryPoint);
                    return Integer.compare(distB, distA);  // Max-heap: larger distance comes first
                }
            });

            // Iterate over all points
            for (int[] point : points) {
                // Add the current point to the heap
                maxHeap.offer(point);

                // If the heap exceeds size k, remove the farthest point
                if (maxHeap.size() > k) {
                    maxHeap.poll();
                }
            }

            // Extract the k nearest neighbors from the heap
            int[][] kNearest = new int[k][2];
            for (int i = 0; i < k; i++) {
                kNearest[i] = maxHeap.poll();
            }

            return kNearest;
        }

        public static void doFinal(int[][] points, int[] queryPoint, int k) {
            int[][] nearestNeighbors = knn(points, queryPoint, k);

            // Print the k nearest neighbors
            System.out.println("K Nearest Neighbors:");
            for (int[] neighbor : nearestNeighbors) {
                System.out.println("(" + neighbor[0] + ", " + neighbor[1] + ")");
            }
        }
}
