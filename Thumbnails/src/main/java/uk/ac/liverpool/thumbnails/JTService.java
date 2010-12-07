/*******************************************************************************
 * This Library is :
 * 
 *     Copyright © 2010 Fabio Corubolo - all rights reserved
 *     corubolo@gmail.com
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published
 *     by the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * see COPYING.LESSER.txt
 * 
 ******************************************************************************/
package uk.ac.liverpool.thumbnails;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
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

public class JTService implements GenericService{

    static boolean reg;
    public static final String JT_MIME = "application/x-jt";
    public BufferedImage generateThumb(URI u, File f,  int w, int h, int page)
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
        Input in;
        if (f!=null)
            in = new Input(f);
        else 
            in = new Input(u.toURL());
        SceneGraphComponent sgc = Readers.read(in);
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

    @Override
    public String[] getSupportedMimes() {
        return new String[]{JT_MIME};
    }

    @Override
    public FontInformation[] extractFontList(URI u, File fff)
            throws MalformedURLException, IOException {
        return null;
    }

    @Override
    public String extraxtXMLText(URI u, File f) throws MalformedURLException,
            IOException {
        return null;
    }

    @Override
    public void generateSVG(URI u, File f, int w, int h, int page, Writer out)
            throws MalformedURLException, IOException {
        // TODO Auto-generated method stub
        return ;
    }
}
