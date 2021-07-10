package tools.approximators;

import org.rlcommunity.rlglue.codec.types.Observation;

public class CMACApproximator {
    int numberOfFeatures;
    int numberOfTilings;
    double[] weights;
    double tileSize;
    TileCoder tileCoder;

    public CMACApproximator(int numberOfFeatures, int numberOfTilings, double tileSize) {
        this.numberOfFeatures = numberOfFeatures;
        this.weights = new double[numberOfFeatures];
        this.tileCoder = new TileCoder();
        this.tileSize = tileSize;
        this.numberOfTilings = numberOfTilings;
    }

    public double getQValue(Observation observation) {
        int[] activeTiles = getActiveTilesIndices(observation);
        double result = 0.0;
        for (int i = 0; i < numberOfTilings; i++) {
            result += weights[ activeTiles[i] ];
        }
        return result;
    }
    public void setQValue(Observation observation, double error) {

        int[] activeTiles = getActiveTilesIndices(observation);

        for (int i = 0; i < numberOfTilings; i++) {
            weights[ activeTiles[i] ] += error / numberOfTilings;
        }

    }

    private int[] getActiveTilesIndices(Observation observation) {
        double[] scaledValues = new double[observation.doubleArray.length];
        for (int i = 0; i < observation.doubleArray.length; i++) {
            scaledValues[i] = observation.doubleArray[i] / tileSize;
        }
        int[] tileIndices = new int[numberOfTilings];
        int[] noInts = new int[0];
        tileCoder.tiles(tileIndices , 0 , numberOfTilings , numberOfFeatures , scaledValues , noInts);
        return tileIndices;
    }
    
    public double getCellValue(int cellIndex) {
    	return weights[cellIndex];
    }
    public void setCellValue(int cellIndex, double value) {
    	weights[cellIndex] = value;
    }
    public static void main(String[] args) {
        CMACApproximator apprx = new CMACApproximator(16,1,0.25);
        Observation observation = new Observation(0,2,0);
        observation.setDouble(0, 0.20);
        observation.setDouble(1, 0.20);

        Observation observation2 = new Observation(0,2,0);
        observation2.setDouble(0, 0.26);
        observation2.setDouble(1, 0.22);

        Observation observation3 = new Observation(0,2,0);
        observation3.setDouble(0, 0.26);
        observation3.setDouble(1, 0.28);


        int[] activeTiles = apprx.getActiveTilesIndices(observation);
        for (int activeTile : activeTiles) {
            System.out.println(activeTile);
        }

        activeTiles = apprx.getActiveTilesIndices(observation2);
        for (int activeTile : activeTiles) {
            System.out.println(activeTile);
        }

        activeTiles = apprx.getActiveTilesIndices(observation3);
        for (int activeTile : activeTiles) {
            System.out.println(activeTile);
        }


    }

}
