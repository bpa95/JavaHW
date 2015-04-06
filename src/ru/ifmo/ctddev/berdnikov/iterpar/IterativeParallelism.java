package ru.ifmo.ctddev.berdnikov.iterpar;

import info.kgeorgiy.java.advanced.concurrent.ListIP;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * This class provides methods for operating lists using threads
 * for better efficiency.
 * You can: <p>
 * - concatenate all string representations of elements by {@link #concat(int, List)} <p>
 * - filter list by predicate by {@link #filter(int, List, Predicate)} <p>
 * - map function through list by {@link #map(int, List, Function)} <p>
 * - get maximum or minimum of list by {@link #maximum(int, List, Comparator)} and {@link #minimum(int, List, Comparator)} <p>
 * - get known if all or any element satisfy predicate by {@link #all(int, List, Predicate)} and {@link #any(int, List, Predicate)}
 */
public class IterativeParallelism implements ListIP {

    private <E, R> List<R> parallel(int threads, List<? extends E> list, BiFunction<R, E, R> fun, R initEl) throws InterruptedException {
        if (threads > list.size()) {
            threads = list.size();
        } else if (threads < 1) {
            threads = 1;
        }
        int chunkSize = list.size() / threads;
        int chunkRest = list.size() % threads;
        int lower = 0;
        int upper = chunkSize + chunkRest;
        List<Thread> thrs = new ArrayList<>(threads);
        List<R> results = Collections.synchronizedList(new ArrayList<>(threads));
        for (int i = 0; i < threads; i++) {
            results.add(null);
        }
        for (int i = 0; i < threads; i++) {
            Monoid<E, R> m = new Monoid<>(list.subList(lower, upper), results, initEl, fun, i);
            Thread t = new Thread(m);
            t.start();
            thrs.add(t);
            lower = upper;
            upper = lower + chunkSize;
        }
        for (Thread t : thrs) {
            t.join();
        }
        return results;
    }

    /**
     * Concatenates all string representations of elements of given list
     * and returns resultant string. It uses <tt>threads</tt> number of threads to
     * obtain better efficiency.
     * @param threads number of threads
     * @param list initial list
     * @return <tt>String</tt> which contains result
     * @throws InterruptedException when some of threads are interrupted
     */
    @Override
    public String concat(int threads, List<?> list) throws InterruptedException {
        BiFunction<String, Object, String> fun = (s, o) -> s + o.toString();
        List<String> results = parallel(threads, list, fun, "");
        String result = "";
        for (String s : results) {
            result += s;
        }
        return result;
    }

    /**
     * Filters given list by predicate. It uses <tt>threads</tt> number of threads to
     * obtain better efficiency.
     *
     * @param threads number of threads
     * @param list initial list
     * @param predicate predicate which returns true if element must be in the resultant list
     * @param <T> type of elements of given list
     * @return list of elements which passed given predicate
     * @throws InterruptedException when some of threads are interrupted
     */
    @Override
    public <T> List<T> filter(int threads, List<? extends T> list, Predicate<? super T> predicate) throws InterruptedException {
        BiFunction<List<T>, T, List<T>> fun = (ts, t) -> {
            if (ts == null) {
                ts = new ArrayList<>();
            }
            if (predicate.test(t)) {
                ts.add(t);
            }
            return ts;
        };
        List<List<T>> results = parallel(threads, list, fun, null);
        List<T> result = new ArrayList<>();
        for (List<T> r : results) {
            result.addAll(r);
        }
        return result;
    }

    /**
     * Applies given function to all elements. It uses <tt>threads</tt> number of threads to
     * obtain better efficiency.
     *
     * @param threads number of threads
     * @param list initial list
     * @param function given function
     * @param <T> type of elements of initial list
     * @param <U> type of elements of the resultant list
     * @return list of elements with applied function
     * @throws InterruptedException when some of threads are interrupted
     */
    @Override
    public <T, U> List<U> map(int threads, List<? extends T> list, Function<? super T, ? extends U> function) throws InterruptedException {
        BiFunction<List<U>, T, List<U>> fun = (ts, t) -> {
            if (ts == null) {
                ts = new ArrayList<>();
            }
            ts.add(function.apply(t));
            return ts;
        };
        List<List<U>> results = parallel(threads, list, fun, null);
        List<U> result = new ArrayList<>();
        for (List<U> r : results) {
            result.addAll(r);
        }
        return result;
    }

    /**
     * Counts the maximum of given list by given comparator. It uses <tt>threads</tt> number of threads to
     * obtain better efficiency.
     *
     * @param threads number of threads
     * @param list initial list
     * @param comparator given comparator
     * @param <T> type of elements of initial list
     * @return maximum of given list
     * @throws InterruptedException when some of threads are interrupted
     */
    @Override
    public <T> T maximum(int threads, List<? extends T> list, Comparator<? super T> comparator) throws InterruptedException {
        BiFunction<T, T, T> fun = (t1, t2) -> comparator.compare(t1, t2) >= 0 ? t1 : t2;
        List<T> maxs = parallel(threads, list, fun, list.get(0));
        return Collections.max(maxs, comparator);
    }

    /**
     * Counts the minimum of given list by given comparator. It uses <tt>threads</tt> number of threads to
     * obtain better efficiency.
     *
     * @param threads number of threads
     * @param list initial list
     * @param comparator given comparator
     * @param <T> type of elements of initial list
     * @return minimum of given list
     * @throws InterruptedException when some of threads are interrupted
     */
    @Override
    public <T> T minimum(int threads, List<? extends T> list, Comparator<? super T> comparator) throws InterruptedException {
        BiFunction<T, T, T> fun = (t1, t2) -> comparator.compare(t1, t2) > 0 ? t2 : t1;
        List<T> mins = parallel(threads, list, fun, list.get(0));
        return Collections.min(mins, comparator);
    }

    /**
     * Returns <tt>true</tt> if all elements satisfy given predicate,
     * <tt>false</tt> otherwise. It uses <tt>threads</tt> number of threads to
     * obtain better efficiency.
     *
     * @param threads number of threads
     * @param list initial list
     * @param predicate given predicate
     * @param <T> type of elements of initial list
     * @return <tt>true</tt> if all elements satisfy given predicate
     * @throws InterruptedException when some of threads are interrupted
     */
    @Override
    public <T> boolean all(int threads, List<? extends T> list, Predicate<? super T> predicate) throws InterruptedException {
        BiFunction<Boolean, T, Boolean> fun = (f, t) -> f & predicate.test(t);
        List<Boolean> results = parallel(threads, list, fun, true);
        boolean f = true;
        for (boolean e : results) {
            f &= e;
        }
        return f;
    }

    /**
     * Returns <tt>true</tt> if any of elements satisfy given predicate,
     * <tt>false</tt> otherwise. It uses <tt>threads</tt> number of threads to
     * obtain better efficiency.
     *
     * @param threads number of threads
     * @param list initial list
     * @param predicate given predicate
     * @param <T> type of elements of initial list
     * @return <tt>true</tt> if any of elements satisfy given predicate
     * @throws InterruptedException when some of threads are interrupted
     */
    @Override
    public <T> boolean any(int threads, List<? extends T> list, Predicate<? super T> predicate) throws InterruptedException {
        BiFunction<Boolean, T, Boolean> fun = (f, t) -> f | predicate.test(t);
        List<Boolean> results = parallel(threads, list, fun, false);
        boolean f = false;
        for (boolean e : results) {
            f |= e;
        }
        return f;
    }
}
