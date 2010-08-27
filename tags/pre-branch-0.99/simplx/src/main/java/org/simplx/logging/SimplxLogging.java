/*
 * Copyright (c) 2009, 2010, Ken Arnold All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * Neither the name of the myself nor the names of its contributors may be used
 * to endorse or promote products derived from this software without specific
 * prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * @SimplxCopyright
 */

package org.simplx.logging;

import java.util.Formatter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class contains methods for creating standard loggers for objects and
 * classes.
 *
 * @author Ken Arnold
 */
public class SimplxLogging {
    /**
     * Not used. This is a utility class. The constructor is left as public so
     * that  you can create subclasses that add other utility methods.
     */
    public SimplxLogging() {
    }

    /**
     * Returns the logger to use for the given object. If the object is not a
     * class object, this is equivalent to <tt>loggerFor(obj.getClass())</tt>.
     * <p/>
     * This method is idempotent: it will always return the same object given
     * the same parameter.  This means it can be used without synchronization in
     * a thread-safe manner, such as in:
     * <pre>
     * protected Logger logger() {
     *     if (logger == null)
     *         logger = SimplxLogging.loggerFor(this);
     *     return logger;
     * }
     * </pre>
     * If this method is called in multiple threads, the worst that will happen
     * is that the same value will get assigned to the {@code logger} field
     * multiple times, which is harmless.  99% of the time, though, this check
     * will avoid the call in the first place because <b>logger</b> will have
     * been set by a previous call, without the overhead of any
     * synchronization.
     *
     * @param obj The object that will have its actions logged.
     *
     * @return The logger for the object.
     *
     * @see #loggerFor(Class)
     */
    public static Logger loggerFor(Object obj) {
        if (obj instanceof Class)
            return loggerFor((Class) obj);
        else
            return loggerFor(obj.getClass());
    }

    /**
     * Returns the logger to use for the given class. The logger will be named
     * using the class's name.
     *
     * @param type The class that will have its actions logged.
     */
    public static Logger loggerFor(Class type) {
        return Logger.getLogger(type.getName());
    }

    /*
     * Note on design choice: I could have chosen to have the loggerFor methods
     * return a new subtype of Logger that added these methods, delegating all
     * other calls to the original logger (the one returned by the underlying
     * getLogger calls).  The problem is that this new logger class would only
     * delegate those calls that existed at the time I wrote this class.  Any
     * methods added in a future version of Java would have their default
     * implementations, and therefore not be delegated.
     *
     * Therefore I have chosen to have external utility methods instead.  These
     * are more awkward to use, but will not react badly with future changes to
     * Logger.
     *
     * This is a classic example of why using interfaces for abstract concepts
     * is often a very good idea.  If Logger was an interface, I could use
     * reflection to fix this.  (Specifically, I could add a new interface with
     * the new format methods, take the original logger and wrap it in a Proxy
     * object that implemented both the Logger interface and the new interface.
     * The proxy would directly implement the format methods, and delegate all
     * other methods to the original logger.  Such code would not know
     * specifically what methods it was delegating, it could just delegate them
     * all. This would make it work properly with added methods.)
     *
     * This is also a good advertisement for the unusual object-oriented feature
     * that Haskel and other languages have of adding types to objects at
     * runtime. In that case, I could simply add the format methods to the log
     * object returned by the getLogger call and return the original object.
     * Much simpler.  (I leave open the question of whether such a feature is a
     * generally good idea; this requires dropping strict type checking form a
     * language, and may be confusing to some folks.  My current guess is that
     * type additions are good, but I have yet to have to live with them.)
     */

    /**
     * Logs a message, using a {@link Formatter} to create the log message.  The
     * message will be the result of calling {@link Formatter#format(String,Object...)}
     * on a formatter with the default locale.  The formatting only happens if
     * the logger says that the level is loggable.
     * <p/>
     * The name "logFormat" was chosen to make your code more readable if you
     * statically import the method.  "SimplxLogging.format" is too long for
     * easy readability.
     *
     * @param logger The logger.
     * @param level  The level passed to {@link Logger#log(Level,String) log}.
     * @param fmt    The format passed to {@link Formatter#format(String,Object...)
     *               format}.
     * @param args   The arguments passed to  {@link Formatter#format(String,Object...)
     *               format}.
     */
    public static void logFormat(Logger logger, Level level, String fmt,
            Object... args) {

        logFormat(logger, level, null, fmt, args);
    }

    /**
     * Logs a message, using a {@link Formatter} to create the log message, with
     * an associated exception.  The message will be the result of calling
     * {@link Formatter#format(String,Object...)} on a formatter with the
     * default locale.  The formatting only happens if the logger says that the
     * level is loggable.
     * <p/>
     * The name "logFormat" was chosen to make your code more readable if you
     * statically import the method.  "SimplxLogging.format" is too long for
     * easy readability.
     *
     * @param logger The logger.
     * @param level  The level passed to {@link Logger#log(Level,String) log}.
     * @param thrown The thrown exception associated with this log message, or
     *               <tt>null</tt> if none.
     * @param fmt    The format passed to {@link Formatter#format(String,Object...)
     *               format}.
     * @param args   The arguments passed to  {@link Formatter#format(String,Object...)
     *               format}.
     */
    public static void logFormat(Logger logger, Level level, Throwable thrown,
            String fmt, Object... args) {

        String msgString = doFormat(logger, level, fmt, args);
        if (msgString != null)
            logger.log(level, msgString, thrown);
    }

    /**
     * Logs a message, using a {@link Formatter} to create the log message,
     * specifying source class and method.  The message will be the result of
     * calling {@link Formatter#format(String,Object...)} on a formatter with
     * the default locale.  The formatting only happens if the logger says that
     * the level is loggable.
     *
     * @param logger       The logger.
     * @param level        The level passed to {@link Logger#logp(Level,String,String,String)
     *                     logp}.
     * @param sourceClass  The sourceClass passed to {@link Logger#logp(Level,String,String,String)
     *                     logp}; can be <tt>null</tt>.
     * @param sourceMethod The sourceMethod passed to {@link Logger#logp(Level,String,String,String)
     *                     logp}; can be <tt>null</tt>.
     * @param fmt          The format passed to {@link Formatter#format(String,Object...)
     *                     format}.
     * @param args         The arguments passed to  {@link Formatter#format(String,Object...)
     *                     format}.
     */
    public static void logFormatp(Logger logger, Level level,
            String sourceClass, String sourceMethod, String fmt,
            Object... args) {
        logFormatp(logger, level, sourceClass, sourceMethod, null, fmt, args);
    }

    /**
     * Logs a message, using a {@link Formatter} to create the log message,
     * specifying source class and method, with an associated exception.  The
     * message will be the result of calling {@link Formatter#format(String,Object...)}
     * on a formatter with the default locale.  The formatting only happens if
     * the logger says that the level is loggable.
     *
     * @param logger       The logger.
     * @param level        The level passed to {@link Logger#logp(Level,String,String,String)
     *                     logp}.
     * @param sourceClass  The sourceClass passed to {@link Logger#logp(Level,String,String,String)
     *                     logp}; can be <tt>null</tt>.
     * @param sourceMethod The sourceMethod passed to {@link Logger#logp(Level,String,String,String)
     *                     logp}; can be <tt>null</tt>.
     * @param thrown       The thrown exception associated with this log
     *                     message, or <tt>null</tt> if none.
     * @param fmt          The format passed to {@link Formatter#format(String,Object...)
     *                     format}.
     * @param args         The arguments passed to  {@link Formatter#format(String,Object...)
     *                     format}.
     */
    public static void logFormatp(Logger logger, Level level,
            String sourceClass, String sourceMethod, Throwable thrown,
            String fmt, Object... args) {
        String msgString = doFormat(logger, level, fmt, args);
        if (msgString != null)
            logger.logp(level, sourceClass, sourceMethod, msgString, thrown);
    }

    /**
     * Logs a message, using a {@link Formatter} to create the log message,
     * specifying source class, method, and resource bundle name.  The message
     * will be the result of calling {@link Formatter#format(String,Object...)}
     * on a formatter with the default locale.  The formatting only happens if
     * the logger says that the level is loggable.
     *
     * @param logger       The logger.
     * @param level        The level passed to {@link Logger#logrb(Level,String,String,String,String)
     *                     logrb}.
     * @param sourceClass  The sourceClass passed to {@link Logger#logrb(Level,String,String,String,String)
     *                     logrb}; can be <tt>null</tt>.
     * @param sourceMethod The sourceMethod passed to {@link Logger#logrb(Level,String,String,String,String)
     *                     logrb}; can be <tt>null</tt>.
     * @param bundleName   The name of resource bundle to localize msg; can be
     *                     <tt>null</tt>.
     * @param fmt          The format passed to {@link Formatter#format(String,Object...)
     *                     format}.
     * @param args         The arguments passed to  {@link Formatter#format(String,Object...)
     *                     format}.
     */
    public static void logFormatrb(Logger logger, Level level,
            String sourceClass, String sourceMethod, String bundleName,
            String fmt, Object... args) {

        logFormatrb(logger, level, sourceClass, sourceMethod, bundleName, null,
                fmt, args);
    }

    /**
     * Logs a message, using a {@link Formatter} to create the log message,
     * specifying source class, method, and resource bundle name, with an
     * associated exception.  The message will be the result of calling {@link
     * Formatter#format(String,Object...)} on a formatter with the default
     * locale.  The formatting only happens if the logger says that the level is
     * loggable.
     *
     * @param logger       The logger.
     * @param level        The level passed to {@link Logger#logrb(Level,String,String,String,String)
     *                     logrb}.
     * @param sourceClass  The sourceClass passed to {@link Logger#logrb(Level,String,String,String,String)
     *                     logrb}; can be <tt>null</tt>.
     * @param sourceMethod The sourceMethod passed to {@link Logger#logrb(Level,String,String,String,String)
     *                     logrb}; can be <tt>null</tt>.
     * @param bundleName   The name of resource bundle to localize msg; can be
     *                     <tt>null</tt>.
     * @param thrown       The thrown exception associated with this log
     *                     message, or <tt>null</tt> if none.
     * @param fmt          The format passed to {@link Formatter#format(String,Object...)
     *                     format}.
     * @param args         The arguments passed to  {@link Formatter#format(String,Object...)
     *                     format}.
     */
    public static void logFormatrb(Logger logger, Level level,
            String sourceClass, String sourceMethod, String bundleName,
            Throwable thrown, String fmt, Object... args) {

        String msgString = doFormat(logger, level, fmt, args);
        if (msgString != null) {
            logger.logrb(level, sourceClass, sourceMethod, bundleName,
                    msgString, thrown);
        }
    }

    private static String doFormat(Logger logger, Level level, String fmt,
            Object... args) {

        if (!logger.isLoggable(level))
            return null;

        Formatter formatter = new Formatter();
        formatter.format(fmt, args);
        return formatter.toString();
    }
}
