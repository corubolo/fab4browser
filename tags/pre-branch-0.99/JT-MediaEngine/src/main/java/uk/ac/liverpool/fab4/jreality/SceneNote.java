package uk.ac.liverpool.fab4.jreality;

import de.jreality.geometry.Primitives;
import de.jreality.scene.IndexedFaceSet;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.scene.data.Attribute;
import de.jreality.scene.data.StorageModel;
import de.jreality.util.SceneGraphUtility;
import java.util.Map;
import multivalent.Behavior;
import multivalent.CHashMap;
import multivalent.Document;
import multivalent.ESISNode;
import multivalent.Layer;
import uk.ac.liverpool.fab4.jreality.SoftViewerLeaf;

public class SceneNote extends Behavior
{
  private String theText;

  public ESISNode save()
  {
    putAttr("text", this.theText);
    ESISNode e = super.save();
    e.appendChild(this.theText);

    CHashMap pdest_ = new CHashMap(5);

    e.appendChild(new ESISNode("destination", pdest_));
    e.appendChild(this.theText);

    return e;
  }

    public void restore(ESISNode n, Map<String, Object> attr, Layer layer)
  {
    super.restore(n, attr, layer);
    Document doc = (Document)getBrowser().getRoot().findBFS("content");
    if (doc.getFirstLeaf() instanceof SoftViewerLeaf) {
      SoftViewerLeaf sw = (SoftViewerLeaf)doc.getFirstLeaf();
      SceneGraphComponent root = sw.getSceneRoot();
      SceneGraphComponent aux = sw.getAuxiliaryRoot();
      if (aux == null) {
        aux = SceneGraphUtility.createFullSceneGraphComponent("AUX");
      }

      this.theText = getAttr("text", "(nothing)");
      putAttr("text", this.theText);
      IndexedFaceSet cubo = Primitives.cube();
      int nn = cubo.getNumFaces();
      String[] labels = new String[nn];
      for (int i = 0; i < nn; ++i)
        labels[i] = (this.theText + i);
      cubo.setFaceAttributes(Attribute.LABELS, 
        StorageModel.STRING_ARRAY.createReadOnly(labels));
      aux.setGeometry(cubo);
      root.addChild(aux);
    }
  }
}