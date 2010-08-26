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

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import multivalent.CLGeneral;
import multivalent.INode;
import multivalent.StyleSheet;
import multivalent.node.HR;
import multivalent.node.IParaBox;
import multivalent.node.IVBox;
import multivalent.node.LeafUnicode;
import phelps.awt.Colors;
import uk.ac.liverpool.fab4.behaviors.TimedMedia;

public class AudioDocument extends IVBox implements TimedMedia, Runnable {

    private static final int BUFFERSIZE = 128000;

    private URI uri;

    private Status current = Status.STOP;

    private AudioFileFormat afformat;

    private AudioFormat decodedFormat;

    private AudioFormat fformat;

    private AudioInputStream ainput;

    private AudioInputStream din;

    private SourceDataLine line;

    private Thread playThread;

    private double duration;

    private DurationUnit durationUnit;

    // private Timer timer;

    byte[] buffer;

    private float bps;

    boolean reinit = false;

    double startPos;

    static private int openThreads = 0;

    StyleSheet ss;

    private long prevtt;

    private Boolean global = true;

    /**
	 * 
	 */
    public AudioDocument(String name, Map<String, Object> attr, INode parent,
            URI uri) throws LineUnavailableException {
        super(name, attr, parent);
        this.uri = uri;
        buffer = new byte[BUFFERSIZE];

        current = Status.PLAY;
        ss = parent.getDocument().getStyleSheet();
        init(0);

        play();
        Boolean s;
        s = (Boolean) attr.get("GLOBAL");
        if (s != null) {
            global = s;
        }
        getBrowser().eventq(TimedMedia.MSG_STATUS_CHANGED, current);
    }

    private void init(long pos) {
        new StringBuilder(1000);
        if (pos == 0) {
            startPos = 0;
        }

        // if (timer == null)
        // timer = new Timer(500, this);
        try {
            if (uri.getScheme().equals("file")) {
                File f = new File(uri.getPath());
                if (f.exists()) {
                    afformat = AudioSystem.getAudioFileFormat(f);
                    ainput = AudioSystem.getAudioInputStream(f);

                } else {
                    return;
                }
            } else {
                afformat = AudioSystem.getAudioFileFormat(uri.toURL());
                ainput = AudioSystem.getAudioInputStream(uri.toURL());
            }
            // System.out.println("INIT POS " + pos);

            fformat = afformat.getFormat();
            if (afformat.getFrameLength() != -1) {
                duration = afformat.getFrameLength() / fformat.getFrameRate();
                getBrowser().eventq(TimedMedia.MSG_GOT_DURATION, duration);
            } else {
                duration = -1;
            }
            bps = afformat.getByteLength() / afformat.getFrameLength()
                    * fformat.getFrameRate();
            // System.out.println(bps);

            durationUnit = DurationUnit.SEC;

            decodedFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                    fformat.getSampleRate(), 16, fformat.getChannels(), fformat
                            .getChannels() * 2, fformat.getSampleRate(), false);
            din = AudioSystem.getAudioInputStream(decodedFormat, ainput);
            if (pos != 0) {
                try {
                    din.skip(pos);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            if (!reinit) {
                writeMetadata();
            }

            line = getLine(decodedFormat);
            reinit = true;
            return;
        } catch (MalformedURLException e) {

            e.printStackTrace();
        } catch (UnsupportedAudioFileException e) {
            putLine("Unsupported format!", null);
            e.printStackTrace();
            close();
        } catch (IOException e) {
            putLine("Unsupported format!", null);
            e.printStackTrace();
            close();
        } catch (LineUnavailableException e) {
            putLine("Audio Line unavaliable!", null);
            close();
            e.printStackTrace();
        }
        return;
    }

    private void writeMetadata() {
        Map<String, Object> om = new HashMap<String, Object>(afformat
                .properties());
        if (om == null) {
            return;
        }
        CLGeneral gs = new CLGeneral();
        gs.setForeground(Colors.getColor(getAttr("foreground"), Color.YELLOW));
        gs.setBackground(Colors.getColor(getAttr("background"), Color.BLACK));
        gs.setPadding(8);
        gs.setSize(32.0f);
        CLGeneral gs3 = new CLGeneral();
        gs3.setForeground(Colors.getColor(getAttr("foreground"), Color.YELLOW));
        gs3.setBackground(Colors.getColor(getAttr("background"), Color.BLACK));
        gs3.setPadding(5);
        gs3.setSize(28.0f);
        CLGeneral gs2 = new CLGeneral();
        gs2.setForeground(Colors.getColor(getAttr("foreground"), Color.WHITE));
        gs2.setBackground(Colors.getColor(getAttr("background"), Color.BLACK));
        gs2.setPadding(3);
        gs2.setSize(20.0f);
        String s;
        Set<Entry<String, Object>> i;
        s = remove(om, "author");
        if (s != null) {
            putLine("" + s, gs);
        }
        s = remove(om, "title");
        if (s != null) {
            putLine("" + s, gs3);
        }
        s = remove(om, "album");
        if (s != null) {
            putLine("" + s, gs3);
        }
        if (duration != -1) {
            int sec = (int) duration % 60;
            int min = (int) duration / 60;
            int hour = min / 60;
            min %= 60;
            s = "" + hour + ":" + (min < 10 ? "0" + min : "" + min) + ":"
                    + (sec < 10 ? "0" + sec : "" + sec);
            putLine("" + s, gs2);
        }
        new HR("hr", null, this);
        putLine("", gs2);
        i = om.entrySet();
        for (Entry<String, Object> od : i) {
            putLine(od.getKey() + " = " + od.getValue(), null);
        }

    }

    private String remove(Map<String, Object> om, String key) {
        String s = null;
        Object o;
        if ((o = om.remove(key)) != null) {
            s = o.toString();
        }
        return s;
    }

    private void putLine(String line, CLGeneral gs) {
        INode paraNode = null;
        INode lineNode = null;

        StringBuffer sb = new StringBuffer(line);
        paraNode = new IVBox("para", null, this);
        lineNode = new IParaBox("Line", null, paraNode); // IHBox?
        int start = 0;
        for (int i = 0, imax = sb.length(); i < imax; i++) {
            char wch = sb.charAt(i);
            if (wch == '\t') { // tabs get own word
                if (i > start) {
                    new LeafUnicode(sb.substring(start, i), null, lineNode);
                }
                new LeafUnicode(sb.substring(i, i + 1), null, lineNode);
                start = i + 1;
            } else if (wch == ' ') {
                if (i > start) {
                    String txt = i - start == 1 ? phelps.lang.Strings
                            .valueOf(sb.charAt(start)) : sb.substring(start, i);
                    new LeafUnicode(txt, null, lineNode);
                }
                start = i + 1;
            }
        }
        if (start < sb.length()) {
            new LeafUnicode(sb.substring(start), null, lineNode);
        }
        if (gs != null && paraNode != null) {
            ss.put(paraNode, gs);
        }
    }

    public void run() {
        try {
            if (line != null) {
                line.start();
                int nBytesRead = 0;

                while (nBytesRead != -1 && playThread != null
                        && current == Status.PLAY) {
                    if (current == Status.STOP) {
                        break;
                    }
                    if (din != null) {
                        nBytesRead = din.read(buffer, 0, buffer.length);
                    } else {
                        break;
                    }
                    if (current == Status.STOP) {
                        break;
                    }
                    if (nBytesRead != -1 && line != null) {
                        line.write(buffer, 0, nBytesRead);
                    }
                    if (line == null) {
                        break;
                    }

                    long tt = System.currentTimeMillis() - prevtt;
                    if (tt >= 500) {
                        prevtt = System.currentTimeMillis();
                        double position = (line.getMicrosecondPosition() / 1000000.0)
                                + startPos;

                        double p2 = position / duration;
                        getBrowser().eventq(TimedMedia.MSG_PlAYTIMEPERCENT, p2);
                        // Fab4 ff = Fab4.getMVFrame(getBrowser());
                        // //
                        // //System.out.println(ff.pmedia.sl.getValueIsAdjusting());
                        // //
                        // //System.out.println(SwingUtilities.isEventDispatchThread());
                        // if (!ff.pmedia.sl.getValueIsAdjusting()){
                        // // //ff.pmedia.sl.setValueIsAdjusting(true);
                        // ff.pmedia.ticking = true;
                        // ff.pmedia.sl.setValue((int)(p2 * 100.d));
                        // ff.pmedia.ticking = false;
                        // // //ff.pmedia.sl.setValueIsAdjusting(false);
                        // }
                        getBrowser().eventq(TimedMedia.MSG_PlAYTIME, position);
                    }

                }
                System.out.println("Loop and thread end! because: "
                        + nBytesRead + playThread + current);
                if (current != Status.PAUSE && current != Status.STOP) {
                    if (line != null) {
                        line.drain();
                        line.stop();
                        line.close();
                    }
                    if (din != null) {
                        din.close();
                    }
                    if (ainput != null) {
                        ainput.close();
                    }
                    current = Status.STOP;
                    // if (timer != null)
                    // timer.stop();
                }
                getBrowser().eventq(TimedMedia.MSG_STATUS_CHANGED, current);
                playThread = null;

            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            openThreads--;
            // System.out.println(openThreads);
        }
    }

    private SourceDataLine getLine(AudioFormat audioFormat)
            throws LineUnavailableException {
        SourceDataLine res = null;
        DataLine.Info info = new DataLine.Info(SourceDataLine.class,
                audioFormat);
        res = (SourceDataLine) AudioSystem.getLine(info);
        res.open(audioFormat);
        return res;
    }

    public double getDuration() {
        return duration;
    }

    public DurationUnit getDurationUnit() {
        return durationUnit;
    }

    public double getPosition() {
        return (line.getMicrosecondPosition() / 1000000.0);
    }

    public DurationUnit getPositionUnit() {
        return DurationUnit.SEC;
    }

    public Status getStatus() {
        return current;
    }

    public boolean setPosition(double d) {
        // line.stop();
        // timer.stop();
        // line.flush();
        // //current = Status.PAUSE;
        setStatus(Status.STOP);
        double newSeconds = duration * d;
        startPos = newSeconds;
        long newBytes = (long) (newSeconds * bps);
        System.out.println("  perc: " + d + " to sec " + newSeconds
                + " new bytes " + newBytes + " on length "
                + afformat.getByteLength());

        current = Status.PLAY;
        init(newBytes);
        play();
        getBrowser().eventq(TimedMedia.MSG_STATUS_CHANGED, current);
        double position = (line.getMicrosecondPosition() / 1000000.0)
                + startPos;

        double p3 = position / duration;
        // System.out.println(p2 + " " + position);
        getBrowser().eventq(TimedMedia.MSG_PlAYTIMEPERCENT, p3);
        getBrowser().eventq(TimedMedia.MSG_PlAYTIME, position);
        getBrowser().eventq(TimedMedia.MSG_STATUS_CHANGED, current);
        // try {
        // ainput.skip(newBytes);
        // } catch (IOException e) {
        // // TODO Auto-generated catch block
        // e.printStackTrace();
        // }
        // //current = Status.PLAY;
        // System.out.println(p2 + " " + newSeconds);
        // getBrowser().eventq(TimedMedia.MSG_PlAYTIMEPERCENT, p2);
        // getBrowser().eventq(TimedMedia.MSG_PlAYTIME, newSeconds);
        // if (playThread == null || !playThread.isAlive()) {
        // play();
        // } else {
        // line.start();
        // timer.start();
        // }
        // getBrowser().eventq(TimedMedia.MSG_STATUS_CHANGED, current);
        return false;
    }

    void close() {

        setStatus(Status.STOP);
        // timer = null;
        // getBrowser().eventq(TimedMedia.MSG_STATUS_CHANGED, current);
    }

    private void stop() {

        if (line != null) {
            line.stop();
            line.close();
        }
        try {
            if (ainput != null) {
                ainput.close();
            }
            if (din != null) {
                din.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        ainput = null;
        din = null;
        line = null;
        playThread = null;
        // timer.stop();
    }

    private void play() {
        if (playThread == null) {
            playThread = new Thread(this, "Audio Playback");
        }
        playThread.start();
        // timer.start();
        openThreads++;
        // System.out.println(openThreads);
    }

    public Status setStatus(Status st) {
        Status ps = current;
        switch (st) {
        case STOP:
            // System.out.println("STOP");
            if (current != Status.STOP) {
                current = Status.STOP;
            }
            stop();
            break;
        case PLAY:
            if (current == Status.STOP) {
                // System.out.println("PLAY");
                current = Status.PLAY;
                init(0);
                play();
            } else if (current == Status.PAUSE) {
                // System.out.println("PLAY - pause");
                current = Status.PLAY;
                if (playThread == null || !playThread.isAlive()) {
                    play();
                } else {
                    line.start();
                    // timer.start();
                }
            }
            break;
        case PAUSE:
            if (current == Status.PLAY) {
                // System.out.println("PAUSE");
                // ine.flush();
                line.stop();
                // timer.stop();
                current = Status.PAUSE;

            }
            break;
        default:
            break;
        }
        getBrowser().eventq(TimedMedia.MSG_STATUS_CHANGED, current);
        return ps;
    }

    public boolean getDisplayGlobalUI() {
        return global;
    }

    // public void actionPerformed(ActionEvent e) {
    // if (e.getSource() == timer && line != null) {
    // double position = (double) (((double) line.getMicrosecondPosition()) /
    // 1000000.0)
    // + startPos;
    //
    // double p2 = position / duration;
    // // System.out.println(p2 + " " + position);
    // getBrowser().eventq(TimedMedia.MSG_PlAYTIMEPERCENT, p2);
    // getBrowser().eventq(TimedMedia.MSG_PlAYTIME, position);
    // }
    //
    // }

}
