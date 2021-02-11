package data.timeseries;

public interface LabelEncoder {

    public void fit(String[] y);

    public int[] fitTransform(String[] y);

    public int[] transform(String[] y) throws Exception;

    public String[] inverseTransform(int[] y) throws Exception;

    public Integer[] getUniqueLabels();

    public String[] getUniqueLabelStrings();

    public boolean hasLabel(Integer label);

    public boolean hasStringLabel(String stringLabel);

    public Integer getLabel(String stringLabel);

    public String getStringLabel(Integer label);

    public boolean isFitted();

}
