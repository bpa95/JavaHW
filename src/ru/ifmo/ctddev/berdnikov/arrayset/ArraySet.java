package ru.ifmo.ctddev.berdnikov.arrayset;

import java.util.*;

public class ArraySet<E> extends AbstractSet<E> implements NavigableSet<E> {
    private List<E> array;
    private Comparator<? super E> comparator;

    public ArraySet() {
        this(Collections.emptyList(), null);
    }

    public ArraySet(Comparator<? super E> comparator) {
        this(Collections.emptyList(), comparator);
    }

    public ArraySet(Collection<? extends E> c) {
        this(c, null);
    }

    public ArraySet(Collection<? extends E> c, Comparator<? super E> comparator) {
        Set<E> set = new TreeSet<>(comparator);
        set.addAll(c);
        array = new ArrayList<>(set);
        this.comparator = comparator;
    }

    private ArraySet(List<E> list, Comparator<? super E> comparator) {
        array = list;
        this.comparator = comparator;
    }

    private int search(E e) {
        return Collections.binarySearch(array, e, comparator);
    }

    private E elemOrNull(int i) {
        if (0 <= i && i < array.size()) {
            return array.get(i);
        } else {
            return null;
        }
    }

    @Override
    public E lower(E e) {
        int i = search(e);
        if (i < 0) {
            i = -(i + 1);
        }
        return elemOrNull(i - 1);
    }

    @Override
    public E floor(E e) {
        int i = search(e);
        if (i < 0) {
            i = -(i + 1) - 1;
        }
        return elemOrNull(i);
    }

    @Override
    public E ceiling(E e) {
        int i = search(e);
        if (i < 0) {
            i = -(i + 1);
        }
        return elemOrNull(i);
    }

    @Override
    public E higher(E e) {
        int i = search(e);
        if (i < 0) {
            i = -(i + 1) - 1;
        }
        return elemOrNull(i + 1);
    }

    @Override
    public E pollFirst() {
        throw new UnsupportedOperationException();
    }

    @Override
    public E pollLast() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int size() {
        return array.size();
    }

    @Override
    public boolean contains(Object o) {
        return 0 <= search((E) o);
    }

    private class ImIterator<T> implements Iterator<T> {
        Iterator<T> iterator;

        ImIterator(Iterator<T> iterator) {
            this.iterator = iterator;
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public T next() {
            return iterator.next();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public Iterator<E> iterator() {
        return new ImIterator<>(array.iterator());
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    private Comparator<? super E> comparatorOrNatural() {
        if (comparator == null) {
            return (Comparator<? super E>) Comparator.naturalOrder();
        } else {
            return comparator;
        }
    }

    @Override
    public NavigableSet<E> descendingSet() {
        return new ArraySet<>((Collection<E>) array, comparatorOrNatural().reversed());
    }

    @Override
    public Iterator<E> descendingIterator() {
        return new ImIterator<>(new ArraySet<>((Collection<E>) array, comparatorOrNatural().reversed()).iterator());
    }

    @Override
    public NavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
        return tailSet(fromElement, fromInclusive).headSet(toElement, toInclusive);
    }

    public NavigableSet<E> headSet(E toElement, boolean inclusive) {
        int i = search(toElement);
        if (i >= 0 && inclusive) {
            i++;
        } else if (i < 0) {
            i = -(i + 1);
        }
        return new ArraySet<>(array.subList(0, i), comparator);
    }

    @Override
    public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
        int i = search(fromElement);
        if (i >= 0 && !inclusive) {
            i++;
        } else if (i < 0) {
            i = -(i + 1);
        }
        return new ArraySet<>(array.subList(i, array.size()), comparator);
    }

    @Override
    public Comparator<? super E> comparator() {

        return comparator;
    }

    @Override
    public SortedSet<E> subSet(E fromElement, E toElement) {
        return subSet(fromElement, true, toElement, false);
    }

    @Override
    public SortedSet<E> headSet(E toElement) {
        return headSet(toElement, false);
    }

    @Override
    public SortedSet<E> tailSet(E fromElement) {
        return tailSet(fromElement, true);
    }

    @Override
    public E first() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        return array.get(0);
    }

    @Override
    public E last() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        return array.get(array.size() - 1);
    }
}
