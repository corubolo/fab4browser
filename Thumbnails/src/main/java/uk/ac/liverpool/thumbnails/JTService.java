package uk.ac.liverpool.thumbnails;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;

import uk.ac.liv.jt.debug.DebugJTReader;
import uk.ac.liv.jt.segments.LSGSegment;
import uk.ac.liv.jt.viewer.JTReader;
import de.jreality.geometry.BoundingBoxUtility;
import de.jreality.math.MatrixBuilder;
import de.jreality.math.P3;
import de.jreality.math.Pn;
import de.jreality.math.Rn;
import de.jreality.reader.Readers;
import de.jreality.scene.Appearance;
import de.jreality.scene.Camera;
import de.jreality.scene.DirectionalLight;
import de.jreality.scene.Light;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.scene.SceneGraphPath;
import de.jreality.shader.CommonAttributes;
import de.jreality.softviewer.Renderer;
import de.jreality.sunflow.SunflowRenderer;
import de.jreality.util.Input;
import de.jreality.util.Rectangle3D;

public class JTService {

    static boolean reg;

    public static BufferedImage generateJTThumb(URI u, int w, int h)
            throws MalformedURLException, IOException {

        // first register JT adapter
        if (reg == false) {
            Readers.registerReader("JT", JTReader.class);
            // register the file ending .jt for files containing JT-format data
            Readers.registerFileEndings("JT", "jt");
            DebugJTReader.debugCodec = false;
            DebugJTReader.debugMode = false;
            LSGSegment.doRender = false;
            reg = true;
        }
        SceneGraphComponent sgc = Readers.read(new Input(u.toURL()));
        SceneGraphComponent rootNode = new SceneGraphComponent("root");
        SceneGraphComponent cameraNode = new SceneGraphComponent("camera");
        SceneGraphComponent geometryNode = new SceneGraphComponent("geometry");
        SceneGraphComponent lightNode = new SceneGraphComponent("light");
        rootNode.addChild(geometryNode);
        rootNode.addChild(cameraNode);
        cameraNode.addChild(lightNode);

        Light dl = new DirectionalLight();
        lightNode.setLight(dl);
        if (sgc != null)
            geometryNode.addChild(sgc);
        MatrixBuilder.euclidean().translate(0, 0, 3).assignTo(cameraNode);

        Appearance rootApp = new Appearance();
        rootApp.setAttribute(CommonAttributes.BACKGROUND_COLOR, new Color( 1f,1f,1f,0f));
        rootApp.setAttribute(CommonAttributes.DIFFUSE_COLOR, new Color(1f, 0f,
                0f));
        rootApp.setAttribute(CommonAttributes.SMOOTH_SHADING, true);
        rootNode.setAppearance(rootApp);

        Camera camera = new Camera();
        cameraNode.setCamera(camera);
        SceneGraphPath camPath = new SceneGraphPath(rootNode, cameraNode);
        camPath.push(camera);

        Rectangle3D worldBox = BoundingBoxUtility.calculateBoundingBox(sgc);// .
        // bbv.getBoundingBox();

        SceneGraphPath w2a = camPath.popNew();
        w2a.pop();
        double[] w2ava = w2a.getInverseMatrix(null);
        worldBox.transformByMatrix(worldBox, w2ava);
        double[] extent = worldBox.getExtent();

        double ww = (extent[1] > extent[0]) ? extent[1] : extent[0];
        double focus = .5 * ww
                / Math.tan(Math.PI * (camera.getFieldOfView()) / 360.0);

        double[] to = worldBox.getCenter();
        to[2] += extent[2] * .5;
        double[] tofrom = { 0, 0, focus };
        double[] from = Rn.add(null, to, tofrom);

        double[] newCamToWorld = P3.makeTranslationMatrix(null, from,
                Pn.EUCLIDEAN);
        double[] newWorldToCam = Rn.inverse(null, newCamToWorld);
        camPath.getLastComponent().getTransformation().setMatrix(newCamToWorld); // Translation(from);
        double[] centerWorld = Rn.matrixTimesVector(null, newWorldToCam,
                worldBox.getCenter());

        Rectangle3D cameraBox = worldBox.transformByMatrix(null, newWorldToCam);

        double zmin = cameraBox.getMinZ();
        double zmax = cameraBox.getMaxZ();

        if (camera.getFar() > 0.0 && zmax < 0.0 && -zmax > .1 * camera.getFar())
            camera.setFar(-10 * zmax);
        if (zmin < 0.0 && -zmin < 10 * camera.getNear())
            camera.setNear(-.1 * zmin);

        BufferedImage offscreen = new BufferedImage(w, h,
                BufferedImage.TYPE_INT_ARGB);
        
        //SunflowRenderer renderer = new SunflowRenderer();
        Renderer renderer = new Renderer(offscreen);
        renderer.setBestQuality(true);
        renderer.setBackgroundColor(new Color( 1f,1f,1f,0f).getRGB());
        renderer.setCameraPath(camPath);
        renderer.setSceneRoot(rootNode);
        renderer.render();
        renderer.update();
        return offscreen;
        // CameraUtility.encompass(this);

    }
}