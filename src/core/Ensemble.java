package core;

public interface Ensemble {

    public int getSize();

    public Classifier[] getModels();

    public Classifier getModel(int i);

}
