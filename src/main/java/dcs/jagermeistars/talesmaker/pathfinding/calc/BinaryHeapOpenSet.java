package dcs.jagermeistars.talesmaker.pathfinding.calc;

/**
 * Binary heap implementation for efficient open set management in A*.
 * Provides O(log n) insert, poll, and update operations.
 */
public class BinaryHeapOpenSet {
    private PathNode[] heap;
    private int size;

    private static final int DEFAULT_CAPACITY = 1024;

    public BinaryHeapOpenSet() {
        this(DEFAULT_CAPACITY);
    }

    public BinaryHeapOpenSet(int initialCapacity) {
        this.heap = new PathNode[initialCapacity];
        this.size = 0;
    }

    /**
     * Insert a node into the heap.
     * O(log n)
     */
    public void insert(PathNode node) {
        if (size >= heap.length) {
            grow();
        }
        heap[size] = node;
        node.setHeapIndex(size);
        size++;
        siftUp(size - 1);
    }

    /**
     * Remove and return the node with lowest fCost.
     * O(log n)
     */
    public PathNode poll() {
        if (size == 0) {
            return null;
        }
        PathNode result = heap[0];
        result.setHeapIndex(-1);

        size--;
        if (size > 0) {
            heap[0] = heap[size];
            heap[0].setHeapIndex(0);
            heap[size] = null;
            siftDown(0);
        } else {
            heap[0] = null;
        }

        return result;
    }

    /**
     * Peek at the node with lowest fCost without removing.
     * O(1)
     */
    public PathNode peek() {
        return size > 0 ? heap[0] : null;
    }

    /**
     * Update a node's position after its cost changed.
     * O(log n)
     */
    public void update(PathNode node) {
        int index = node.getHeapIndex();
        if (index < 0 || index >= size) {
            return;
        }
        // Try both directions since cost could increase or decrease
        siftUp(index);
        siftDown(node.getHeapIndex());
    }

    /**
     * Check if heap contains the node.
     * O(1) using heapIndex
     */
    public boolean contains(PathNode node) {
        int index = node.getHeapIndex();
        return index >= 0 && index < size && heap[index] == node;
    }

    /**
     * Check if heap is empty.
     */
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Get current size.
     */
    public int size() {
        return size;
    }

    /**
     * Clear the heap.
     */
    public void clear() {
        for (int i = 0; i < size; i++) {
            if (heap[i] != null) {
                heap[i].setHeapIndex(-1);
                heap[i] = null;
            }
        }
        size = 0;
    }

    /**
     * Move node up to maintain heap property.
     */
    private void siftUp(int index) {
        PathNode node = heap[index];
        while (index > 0) {
            int parentIndex = (index - 1) / 2;
            PathNode parent = heap[parentIndex];
            if (node.compareTo(parent) >= 0) {
                break;
            }
            // Swap with parent
            heap[index] = parent;
            parent.setHeapIndex(index);
            index = parentIndex;
        }
        heap[index] = node;
        node.setHeapIndex(index);
    }

    /**
     * Move node down to maintain heap property.
     */
    private void siftDown(int index) {
        PathNode node = heap[index];
        int half = size / 2;

        while (index < half) {
            int leftChild = 2 * index + 1;
            int rightChild = leftChild + 1;
            int smallestChild = leftChild;

            // Find smaller child
            if (rightChild < size && heap[rightChild].compareTo(heap[leftChild]) < 0) {
                smallestChild = rightChild;
            }

            if (node.compareTo(heap[smallestChild]) <= 0) {
                break;
            }

            // Swap with smaller child
            heap[index] = heap[smallestChild];
            heap[smallestChild].setHeapIndex(index);
            index = smallestChild;
        }
        heap[index] = node;
        node.setHeapIndex(index);
    }

    /**
     * Double the heap capacity.
     */
    private void grow() {
        PathNode[] newHeap = new PathNode[heap.length * 2];
        System.arraycopy(heap, 0, newHeap, 0, heap.length);
        heap = newHeap;
    }
}