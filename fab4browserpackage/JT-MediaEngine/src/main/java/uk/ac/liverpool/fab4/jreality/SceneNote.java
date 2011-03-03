package uk.ac.liverpool.fab4.jreality;

import static de.jreality.shader.CommonAttributes.POLYGON_SHADER;
import static de.jreality.shader.CommonAttributes.TRANSPARENCY;

import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.swing.JOptionPane;

import multivalent.Behavior;
import multivalent.Document;
import multivalent.ESISNode;
import multivalent.Layer;
import uk.ac.liverpool.fab4.Fab4;
import de.jreality.geometry.BoundingBoxUtility;
import de.jreality.geometry.IndexedFaceSetUtility;
import de.jreality.scene.Appearance;
import de.jreality.scene.IndexedFaceSet;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.scene.SceneGraphNode;
import de.jreality.shader.CommonAttributes;
import de.jreality.util.Rectangle3D;
import de.jreality.util.SceneGraphUtility;

public class SceneNote extends Behavior
{
    private String theText;
    private String location;
    SceneGraphComponent destination;
    private SceneGraphComponent c;

    public ESISNode save()
    {

        putAttr("text", this.theText);
        putAttr("location", this.location);
        ESISNode e = super.save();
        ESISNode content = new ESISNode("content");
        e.appendChild(content);
        content.appendChild(theText.toString());
        return e;
    }


    @Override
    public void destroy() {
        if (destination!=null) {
            destination.removeChild(c);
        }
        super.destroy();
    }
    public void restore(ESISNode n, Map<String, Object> attr, Layer layer)
    {
        super.restore(n, attr, layer);
        Document doc = (Document)getBrowser().getRoot().findBFS("content");
        if (doc.getFirstLeaf() instanceof SoftViewerLeaf) {
            final SoftViewerLeaf sw = (SoftViewerLeaf)doc.getFirstLeaf();

            location = getAttr("location");
            // Case of a newly created note
            if (location == null) {
                if (sw.sgp == null){

                    JOptionPane.showMessageDialog(getBrowser(), "Please select first a component to annotate (point and click the center button or alt-left button) ");
                    return;}
                location = "";
                for (SceneGraphNode curr : sw.sgp.toList()) {
                    location+=curr.getName() + "§";
                }
                destination = sw.sgp.getLastComponent();
                theText = JOptionPane.showInputDialog("Note content: ");
                Fab4.getMVFrame(getBrowser()).updateAnnoIcon();
            }
            // case this is an existing note being loaded 
            else {
                destination = loadPath(sw);
                sw.sgp = SceneGraphUtility.getPathsBetween(sw.rootNode, destination).get(0);
                theText = getAttr("text", "(nothing)");

            }
            //Appearance app = destination.getAppearance();
            //if (app == null){
            Appearance   app = new Appearance("Note");

            //}

//            de.jreality.shader.DefaultGeometryShader dgs = ShaderUtility.createDefaultGeometryShader(app, true);
//            DefaultTextShader fts = (DefaultTextShader) ((DefaultPolygonShader)dgs.getPolygonShader()).getTextShader();
//            fts.setDiffuseColor(Color.black);
//
//            Double scale = new Double(0.01);
//
//            fts.setScale(scale);
//
//            double[] offset = new double[]{-.1,0,0.3};
//
//            fts.setOffset(offset);
//            DefaultGeometryShader dgs = ShaderUtility.createDefaultGeometryShader(app, true);
//            dgs.setShowLines(false);
//            dgs.setShowPoints(false);
//            DefaultPolygonShader dps = (DefaultPolygonShader) dgs.createPolygonShader("default");
//            dps.setDiffuseColor(Color.white);
//            Texture2D tex2d = (Texture2D) AttributeEntityUtility.
//            createAttributeEntity(Texture2D.class, POLYGON_SHADER+"."+CommonAttributes.TEXTURE_2D,app, true);
//            BufferedImage is = new BufferedImage(128, 128, BufferedImage.TYPE_4BYTE_ABGR);
//            Graphics2D g = (Graphics2D) is.getGraphics();
//            g.setColor(Color.white);
//            g.fillRect(0, 0, is.getWidth(), is.getHeight());
//            g.setColor(Color.black);
//            g.setFont(new Font (Font.SANS_SERIF, Font.PLAIN, 22));
//            g.drawString(""+theText, 10, 10);
//            ImageData id = new ImageData(is);
////            Matrix foo = new Matrix();
////            MatrixBuilder.euclidean().scale(10, 5, 1).assignTo(foo);
////            tex2d.setTextureMatrix(foo);
//            tex2d.setImage(id);
////            tex2d.setRepeatS(Texture2D.GL_CLAMP_TO_EDGE);
////            tex2d.setRepeatT(Texture2D.GL_CLAMP_TO_EDGE);
////            tex2d.setMagFilter(Texture2D.GL_NEAREST);
////            tex2d.setMinFilter(Texture2D.GL_NEAREST);
//
//            tex2d.setMipmapMode(true);

            app.setAttribute(CommonAttributes.TRANSPARENCY_ENABLED, true);
            app.setAttribute(POLYGON_SHADER+"."+TRANSPARENCY, .8);
//            app.setAttribute(CommonAttributes.TEXT_SHADER+"."+TRANSPARENCY_ENABLED, false);
//            app.setAttribute(CommonAttributes.TEXT_SHADER+"."+CommonAttributes.TRANSPARENCY, 0);
            Rectangle3D r = BoundingBoxUtility.calculateChildrenBoundingBox((SceneGraphComponent) destination);
            IndexedFaceSet cubo  = IndexedFaceSetUtility.representAsSceneGraph(r);
            //IndexedFaceSetUtility.calculateAndSetFaceNormals(cubo);
//            int nn = cubo.getNumFaces();
//            String[] labels = new String[nn];
//            for (int i = 0; i < nn; ++i)
//                labels[i] = "";
//            labels[0] = (this.theText);
//            cubo.setFaceAttributes(Attribute.LABELS, 
//                    StorageModel.STRING_ARRAY.createReadOnly(labels));
            c = SceneGraphUtility.createFullSceneGraphComponent("Annotation");
            c.setAppearance(app);
            c.setGeometry(cubo);




            destination.addChild(c);
        }
    }

    
    
    private SceneGraphComponent loadPath(SoftViewerLeaf sw) {
        StringTokenizer st = new StringTokenizer(location, "§");
        SceneGraphComponent r = sw.rootNode;
        String nt = st.nextToken();
        while (st.hasMoreTokens()){

            nt = st.nextToken();
            List<SceneGraphComponent > l = r.getChildComponents();
            SceneGraphComponent nn = null;
            for (SceneGraphComponent c:l) {
                if (c.getName().equals(nt)) {
                    nn = c;
                }
            }
            if (nn == null)
                return null;
            else r = nn;
        }
        return r;
    }
}