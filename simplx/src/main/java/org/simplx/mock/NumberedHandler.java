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

package org.simplx.mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * This handler returns specific responses at specifically numbered requests.
 * For example, you can specify that the third request to the server return the
 * value {@code "<error>"}; that the sixth through tenth requests return the
 * values {@code "six"}, {@code "seven"}, {@code "eight"}, {@code "nine"}, and
 * {@code "ten"}; that the tenth through twentieth requests return alternating
 * values of {@code "yes"} and {@code "no"}; etc.
 * <p/>
 * Request numbering can either be by overall request to the server (the
 * default), or by requests made to {@code NumberedHandler} objects by the
 * server. This second mechanism is a way to have some other type of handler
 * first filter out certain kinds of requests, and have the numbered handlers
 * for this server share a single count of other requests that make it to a
 * numbered handler. For example, if a {@link StringHandler} was used to filter
 * out "/ping" URL requests at a higher priority than any {@link
 * NumberedHandler}, the NumberedHandler object could be triggered by counts of
 * the URLs that were not "/ping" requests:
 * <pre>
 * httpd.addHandler(new StringHandler("/ping", "OK\n"));
 * httpd.addHandler(new NumberedHandler(0, "login OK\n"));
 * httpd.addHandler(new NumberedHandler(1, 10, "value store OK\n"));
 * httpd.addHandler(new NumberedHandler(11, "answer: 17.5\n"));
 * </pre>
 * Ping requests might come in every second, and the driving test code could
 * still have request 0 return {@code "login OK"}, requests 1 through 10 return
 * {@code "value store OK"}, and request 11 return {@code "answer: 17.5"}, no
 * matter when the ping requests arrived.
 * <p/>
 * You can customize the use to give responses for odd-numbered requests,
 * prime-numbered requests, and so forth by providing your own {@link Checker}
 * implementation.
 *
 * @author Ken Arnold
 * @param <Q> The type of query comming into the mocker.
 * @param <A> The type of response returned by the mocker.
 */
public class NumberedHandler<Q, A> extends MockHandler<Q, A> {
    private final Checker checker;
    private final boolean overall;
    private final List<A> responses;
    private int respNum;

    private static final Map<Mocker, RequestCounter> counterHolders =
            new WeakHashMap<Mocker, RequestCounter>();

    /**
     * A checker object that discovers if a given request number appiles to a
     * {@link NumberedHandler}.
     */
    public interface Checker {
        /**
         * Returns {@code true} if the request with the given number is in range
         * for a {@link NumberedHandler}.
         *
         * @param requestNum The request number.
         *
         * @return {@code true} if the request with the given number is in range
         *         for a {@link NumberedHandler}.
         */
        boolean applies(int requestNum);
    }

    /**
     * A checker that returns {@code true} if the request number is within a
     * given range.
     */
    public static class RangeChecker implements Checker {
        private final int min;
        private final int max;

        /**
         * Creates a new range checker for the given range. The range is applied
         * in the usual array style of &lt;= min and &lt; max.
         *
         * @param min The minimum value in the range.
         * @param max One larger than the maximum value in the range.
         */
        public RangeChecker(int min, int max) {
            this.min = min;
            this.max = max;
        }

        /**
         * Returns {@code true} if the request number is in the range.
         *
         * @param requestNum The request number.
         *
         * @return {@code true} if the request number is in the range.
         */
        public boolean applies(int requestNum) {
            return requestNum >= min && requestNum < max;
        }
    }

    /**
     * This class is used for keeping track of request counting that is not
     * overall. That is, for counting requests that are examined by at least one
     * {@link NumberedHandler} for the {@link MockHandler}.
     */
    private static class RequestCounter {
        int lastSizeSeen = -1;
        int requestNum = -1;

        synchronized void adjustCount(int size) {
            if (size != lastSizeSeen) {
                lastSizeSeen = size;
                requestNum++;
            }
        }
    }

    /**
     * Creates a {@link NumberedHandler} that applies to a specific request
     * number. This will use overall numbering.
     *
     * @param requestNum The request number.
     * @param response   The response for that request.
     */
    public NumberedHandler(int requestNum, A response) {
        this(requestNum, requestNum + 1, response);
    }

    /**
     * Creates a {@link NumberedHandler} that applies to a specific request
     * number.
     *
     * @param requestNum The request number.
     * @param overall    Whether to use overall numbering.
     * @param response   The response for that request.
     */
    public NumberedHandler(int requestNum, boolean overall, A response) {
        this(requestNum, requestNum + 1, overall, response);
    }

    /**
     * Creates a {@link NumberedHandler} that applies to a range of request
     * numbers. This will use overall numbering. Responses will loop through the
     * list of responses provided. For example, you can get a repeat set of
     * {@code "yes"}, {@code "no"}, and {@code "maybe"} responses can be
     * provided over the range of request number 1 through 10 with the following
     * code:
     * <pre>
     * new NumberedHandler(1, 11, "yes", "no", "maybe");
     * </pre>
     *
     * @param min      The first number the handler applies to.
     * @param max      One past the last number the handler applies to.
     * @param response The response for that request.
     * @param others   Any other responses to return.
     */
    public NumberedHandler(int min, int max, A response, A... others) {

        this(min, max, true, response, others);
    }

    /**
     * Creates a {@link NumberedHandler} that applies to a range of request
     * numbers. Responses will loop through the list of responses provided. For
     * example, you can get a repeat set of {@code "yes"}, {@code "no"}, and
     * {@code "maybe"} responses can be provided over the range of request
     * number 1 through 10, using non-overall numbering, with the following
     * code:
     * <pre>
     * new NumberedHandler(1, 11, false, "yes", "no", "maybe");
     * </pre>
     *
     * @param min      The first number the handler applies to.
     * @param max      One past the last number the handler applies to.
     * @param overall  Whether to use overall numbering.
     * @param response The response for that request.
     * @param others   Any other responses to return.
     */
    public NumberedHandler(int min, int max, boolean overall, A response,
            A... others) {

        this(new RangeChecker(min, max), overall, response, others);
    }

    /**
     * Creates a {@link NumberedHandler} that applies to a {@link Checker
     * Checker}-specified set of numbers. Responses will loop through the list
     * of responses provided. For example, you can get a repeat set of {@code
     * "yes"}, {@code "no"}, and {@code "maybe"} responses can be provided for
     * all odd numbers, using non-overall numbering, with the following code:
     * <pre>
     * new NumberedHandler(new Checker() {
     *     public boolean applies(int requestNum) {
     *         return (requestNum % 2) == 1;
     *     }
     * }, false, "yes", "no", "maybe");
     * </pre>
     *
     * @param checker  The checker to use to discover if a number applies to the
     *                 new handler.
     * @param overall  Whether to use overall numbering.
     * @param response The response for that request.
     * @param others   Any other responses to return.
     */
    public NumberedHandler(Checker checker, boolean overall, A response,
            A... others) {

        this.checker = checker;
        this.overall = overall;

        responses = new ArrayList<A>(others.length + 1);
        responses.add(response);
        responses.addAll(Arrays.asList(others));
    }

    /**
     * If the request number applies to this handler, return the response
     * specified for this request.  The request object itself is not examined.
     */
    @Override
    public A response(Mocker<Q, A> mocker, Q request) {
        int num;
        if (overall) {
            num = mocker.getHistory().size() - 1;
        } else {
            RequestCounter counter;
            synchronized (counterHolders) {
                counter = counterHolders.get(mocker);
                if (counter == null) {
                    counter = new RequestCounter();
                    counterHolders.put(mocker, counter);
                }
            }
            counter.adjustCount(mocker.getHistory().size());
            num = counter.requestNum;
        }

        if (!checker.applies(num))
            return null;

        while (respNum >= responses.size())
            respNum -= responses.size();

        return responses.get(respNum++);
    }
}
