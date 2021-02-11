package data.timeseries;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class NonContinuousLabelEncoder implements LabelEncoder{

//    Encode target labels with value between 0 and nClasses-1.

    private Map<String, Integer> encodings;
    private Map<Integer, String> decodings;
    int nextEncodingLabel = 0;
    private boolean isFitted = false;

    public NonContinuousLabelEncoder(){
        encodings = new HashMap<>();
        decodings = new HashMap<>();
    }

    //TOOD note this function does not clear any existing labels -- add a reset/clear function?
    public void fit(String[] y){
        String[] uniqueClasses = Arrays.stream(y).distinct().toArray(String[]::new);
        Arrays.sort(uniqueClasses);

        for (int i = 0; i < uniqueClasses.length; i++) {
            encodings.put(uniqueClasses[i], i);
            decodings.put(i, uniqueClasses[i]);
        }

        nextEncodingLabel = encodings.size();

        isFitted = true;
    }

    public int[] fitTransform(String[] y){
        fit(y);
        return encodings.values().stream().mapToInt(i->i).toArray();
    }

    public int[] transform(String[] y) throws Exception {
        if (!isFitted){
            throw new Exception("LabelEncoder must be fitted before calling the transform function.");
        }

        int[] testEncodings = new int[y.length];

        for (int i = 0; i < y.length ; i++) {
            if (encodings.containsKey(y[i])){
                testEncodings[i] = encodings.get(y[i]);
            }else{
                throw new Exception("Argument y contains labels unknown during the fitting process");
            }
        }

        return testEncodings;
    }

    public String[] inverseTransform(int[] y) throws Exception {
        if (!isFitted){
            throw new Exception("LabelEncoder must be fitted before calling the inverse_transform function.");
        }

        String[] testDecodings = new String[y.length];

        for (int i = 0; i < y.length ; i++) {
            if (decodings.containsKey(y[i])){
                testDecodings[i] = decodings.get(y[i]);
            }else{
                throw new Exception("Argument y contains labels unknown during the fitting process");
            }
        }

        return testDecodings;
    }

    public synchronized void addLabel(String label){

        if (!encodings.containsKey(label)){
            encodings.put(label, nextEncodingLabel);
            decodings.put(nextEncodingLabel, label);
            nextEncodingLabel++;    // potential integer overflow could occur, unlikely in this application
        }

        if (!isFitted){
            isFitted = true;
        }
    }

    public synchronized void removeLabel(String label){

        if (!encodings.containsKey(label)){
            int encoding = encodings.get(label);
            encodings.remove(label);
            decodings.remove(encoding);
        }

        if (encodings.isEmpty()){
            isFitted = false;
            nextEncodingLabel = 0;
        }
    }

    public Integer[] getUniqueLabels(){
        return decodings.keySet().toArray(new Integer[decodings.size()]);
    }

    public String[] getUniqueLabelStrings(){
        return encodings.keySet().toArray(new String[encodings.size()]);
    }

    public boolean hasLabel(Integer label){
        return encodings.containsKey(label);
    }

    public boolean hasStringLabel(String stringLabel){
        return decodings.containsKey(stringLabel);
    }

    public Integer getLabel(String stringLabel){
        return encodings.get(stringLabel);
    }

    public String getStringLabel(Integer label){
        return decodings.get(label);
    }

    public boolean isFitted(){
        return encodings.size() > 0;
    }


}
