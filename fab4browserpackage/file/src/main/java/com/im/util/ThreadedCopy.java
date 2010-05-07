package com.im.util;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Copies input from an input stream to an output stream in an independent
 * thread.
 */
public class ThreadedCopy {
    private final Copier copier;
    private final String name;

    class Copier extends Thread {
        private final InputStream in;
        private final OutputStream out;
        private IOException exception;

        Copier(InputStream in, OutputStream out) {
            this.in = in;
            this.out = out;
        }

        @Override
        public void run() {
//            System.out.println(this + ":run");
            try {
                byte[] buf = new byte[8 * 1024];
                int len;

                while ((len = in.read(buf)) > 0) {
//                    System.out.println(this + ":len = " + len);
                    out.write(buf, 0, len);
                }
            } catch (IOException e) {
                exception = e;
            } finally {
//                System.out.println(this + ":done");
                IOUtils.closeQuietly(out);
            }
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * Creates a thread that will copy an input stream to an output stream. This
     * does not start the thread; for that, use {@link #start()}.  This will
     * have the name {@code "<no name>"}.
     *
     * @param in  The input stream.
     * @param out The output stream.
     */
    public ThreadedCopy(InputStream in, OutputStream out) {
        this("<no name>", in, out);
    }

    /**
     * Creates a thread that will copy an input stream to an output stream. This
     * does not start the thread; for that, use {@link #start()}.
     *
     * @param name The name of the copy (useful for debugging).
     * @param in   The input stream.
     * @param out  The output stream.
     */
    public ThreadedCopy(String name, InputStream in, OutputStream out) {
        this.name = name;
        copier = new Copier(in, out);
    }

    /** Start the copy thread going. */
    public void start() {
        copier.start();
    }

    /** Request an interruption of the copy thread. */
    public void interrupt() {
        copier.interrupt();
    }

    /**
     * Join the copy thread.  If the thread has not yet finished this will block
     * until it has.  If it has finished, this will return immediately.
     *
     * @return The exception that interrupted the execution, or {@code null} if
     *         none did.
     *
     * @throws InterruptedException If the {@link Thread#join} call throws an
     *                              {@link InterruptedException}.
     */
    public IOException join() throws InterruptedException {
        copier.join();
        return copier.exception;
    }

    /**
     * Returns the name of this copy operation.
     *
     * @return The name of this copy operation.
     */
    @Override
    public String toString() {
        return name;
    }
}