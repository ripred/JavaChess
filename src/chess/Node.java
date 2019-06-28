package Chess;

import java.util.Objects;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.NoSuchElementException;

public class Node implements Iterable<Byte> {
    private byte[] state;

    Node(byte[] t) {
        state = t;
    }

    Node(final Node node) {
        state = node.state;
    }

    // Return an iterator over the elements in the array. This is generally not
    // called directly, but is called by Java when used in a "simple" for loop.

    @Override
    public Iterator<Byte> iterator() {
        return new NodeIterator();
    }

    @Override
    public void forEach(Consumer<? super Byte> action) {
        Objects.requireNonNull(action);
        for (Byte t : this.state) {
            action.accept(t);
        }
    }

    // This is a private class that implements iteration over the elements
    // of the list. It is not accessed directly by the user, but is used in
    // the iterator() method of the Array class. It implements the hasNext()
    // and next() methods.
    class NodeIterator implements Iterator<Byte> {
        int current = 0;  // the current element we are looking at

        // return whether or not there are more elements in the array that
        // have not been iterated over.
        public boolean hasNext() {
            return current < Node.this.state.length;
        }

        // return the next element of the iteration and move the current
        // index to the element after that.
        public Byte next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            return state[current++];
        }
    }

    // Return the value at a given index
    public Byte get(int index) {
        return state[index];
    }

    // Set the value at a given index
    public void set(int index, Byte value) {
        state[index] = value;
    }

    // Return the length of the array
    public int length() {
        return state.length;
    }
}



