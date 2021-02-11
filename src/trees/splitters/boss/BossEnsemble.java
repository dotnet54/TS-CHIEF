package trees.splitters.boss;

import java.util.List;

/**
 * An Ensemble of Classifiers
 */
public class BossEnsemble<E> {

  public List<E> model;

  public BossEnsemble() {
  }

  /**
   * Create an Ensemble
   *
   * @param models List of models
   */
  public BossEnsemble(List<E> models) {
    this.model = models;
  }

  public E getHighestScoringModel() {
    return model.get(0);
  }

  public E get(int i) {
    return model.get(i);
  }

  public int size() {
    return model.size();
  }
}
