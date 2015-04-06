package ru.ifmo.ctddev.berdnikov.iterpar;

import java.util.List;
import java.util.function.BiFunction;

/**
 * Auxiliary class for IterativeParallelism.
 * <p>
 * This class represents a monoid with associative binary function
 * and an identity element. It is used by {@link IterativeParallelism}
 * to create threads. It has only one method {@link #run()} which takes
 * an identity element as an initial value of accumulator and successively
 * applies a function to accumulator and list elements. Than the result
 * is added to given list in specified index.
 *
 * @param <E> type of initial list elements
 * @param <R> type of result
 */
public class Monoid<E, R> implements Runnable {
    private final List<? extends E> initList;
    private final List<R> commonList;
    private final BiFunction<R, E, R> function;
    private final R initElement;
    private final int i;


    /**
     * This constructor is used only for set corresponding fields in given values.
     *
     * @param initList initial list
     * @param commonList common list in which the result will be added
     * @param initElement an identity element of monoid
     * @param function an associative function of monoid
     * @param i index of place in the common list where the result will be after the work is done
     */
    public Monoid(List<? extends E> initList, List<R> commonList, R initElement, BiFunction<R, E, R> function, int i) {
        this.initList = initList;
        this.commonList = commonList;
        this.initElement = initElement;
        this.function = function;
        this.i = i;
    }

    /**
     * This method's behaviour is described in class description.
     */
    @Override
    public void run() {
        R result = initElement;
        for (E e : initList) {
            result = function.apply(result, e);
        }
        commonList.set(i, result);
    }
}
