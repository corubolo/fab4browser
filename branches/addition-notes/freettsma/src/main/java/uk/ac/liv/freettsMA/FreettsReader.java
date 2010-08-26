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
package uk.ac.liv.freettsMA;

import com.sun.speech.freetts.FreeTTSSpeakableImpl;
import com.sun.speech.freetts.Voice;
import com.sun.speech.freetts.VoiceManager;

public class FreettsReader {

    Voice theVoice = null;

    float rate = -1;
    float volume = -1;

    public synchronized void readText(final String toRead) {

        if (theVoice != null) {
            theVoice.getAudioPlayer().cancel();
            theVoice.deallocate();
        }
        theVoice = VoiceManager.getInstance().getVoice("kevin16");
        System.out.println(theVoice);
        final Voice helloVoice = theVoice;
        if (helloVoice == null) {
            return;
        }
        if (rate != -1) {
            helloVoice.setRate(rate);
        }
        if (volume != -1) {
            helloVoice.setVolume(volume);
        }
        helloVoice.allocate();
        Thread t = new Thread(new Runnable() {
            public void run() {
                FreeTTSSpeakableImpl ttr = new FreeTTSSpeakableImpl(toRead);
                helloVoice.speak(ttr);
                helloVoice.deallocate();
                theVoice = null;
            }

        });
        t.start();
    }

    public synchronized void pause() {
        if (theVoice != null) {
            theVoice.getAudioPlayer().pause();
        }
    }

    public synchronized void resume() {
        if (theVoice != null) {
            theVoice.getAudioPlayer().resume();
        }
    }

    public synchronized void stop() {
        if (theVoice != null) {
            theVoice.getAudioPlayer().cancel();
            theVoice.deallocate();
            theVoice = null;
        }

    }

    // public synchronized void getPercent() {
    // if (theVoice!=null) {
    // theVoice.getOutputQueue().
    // }
    //		
    // }

    public void setRate(float wpm) {
        if (theVoice != null) {
            theVoice.setRate(wpm);
            rate = wpm;
        }
    }

    public float getRate() {
        if (theVoice != null) {
            return theVoice.getRate();
        } else {
            return -1;
        }
    }

    public float getVolume() {
        if (theVoice != null) {
            return theVoice.getVolume();
        } else {
            return -1;
        }
    }

    public void setVolume(float v) {
        if (theVoice != null) {
            theVoice.getAudioPlayer().setVolume(v);
        }
        volume = v;

    }

    public static void main(String[] args) {

        String voiceName = "kevin16";
        System.setProperty("freetts.voices",
                "com.sun.speech.freetts.en.us.cmu_us_kal.KevinVoiceDirector");
        VoiceManager voiceManager = VoiceManager.getInstance();
        Voice[] voices = voiceManager.getVoices();
        System.out.println(voices);
        final Voice helloVoice = voiceManager.getVoice(voiceName);
        if (helloVoice == null) {
            return;
        }

        /*
         * Allocates the resources for the voice.
         */
        helloVoice.allocate();

        /*
         * Synthesize speech.
         */
        System.out.println("1");
        Thread t = new Thread(new Runnable() {

            public void run() {
                // TODO Auto-generated method stub
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                helloVoice.getAudioPlayer().cancel();
                System.out.println("a");

            }

        });
        t.start();

        helloVoice.speak("Thank you for giving me a voice. "
                + "I'm so glad to say hello to this world.");
        helloVoice.speak("Thank you for giving me a voice. "
                + "I'm so glad to say hello to this world.");

        /*
         * Clean up and leave.
         */
        helloVoice.deallocate();
        // System.exit(0);
    }
}
