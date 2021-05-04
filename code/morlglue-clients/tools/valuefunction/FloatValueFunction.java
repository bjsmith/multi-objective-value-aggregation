package tools.valuefunction;

import org.rlcommunity.rlglue.codec.types.Observation;
import org.rlcommunity.rlglue.codec.types.Reward;

import java.util.ArrayList;
import java.util.Random;

public class FloatValueFunction {
    protected final int numberOfObjectives;
    protected final int numberOfActions;
    protected final int solarStates;
    protected final int loadStates;
    protected final int batteryStates;
    protected final int futureSolarStates;
    protected final int futureLoadStates;
    float[] weights = null;
    Random random = null;

    protected ArrayList< ArrayList<float[][][][][]> > valueFunction = null;
    protected float[] errors = null;

    public FloatValueFunction(int numberOfObjectives, int numberOfActions, int solarStates, int loadStates, int batteryStates, int futureSolarStates, int futureLoadStates, float[] weights) {
        this.numberOfObjectives = numberOfObjectives;
        this.numberOfActions = numberOfActions;
        this.solarStates = solarStates;
        this.loadStates = loadStates;
        this.batteryStates = batteryStates;
        this.futureSolarStates = futureSolarStates;
        this.futureLoadStates = futureLoadStates;

        valueFunction = new ArrayList<>();

        for (int i = 0; i < numberOfObjectives; i++) {
            ArrayList<float[][][][][]> actions = new ArrayList<>();
            for (int j = 0; j < numberOfActions; j++) {
                float[][][][][] array = new float[solarStates][loadStates][batteryStates][futureSolarStates][futureLoadStates];
                actions.add(array);
            }
            valueFunction.add(actions);
        }
        errors = new float[numberOfObjectives];
        this.weights = weights;
        random = new Random();
    }

    public void calculateErrors(int previousAction, Observation prevObservation, int greedyAction, Observation newObservation, float gamma, Reward reward) {
        for (int i = 0; i < numberOfObjectives; i++) {

            ArrayList<float[][][][][]> actions = valueFunction.get(i);

            float[][][][][] qTable =  actions.get(previousAction);
            int solarBin = discretizeBy50(getSolar(prevObservation));
            int loadBin = discretizeBy50(getLoad(prevObservation));
            int battBin = discretizeBy50(getBatteryCharge(prevObservation));
            int fSoloarBin = discretizeBy50(getFutureSolar(prevObservation));
            int fLoadBin = discretizeBy50(getFutureLoad(prevObservation));
            float thisQ = qTable[solarBin][loadBin][battBin][fSoloarBin][fLoadBin];

            qTable =  actions.get(greedyAction);
            solarBin = discretizeBy50(getSolar(newObservation));
            loadBin = discretizeBy50(getLoad(newObservation));
            battBin = discretizeBy50(getBatteryCharge(newObservation));
            fSoloarBin = discretizeBy50(getFutureSolar(newObservation));
            fLoadBin = discretizeBy50(getFutureLoad(newObservation));
            float maxQ = qTable[solarBin][loadBin][battBin][fSoloarBin][fLoadBin];

            errors[i] =  ((float)reward.doubleArray[i]) + gamma * maxQ - thisQ;
        }
    }

    public void update(int previousAction, Observation prevObservation, float alpha) {

        for (int i = 0; i < numberOfObjectives; i++) {
            ArrayList<float[][][][][]> actions = valueFunction.get(i);
            float[][][][][] qTable =  actions.get(previousAction);
            int solarBin = discretizeBy50(getSolar(prevObservation));
            int loadBin = discretizeBy50(getLoad(prevObservation));
            int battBin = discretizeBy50(getBatteryCharge(prevObservation));
            int fSoloarBin = discretizeBy50(getFutureSolar(prevObservation));
            int fLoadBin = discretizeBy50(getFutureLoad(prevObservation));
            float thisQ = qTable[solarBin][loadBin][battBin][fSoloarBin][fLoadBin];


            float newQ = thisQ + alpha * errors[i] ;
            qTable[solarBin][loadBin][battBin][fSoloarBin][fLoadBin] = newQ;
        }
    }

    public int chooseGreedyAction(Observation observation) {
        ArrayList<Integer> bestActions = new ArrayList<>();
        float bestValue = -Float.MAX_VALUE;

        int solarBin = discretizeBy50(getSolar(observation));
        int loadBin = discretizeBy50(getLoad(observation));
        int battBin = discretizeBy50(getBatteryCharge(observation));
        int fSoloarBin = discretizeBy50(getFutureSolar(observation));
        int fLoadBin = discretizeBy50(getFutureLoad(observation));

        for (int a = 0; a < numberOfActions; a++) {
            float scalarValue = 0;
            for (int o = 0; o < numberOfObjectives; o++) {
                ArrayList<float[][][][][]> actions = valueFunction.get(o);
                float[][][][][] qTable = actions.get(a);
                float qValue =qTable[solarBin][loadBin][battBin][fSoloarBin][fLoadBin];
                scalarValue += weights[o] * qValue;
            }
            if ( scalarValue >= bestValue ) {
                if ( scalarValue > bestValue ) {
                    bestActions.clear();
                    bestActions.add(a);
                    bestValue = scalarValue;
                } else {
                    bestActions.add(a);
                }
            }


        }
        if (bestActions.size() > 1) {
            return bestActions.get(random.nextInt(bestActions.size()));
        } else {
            return bestActions.get(0);
        }

    }



    private int discretizeBy20(float input) {
        return  (int) Math.floor( input / 20 );
    }
    private int discretizeBy100(float input) {
        return  (int) Math.floor( input / 100 );
    }

    private int discretizeBy50(float input) {
        return  (int) Math.floor( input / 50 );
    }

    private float getSolar(Observation observation) {

        assert observation != null : "Observation object is null";
        double predictedEnergy = observation.getDouble( 0 );
        assert predictedEnergy < 783  && predictedEnergy >= 0 : "Predicted energy is not in a specified range";

        return (float)predictedEnergy;
    }
    private float getLoad(Observation observation) {

        assert observation != null : "Observation object is null";
        double load = observation.getDouble( 1 );
        assert load < 965  && load >= 0 : "Load is not in a specified range";

        return (float)load;
    }
    private float getBatteryCharge(Observation observation) {

        assert observation != null : "Observation object is null";
        double batteryCharge = observation.getDouble( 2 );
        assert batteryCharge <= 2000  && batteryCharge >= 0 : "Battery charge is not in a specified range";

        return (float)batteryCharge;
    }
    private float getFutureSolar(Observation observation) {

        assert observation != null : "Observation object is null";
        double futureSolar = observation.getDouble( 3 );
        assert futureSolar <= 5000  && futureSolar >= 0 : "Battery charge is not in a specified range";

        return (float)futureSolar;
    }
    private float getFutureLoad(Observation observation) {

        assert observation != null : "Observation object is null";
        double futureLoad = observation.getDouble( 4 );
        assert futureLoad <= 6000  && futureLoad >= 0 : "Battery charge is not in a specified range";

        return (float)futureLoad;
    }


}
