/*******************************************************************************
 *
 *  * Copyright (C) 2007, 2010 - The University of Liverpool
 *  * This program is free software; you can redistribute it and/or modify it under the terms 
 *  * of the GNU General Public License as published by the Free Software Foundation; either version 3 of the License, 
 *  * or (at your option) any later version.
 *  * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied 
 *  * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *  * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *  * 
 *  * Author: Fabio Corubolo
 *  * Email: corubolo@gmail.com
 *  
 *******************************************************************************/

package uk.ac.liverpool.fab4;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.net.URL;
import java.util.Vector;

import com.fluendo.jst.Caps;
import com.fluendo.jst.CapsListener;
import com.fluendo.jst.Clock;
import com.fluendo.jst.Element;
import com.fluendo.jst.ElementFactory;
import com.fluendo.jst.Format;
import com.fluendo.jst.Message;
import com.fluendo.jst.Pad;
import com.fluendo.jst.PadListener;
import com.fluendo.jst.Pipeline;
import com.fluendo.jst.Query;
import com.fluendo.utils.Debug;

public class Fab4Pipeline extends Pipeline implements PadListener, CapsListener {

    private String url;
    private String userId;
    private String password;
    private boolean enableAudio;
    private boolean enableVideo;
    private boolean ignoreAspect;
    private int enableKate;
    private LeafVideo component;
    private int bufferSize = -1;
    private int bufferLow = -1;
    private int bufferHigh = -1;
    private URL documentBase = null;

    private Element httpsrc;
    private Element buffer;
    private Element demux;
    private Element videodec;
    private Element audiodec;
    private Element videosink;
    private Element audiosink;
    private Element v_queue, v_queue2, a_queue = null;
    private Element overlay;
    private Pad asinkpad, ovsinkpad, oksinkpad;
    private Pad apad, vpad;
    private Vector katedec = new Vector();
    private Vector k_queue = new Vector();
    private Element kselector = null;

    public boolean usingJavaX = false;

    private boolean setupVideoDec(String name) {
        videodec = ElementFactory.makeByName(name, "videodec");
        if (videodec == null) {
            noSuchElement(name);
            return false;
        }
        add(videodec);
        return true;
    }

    public void padAdded(Pad pad) {
        Caps caps = pad.getCaps();

        if (caps == null) {
            Debug.log(Debug.INFO, "pad added without caps: " + pad);
            return;
        }
        Debug.log(Debug.INFO, "pad added " + pad);

        String mime = caps.getMime();

        if (enableAudio && mime.equals("audio/x-vorbis")) {
            if (a_queue != null) {
                Debug
                        .log(Debug.INFO,
                                "More than one audio stream detected, ignoring all except first one");
                return;
            }
            a_queue = ElementFactory.makeByName("queue", "a_queue");
            if (a_queue == null) {
                noSuchElement("queue");
                return;
            }

            // if we already have a video queue: We want smooth audio playback
            // over frame completeness, so make the video queue leaky
            if (v_queue != null) {
                v_queue.setProperty("leaky", "2"); // 2 == Queue.LEAK_DOWNSTREAM
            }

            audiodec = ElementFactory.makeByName("vorbisdec", "audiodec");
            if (audiodec == null) {
                noSuchElement("vorbisdec");
                return;
            }
            a_queue.setProperty("maxBuffers", "100");
            add(a_queue);
            add(audiodec);

            pad.link(a_queue.getPad("sink"));
            a_queue.getPad("src").link(audiodec.getPad("sink"));
            if (!audiodec.getPad("src").link(asinkpad)) {
                postMessage(Message.newError(this, "audiosink already linked"));
                return;
            }

            apad = pad;

            audiodec.setState(PAUSE);
            a_queue.setState(PAUSE);
        } else if (enableVideo && mime.equals("video/x-theora")) {
            v_queue = ElementFactory.makeByName("queue", "v_queue");
            v_queue2 = ElementFactory.makeByName("queue", "v_queue2");
            if (v_queue == null) {
                noSuchElement("queue");
                return;
            }

            if (!setupVideoDec("theoradec")) {
                return;
            }
            // if we have audio: We want smooth audio playback
            // over frame completeness
            if (a_queue != null) {
                v_queue.setProperty("leaky", "2"); // 2 == Queue.LEAK_DOWNSTREAM
            }
            v_queue.setProperty("maxBuffers", "175");
            v_queue2.setProperty("maxBuffers", "1");

            add(v_queue);
            add(v_queue2);

            pad.link(v_queue.getPad("sink"));
            v_queue.getPad("src").link(videodec.getPad("sink"));
            videodec.getPad("src").link(v_queue2.getPad("sink"));
            if (!v_queue2.getPad("src").link(ovsinkpad)) {
                postMessage(Message.newError(this, "videosink already linked"));
                return;
            }

            vpad = pad;

            videodec.setState(PAUSE);
            v_queue.setState(PAUSE);
            v_queue2.setState(PAUSE);
        } else if (enableVideo && mime.equals("image/jpeg")) {
            if (!setupVideoDec("jpegdec")) {
                return;
            }
            videodec.setProperty("component", component);

            pad.link(videodec.getPad("sink"));
            if (!videodec.getPad("src").link(ovsinkpad)) {
                postMessage(Message.newError(this, "videosink already linked"));
                return;
            }

            videodec.setState(PAUSE);
        } else if (enableVideo && mime.equals("video/x-smoke")) {
            if (!setupVideoDec("smokedec")) {
                return;
            }
            videodec.setProperty("component", component);

            pad.link(videodec.getPad("sink"));
            if (!videodec.getPad("src").link(ovsinkpad)) {
                postMessage(Message.newError(this, "videosink already linked"));
                return;
            }
            vpad = pad;

            videodec.setState(PAUSE);
        } else if (enableVideo && mime.equals("application/x-kate")) {
            Element tmp_k_queue, tmp_katedec, tmp_katesink;
            Pad tmp_kpad, tmp_ksinkpad;
            int kate_index = katedec.size();

            Debug.debug("Found Kate stream, setting up pipeline branch");

            /* dynamically create a queue/decoder/overlay pipeline */
            tmp_k_queue = ElementFactory.makeByName("queue", "k_queue"
                    + kate_index);
            if (tmp_k_queue == null) {
                noSuchElement("queue");
                return;
            }

            tmp_katedec = ElementFactory.makeByName("katedec", "katedec"
                    + kate_index);
            if (tmp_katedec == null) {
                noSuchElement("katedec");
                return;
            }

            /* The selector is created when the first Kate stream is encountered */
            if (kselector == null) {
                Debug.debug("No Kate selector yet, creating one");

                /* insert an overlay before the video sink */
                ovsinkpad.unlink();
                videodec.getPad("src").unlink();
                overlay = ElementFactory.makeByName("kateoverlay", "overlay");
                if (overlay == null) {
                    noSuchElement("overlay");
                    return;
                }
                ovsinkpad = overlay.getPad("videosink");
                oksinkpad = overlay.getPad("katesink");
                if (!videodec.getPad("src").link(ovsinkpad)) {
                    postMessage(Message.newError(this,
                            "Failed linking video decoder to overlay"));
                    return;
                }
                add(overlay);
                overlay.setProperty("component", component);
                overlay.getPad("videosrc").link(videosink.getPad("sink"));

                kselector = ElementFactory.makeByName("selector", "selector");
                if (kselector == null) {
                    noSuchElement("selector");
                    return;
                }
                add(kselector);
                if (!kselector.getPad("src").link(oksinkpad)) {
                    postMessage(Message.newError(this,
                            "Failed linking Kate selector to overlay"));
                    return;
                }
                kselector.setState(PAUSE);
            }
            tmp_katesink = kselector;

            add(tmp_k_queue);
            add(tmp_katedec);

            tmp_kpad = pad;
            tmp_ksinkpad = tmp_katesink.getPad("sink");

            /* link new elements together */
            if (!pad.link(tmp_k_queue.getPad("sink"))) {
                postMessage(Message.newError(this,
                        "Failed to link new Kate stream to queue"));
                return;
            }
            if (!tmp_k_queue.getPad("src").link(tmp_katedec.getPad("sink"))) {
                postMessage(Message.newError(this,
                        "Failed to link new Kate queue to decoder"));
                return;
            }

            Pad new_selector_pad = kselector.requestSinkPad(tmp_katedec
                    .getPad("src"));
            if (!tmp_katedec.getPad("src").link(new_selector_pad)) {
                postMessage(Message.newError(this, "kate sink already linked"));
                return;
            }

            tmp_katedec.setState(PAUSE);
            tmp_k_queue.setState(PAUSE);

            /* add to the lists */
            katedec.addElement(tmp_katedec);
            k_queue.addElement(tmp_k_queue);

            /* if we have just added the one that was selected, link it now */
            if (enableKate == katedec.size() - 1) {
                doEnableKateIndex(enableKate);
            }
        }
    }

    public void padRemoved(Pad pad) {
        pad.unlink();
        if (pad == vpad) {
            Debug.log(Debug.INFO, "video pad removed " + pad);
            ovsinkpad.unlink();
            vpad = null;
        } else if (pad == apad) {
            Debug.log(Debug.INFO, "audio pad removed " + pad);
            asinkpad.unlink();
            apad = null;
        }
    }

    public void noMorePads() {
        boolean changed = false;
        Element el;

        Debug.log(Debug.INFO, "all streams detected");

        if (apad == null && enableAudio) {
            Debug.log(Debug.INFO, "file has no audio, remove audiosink");
            audiosink.setState(STOP);
            remove(audiosink);
            audiosink = null;
            changed = true;
        }
        if (vpad == null && enableVideo) {
            Debug.log(Debug.INFO, "file has no video, remove videosink");
            videosink.setState(STOP);
            if (overlay != null) {
                overlay.setState(STOP);
            }
            for (int n = 0; n < katedec.size(); ++n) {
                el = (Element) katedec.elementAt(n);
                el.setState(STOP);
                remove(el);
                el = (Element) k_queue.elementAt(n);
                el.setState(STOP);
                remove(el);
            }
            if (kselector != null) {
                kselector.setState(STOP);
                remove(kselector);
                kselector = null;
            }
            remove(videosink);
            remove(overlay);
            katedec.removeAllElements();
            k_queue.removeAllElements();
            videosink = null;
            overlay = null;
            changed = true;
        }
        if (changed) {
            scheduleReCalcState();
        }
    }

    public Fab4Pipeline(LeafVideo videoLeaf) {
        super("pipeline");
        enableAudio = true;
        enableVideo = true;
        component = videoLeaf;
        enableKate = -1; /* none by default */
    }

    public void setUrl(String anUrl) {
        url = anUrl;
    }

    public String getUrl() {
        return url;
    }

    public void setUserId(String aUserId) {
        userId = aUserId;
    }

    public void setPassword(String aPassword) {
        password = aPassword;
    }

    public void enableAudio(boolean b) {
        enableAudio = b;
    }

    public boolean isAudioEnabled() {
        return enableAudio;
    }

    public void enableVideo(boolean b) {
        enableVideo = b;
    }

    public boolean isVideoEnabled() {
        return enableVideo;
    }

    public void setComponent(LeafVideo c) {
        component = c;
    }

    /**
     * Finds a Kate stream from the given language and category names. Returns a
     * negative index if none is found.
     */
    private int findKateStream(String language, String category) {
        int idx = -1;

        boolean has_language = !language.equals("");
        boolean has_category = !category.equals("");
        if (has_language || has_category) {
            for (int n = 0; n < katedec.size(); ++n) {
                Element e = (Element) katedec.elementAt(n);
                if (e != null) {
                    String e_language = String.valueOf(e
                            .getProperty("language"));
                    String e_category = String.valueOf(e
                            .getProperty("category"));
                    if (language.equalsIgnoreCase(e_language)) {
                        if (!has_category || category.equals(e_category)) {
                            idx = n;
                        }
                    }
                }
            }
        }
        return idx;
    }

    /**
     * Selects the Kate stream to enable, by index (if any), or by
     * language/category. The first Kate stream has index 0, the second has
     * index 1, etc. A negative index and empty language/category strings will
     * enable none.
     */
    public void enableKateStream(int idx, String language, String category) {
        if (idx < 0) {
            idx = findKateStream(language, category);
        }
        if (idx == enableKate) {
            return;
        }
        doEnableKateIndex(idx);
    }

    /**
     * Enables the given Kate stream, by index. A negative index will enable
     * none.
     */
    private void doEnableKateIndex(int idx) {
        if (kselector != null) {
            Debug.info("Switching Kate streams from " + enableKate + " to "
                    + idx);
            kselector.setProperty("selected", new Integer(idx));
        } else {
            Debug
                    .warning("Switching Kate stream request, but no Kate selector exists");
        }

        enableKate = idx;
    }

    /**
     * Returns the index of the currently enabled Kate stream (negative if none)
     */
    public int getEnabledKateIndex() {
        return enableKate;
    }

    public LeafVideo getComponent() {
        return component;
    }

    public void setDocumentBase(URL base) {
        documentBase = base;
    }

    public URL getDocumentBase() {
        return documentBase;
    }

    public void setBufferSize(int size) {
        bufferSize = size;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public void setBufferLow(int size) {
        bufferLow = size;
    }

    public int getBufferLow() {
        return bufferLow;
    }

    public void setBufferHigh(int size) {
        bufferHigh = size;
    }

    public int getBufferHigh() {
        return bufferHigh;
    }

    public void resize(Dimension d) {
        if (videosink == null || d == null) {
            return;
        }
        Rectangle bounds = new Rectangle(d);
        // if (application.getShowStatus() == Cortado.STATUS_SHOW) {
        // bounds.height -= application.getStatusHeight();
        // }
        if (bounds.height < 0) {
            bounds.height = 0;
        }
        videosink.setProperty("bounds", bounds);
    }

    public boolean buildOgg() {

        demux = ElementFactory.makeByName("oggdemux", "demux");
        if (demux == null) {
            noSuchElement("oggdemux");
            return false;
        }

        buffer = ElementFactory.makeByName("queue", "buffer");
        if (buffer == null) {
            demux = null;
            noSuchElement("queue");
            return false;
        }
        buffer.setProperty("isBuffer", Boolean.TRUE);
        if (bufferSize != -1) {
            buffer.setProperty("maxSize", new Integer(bufferSize * 1024));
        }
        if (bufferLow != -1) {
            buffer.setProperty("lowPercent", new Integer(bufferLow));
        }
        if (bufferHigh != -1) {
            buffer.setProperty("highPercent", new Integer(bufferHigh));
        }

        add(demux);
        add(buffer);

        httpsrc.getPad("src").link(buffer.getPad("sink"));
        buffer.getPad("src").link(demux.getPad("sink"));
        demux.addPadListener(this);

        buffer.setState(PAUSE);
        demux.setState(PAUSE);

        return true;
    }

    public boolean buildMultipart() {
        demux = ElementFactory.makeByName("multipartdemux", "demux");
        if (demux == null) {
            noSuchElement("multipartdemux");
            return false;
        }
        add(demux);

        httpsrc.getPad("src").link(demux.getPad("sink"));

        demux.addPadListener(this);

        return true;
    }

    public void capsChanged(Caps caps) {
        String mime = caps.getMime();

        if (mime.equals("application/ogg")) {
            buildOgg();
        } else if (mime.equals("multipart/x-mixed-replace")) {
            buildMultipart();
        } else {
            postMessage(Message.newError(this, "unknown type: " + mime));
        }
    }

    private void noSuchElement(String elemName) {
        postMessage(Message.newError(this, "no such element: " + elemName
                + " (check plugins.ini)"));
    }

    private boolean build() {
        String userAgent;
        String extra;
        String vendor = System.getProperty("java.vendor");

        httpsrc = ElementFactory.makeByName("httpsrc", "httpsrc");
        if (httpsrc == null) {
            noSuchElement("httpsrc");
            return false;
        }

        httpsrc.setProperty("url", url);
        httpsrc.setProperty("userId", userId);
        httpsrc.setProperty("password", password);

        userAgent = "Cortado/" + "/" + System.getProperty("java.version");

        extra = "(" + System.getProperty("os.name") + " "
                + System.getProperty("os.version") + ")";

        try {
            String agent = System.getProperty("http.agent");
            if (agent != null) {
                extra = agent;
            }
        } catch (Exception e) {
        }
        userAgent += " " + extra;

        Debug.log(Debug.INFO, "setting User-Agent " + userAgent);

        httpsrc.setProperty("userAgent", userAgent);
        httpsrc.setProperty("documentBase", documentBase);
        add(httpsrc);

        httpsrc.getPad("src").addCapsListener(this);

        if (enableAudio) {
            audiosink = newAudioSink();
            if (audiosink == null) {
                enableAudio = false; // to suppress creation of audio decoder
                // pads
                // ((Cortado)component).status.setHaveAudio(false);
                component.repaint();
                // Don't stop unless video is disabled too
            } else {
                asinkpad = audiosink.getPad("sink");
                add(audiosink);
            }
        }
        if (enableVideo) {
            videosink = ElementFactory.makeByName("leafvideosink",
                    "leafvideosink");
            if (videosink == null) {
                noSuchElement("leafvideosink");
                return false;
            }
            videosink.setProperty("component", component);
            resize(component.getBbox().getSize());

            videosink.setProperty("max-lateness", Long
                    .toString(Clock.MSECOND * 20));
            add(videosink);

            ovsinkpad = videosink.getPad("sink");

        }
        if (audiosink == null && videosink == null) {
            postMessage(Message.newError(this,
                    "Both audio and video are disabled, can't play anything"));
            return false;
        }

        return true;
    }

    protected Element newAudioSink() {
        com.fluendo.plugin.AudioSink s;
        try {
            Class.forName("javax.sound.sampled.AudioSystem");
            Class.forName("javax.sound.sampled.DataLine");
            usingJavaX = true;
            s = (com.fluendo.plugin.AudioSink) ElementFactory.makeByName(
                    "audiosinkj2", "audiosink");
            Debug.log(Debug.INFO, "using high quality javax.sound backend");
        } catch (Throwable e) {
            s = (com.fluendo.plugin.AudioSink) ElementFactory.makeByName(
                    "audiosinksa", "audiosink");
            Debug.log(Debug.INFO, "using low quality sun.audio backend");
        }
        if (s == null) {
            noSuchElement("audiosink");
            return null;
        }
        if (!s.test()) {
            return null;
        } else {
            return s;
        }
    }

    private boolean cleanup() {
        int n;
        Debug.log(Debug.INFO, "cleanup");
        if (httpsrc != null) {
            remove(httpsrc);
            httpsrc = null;
        }
        if (audiosink != null) {
            remove(audiosink);
            audiosink = null;
            asinkpad = null;
        }
        if (videosink != null) {
            remove(videosink);
            videosink = null;
        }
        if (overlay != null) {
            remove(overlay);
            overlay = null;
            ovsinkpad = null;
            oksinkpad = null;
        }
        if (buffer != null) {
            remove(buffer);
            buffer = null;
        }
        if (demux != null) {
            demux.removePadListener(this);
            remove(demux);
            demux = null;
        }
        if (v_queue != null) {
            remove(v_queue);
            v_queue = null;
        }
        if (v_queue2 != null) {
            remove(v_queue2);
            v_queue2 = null;
        }
        if (a_queue != null) {
            remove(a_queue);
            a_queue = null;
        }
        if (videodec != null) {
            remove(videodec);
            videodec = null;
        }
        if (audiodec != null) {
            remove(audiodec);
            audiodec = null;
        }

        for (n = 0; n < katedec.size(); ++n) {
            if (k_queue.elementAt(n) != null) {
                remove((Element) k_queue.elementAt(n));
            }
            if (katedec.elementAt(n) != null) {
                remove((Element) katedec.elementAt(n));
            }
        }
        k_queue.removeAllElements();
        katedec.removeAllElements();

        if (kselector != null) {
            remove(kselector);
            kselector = null;
        }
        return true;
    }

    protected int changeState(int transition) {
        int res;

        switch (transition) {
        case STOP_PAUSE:
            if (!build()) {
                return FAILURE;
            }
            break;
        default:
            break;
        }

        res = super.changeState(transition);

        switch (transition) {
        case PAUSE_STOP:
            cleanup();
            break;
        default:
            break;
        }

        return res;
    }

    protected boolean doSendEvent(com.fluendo.jst.Event event) {
        boolean res;

        if (event.getType() != com.fluendo.jst.Event.SEEK) {
            return false;
        }

        if (event.parseSeekFormat() != Format.PERCENT) {
            return false;
        }

        if (httpsrc == null) {
            return false;
        }

        res = httpsrc.getPad("src").sendEvent(event);
        getState(null, null, -1);

        return res;
    }

    // protected long getPosition() {
    // Query q;
    // long result = 0;
    //
    // q = Query.newPosition(Format.TIME);
    // if (super.query (q)) {
    // result = q.parsePositionValue ();
    // }
    // return result;
    // }
    // long positionFirst = 0;
    // long prevBytes;
    // private double bytesPerTimenuint = 0;
    // private double bytesadd = 0;
    //
    //  
    // protected double getPositionPercent() {
    // if (httpsrc!=null){
    // if (httpsrc instanceof HTTPSrc) {
    // HTTPSrc s = (HTTPSrc) httpsrc;
    // if (positionFirst == 0 )
    // positionFirst = s.getOffset();
    // else if (bytesPerTimenuint == 0){
    // bytesPerTimenuint = s.getOffset() - prevBytes;
    // }
    // prevBytes = s.getOffset();
    // //System.out.println(s.getOffset()+ " " + s.getContentLength() + " " +
    // (bufferHigh) + " " + bufferSize * 1024);
    // if (s.getOffset() == s.getContentLength()) {
    //				
    // double d = ((double)s.getOffset() - positionFirst + bytesadd)/
    // s.getContentLength();
    // bytesadd +=bytesPerTimenuint;
    // if (d > 1.0d)
    // return 1;
    // return d;
    //				
    // }
    // double d = ((double)s.getOffset() - positionFirst)/ s.getContentLength();
    // if (d > 1.0d)
    // return 1;
    // return d;
    // }
    // }
    // return 0;
    // }

    protected double getPositionPercent() {

        return (double) getPosition() / (double) getDuration();
    }

    protected long getPosition() {
        Query q;
        long result = 0;

        q = Query.newPosition(Format.TIME);
        if (super.query(q)) {
            result = q.parsePositionValue();
        }
        return result;
    }

    protected long getDuration() {
        Query q;
        long result = 0;

        q = Query.newDuration(Format.TIME);
        if (super.query(q)) {
            result = q.parseDurationValue();
        }
        return result;
    }
}
