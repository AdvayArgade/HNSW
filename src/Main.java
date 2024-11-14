import java.util.*;
import utils.*;

public class Main {
    public static void main(String[] args) {
        HNSW hnsw = new HNSW(0.8, 40, 10);
        int[][] points = {
                {23, 45}, {12, 5}, {67, 89}, {43, 54}, {91, 12},
                {34, 76}, {28, 9}, {76, 45}, {85, 62}, {49, 33},
                {14, 67}, {93, 28}, {56, 77}, {32, 58}, {68, 14},
                {22, 39}, {84, 97}, {55, 15}, {71, 44}, {60, 29}
        };
        try {
            points = CSVLoader.loadPointsFromCSV("dataset10k.csv");
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        hnsw.setVectors(points);

        long startTime = System.nanoTime();
        hnsw.build(false);
        long endTime = System.nanoTime();
        long elapsedTime = endTime - startTime;
//        System.out.println("Elapsed time in building in milliseconds: " + elapsedTime);

//        hnsw.printLevels();
//        System.out.println(Arrays.toString(hnsw.getEntryPoint()));
        int[] query = new int[]{40, 50};

        startTime = System.nanoTime();
        List<int[]> neighbors = hnsw.search(query, 10, 60);
        endTime = System.nanoTime();
        elapsedTime = endTime - startTime;
        System.out.println("Query: " + Arrays.toString(query));

        System.out.println("HNSW:");
        System.out.println("Elapsed time in searching in milliseconds: " + elapsedTime);


        System.out.print("Neighbors: ");
        for(int[] neighbor : neighbors) {
            System.out.print(Arrays.toString(neighbor) + " ");
        }
        System.out.println();

        startTime = System.nanoTime();
        Naive.doFinal(points, query, 10);
        endTime = System.nanoTime();
        elapsedTime = endTime - startTime;
        System.out.println("Elapsed time in searching with naive method in milliseconds: " + elapsedTime);

    }
}

class Node{
    private final int[] vector;
    float distance;
    private Node referenceNode;
    public Node(int[] vector) {
        this.vector = vector;
    }

    @Override
    public boolean equals(Object obj) {
        // Check if the object is the same as this
        if (this == obj) {
            return true;
        }
        // Check if the object is an instance of Node
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        // Cast the object to Node and compare the vectors
        Node otherNode = (Node) obj;
        return Arrays.equals(this.vector, otherNode.vector);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(vector);
    }

    public Node(int[] vector, Node referenceNode) {
        this.vector = vector;
        this.referenceNode = referenceNode;
        calculateDistance();
    }

    public int[] getVector() {
        return vector;
    }

    public void setReferenceNode(Node referenceNode) {
        this.referenceNode = referenceNode;
    }

    public void calculateDistance() {
        if(referenceNode == null) {
            distance = 0;
            return;
        }

        double distSquared = 0;
        for(int i = 0; i < vector.length; i++) {
            distSquared += Math.pow(vector[i] - referenceNode.vector[i], 2);
        }
        distance = (float) Math.sqrt(distSquared);
    }

    @Override
    public String toString() {
        return "vector: " + Arrays.toString(vector) + "distance: " + distance;
    }
}

class NodeComparator implements Comparator<Node> {
    public int compare(Node n1, Node n2) {
        return Float.compare(n1.distance, n2.distance);
    }
}

class HNSW{
    //constants
    private int dimension;
    private final double levelMultiplier;
    private final int m0;
    private final int m;
    private int highestAllowedLevel = -1;
    private int highestActualLevel = -1;

    //vectors
    static class DataFields {
        int[][] vectors;
        List<Map<int[], SortedSet<Node>>> partitions;
        int[] globalEntryPoint = null;
        public DataFields(){
            partitions = new ArrayList<>();
        }
    }
    private DataFields data = new DataFields();

    public int[] getEntryPoint(){
        return data.globalEntryPoint;
    }

    public HNSW(double levelMultiplier, int m0, int highestAllowedLevel) {
        this.levelMultiplier = levelMultiplier;
        this.m0 = m0;
        this.m = m0/2;
        this.highestAllowedLevel = highestAllowedLevel;
    }

    public HNSW(int m0, int highestAllowedLevel) {
        this.m0 = m0;
        this.m = m0/2;
        levelMultiplier = (int) (1/(Math.log(m0)));
        this.highestAllowedLevel = highestAllowedLevel;
    }

    public HNSW(int m0) {
        this.m0 = m0;
        this.m = m0/2;
        levelMultiplier = (int) (1/(Math.log(m0)));
    }

    public void printLevels(){
        int index = 0;
        for(Map<int[], SortedSet<Node>> level : data.partitions){
            System.out.println("Level: " + index++);
            level.forEach((key, value)->{
                System.out.print(Arrays.toString(key) + " -> ");
                for(Node vector : value) {
                    System.out.print(Arrays.toString(vector.getVector()) + " ");
                }
                System.out.println();
            });
            System.out.println("--------------------");
            System.out.println();
        }
    }

    public void setVectors(int[][] vectors) {
        this.data.vectors = vectors;
        dimension = vectors[0].length;
        //default formula for highest level
        if(highestAllowedLevel==-1) highestAllowedLevel = (int) (Math.log(vectors.length)/levelMultiplier);
    }

    private Node fetchEntryPoint(int level){
        Map<int[], SortedSet<Node>> partition = data.partitions.get(level);
        Node answer = null;
        for(Map.Entry<int[], SortedSet<Node>> keys: partition.entrySet()){
            answer = new Node(keys.getKey());
            break;
        }
        return answer;
    }

    public void build(boolean verbose){
        //create new arraylists for each level
        for(int i = 0; i <= highestAllowedLevel; i++){
            data.partitions.add(new HashMap<>());
        }

        //partition the vectors
        for(int[] vector: data.vectors){
            int level = Math.min((int) ((-Math.log(Math.random()))*levelMultiplier), highestAllowedLevel);

            //NSW search for each level
            for(int i = level; i>=0; i--){
                Map<int[], SortedSet<Node>> currLevel = data.partitions.get(i);
                // skip the whole process if a duplicate exists
                if(currLevel.containsKey(vector)) continue;

                Node entry = fetchEntryPoint(i);
                if(entry==null) {
                    currLevel.put(vector, new TreeSet<>(new NodeComparator()));
                    continue;
                }

                Node reference = new Node(vector, null);
                entry.setReferenceNode(reference);
                entry.calculateDistance();
                SortedSet<Node> neighborSet = new TreeSet<>(new NodeComparator());
                
                PriorityQueue<Node> minHeap = new PriorityQueue<>(new NodeComparator());
                minHeap.add(entry);

                //============Implement neighbor list with tree set=============
                // while inserting current node in neighbor list of other nodes,
                // if the number of neighbors of other nodes exceeds m, don't make this connection
                // by doing so if the current node ends up isolated, remove neighbors of some of the candidates
                // and add this connection
                while(!minHeap.isEmpty()){
                    Node minNode = minHeap.poll();
                    minHeap.clear();
                    neighborSet.add(minNode);
                    boolean isOptimal = true;
//                    System.out.println(currLevel.get(minNode.getVector()));
                    // ========== make the neighbor set of fixed size =============
                    for(Node neighbor: currLevel.get(minNode.getVector())){
                        Node neighborNode = new Node(neighbor.getVector(), reference);
                        neighborSet.add(neighborNode);
                        if(neighborNode.distance < minNode.distance){
                            minHeap.add(neighborNode);
                            isOptimal = false;
                        }
                    }
                    if(isOptimal) break;
                }
                
                int numNeighbors = m;
                if(i==0) numNeighbors = m0;

                Iterator<Node> iterator = neighborSet.iterator();
                int successfulConnections = 0;

                // pruning and making connections
                while (iterator.hasNext() && successfulConnections <= numNeighbors) {
                    Node neighbor = iterator.next();
                    SortedSet<Node> neighborsNeighbors = currLevel.get(neighbor.getVector());
                    if(neighborsNeighbors.size() == numNeighbors){
                        // remove the last element from the neighbor's neighbors if it has a neighbor other than the
                        // current node under consideration
                        SortedSet<Node> lastsNeighbors = currLevel.get(neighborsNeighbors.last().getVector());
                        if(neighborsNeighbors.last().distance > neighbor.distance && lastsNeighbors.size()>1){
                            neighborsNeighbors.remove(neighborsNeighbors.last());
                            lastsNeighbors.remove(new Node(neighbor.getVector()));
                            neighborsNeighbors.add(new Node(vector, neighbor));
                            successfulConnections++;
                        }

                        else {
                            iterator.remove();
                            continue;
                        }
                    }
                    neighborsNeighbors.add(new Node(vector, neighbor));
                    successfulConnections++;
                }

                while (neighborSet.size()>numNeighbors) neighborSet.remove(neighborSet.last());

                currLevel.put(vector, neighborSet);
                // ================== Optional: Ensure that no node is isolated =====================
            }
            // adjusting the global entry point
            // if the current vector has more neighbors than the current global entry point,
            // and it is at the highest actual level
            // make it the new entry point
            if(data.globalEntryPoint==null) data.globalEntryPoint = vector;
            else{
                SortedSet<Node> globalEntryPointNeighbors = data.partitions.get(highestActualLevel).get(data.globalEntryPoint);
                SortedSet<Node> currHighestLevelNeighbors = data.partitions.get(level).get(vector);
                if(level>highestActualLevel) data.globalEntryPoint = vector;
                else if(globalEntryPointNeighbors.size()<=currHighestLevelNeighbors.size() && level==highestActualLevel){
                    data.globalEntryPoint = vector;
                }
            }
            highestActualLevel = Math.max(level, highestActualLevel);
            if(verbose) System.out.println("Inserted " + Arrays.toString(vector));
        }

    }

    List<int[]> search(int[] query, int numNeighbors, int explorationFactor){
        List<int[]> neighbors = new ArrayList<>(numNeighbors);
        TreeSet<Node> candidates = new TreeSet<>(new NodeComparator());

        // part 1 : greedy search of the closest point in the higher layers
        PriorityQueue<Node> minHeap = new PriorityQueue<>(new NodeComparator());
        Node reference = new Node(query, null);
        minHeap.add(new Node(data.globalEntryPoint, reference));
        Node lastClosest = null;

        // main loop
        for(int i = highestActualLevel; i>=0; i--){
            Map<int[], SortedSet<Node>> currLevel = data.partitions.get(i);
            if(lastClosest!=null) minHeap.add(lastClosest);
            Node minNode = null;
            while(!minHeap.isEmpty() && candidates.size()<=explorationFactor){
                minNode = minHeap.poll();
                if(i==0) candidates.addAll(minHeap);
                minHeap.clear();
                boolean isOptimal = true;
                for(Node neighbor: currLevel.get(minNode.getVector())){
                    Node neighborNode = new Node(neighbor.getVector(), reference);
                    if(i==0 && candidates.size()<numNeighbors) candidates.add(neighborNode);
                    if(neighborNode.distance < minNode.distance){
                        minHeap.add(neighborNode);
                        isOptimal = false;
                    }
                }
                if(isOptimal) break;
            }
            lastClosest = minNode;
            candidates.add(lastClosest);
            if(i==0) candidates.addAll(minHeap);
            minHeap.clear();
        }

        for(int i = 0; i<numNeighbors; i++){
            if(candidates.isEmpty()) break;
            neighbors.add(candidates.pollFirst().getVector());
        }

        return neighbors;
    }

}
