/**
 * Created by miteyan on 2/08/2016.
 */

public class BlockMinHeap {

    public BlockMinHeap(int heapSize) {
        blocks = new nodeBlock[heapSize];
    }
    //initially 0
    private int blockCount = 0;
    private nodeBlock[] blocks;

    private void bubbleUp(int childIndex) {
        if (childIndex>0) {
            int parentIndex = (childIndex-1)>>1;
            //if parent smaller than child move parent down the heap recursively
            if (blocks[parentIndex].head>blocks[childIndex].head) {
                //swap
                nodeBlock tmp = blocks[childIndex];
                blocks[childIndex] = blocks[parentIndex];
                blocks[parentIndex] = tmp;

                bubbleUp(parentIndex);
            }
        }
    }

    public int insert(nodeBlock nb) {
        if (blockCount == blocks.length) {
            return -1;
        }else{
            blocks[blockCount] = nb;
            bubbleUp(blockCount++);
            return 1;
        }
    }

    private void bubbleDown(int index) {
        int lChild = index*2+1;
        int rChild = index*2+2;
        if (lChild >=blockCount && rChild>=blockCount) {
            return;
        }
        int smallestChildIndex;

        if (blocks[lChild].head<=blocks[rChild].head) {
            smallestChildIndex = lChild;
        }else{
            smallestChildIndex = rChild;
        }

        if (blocks[index].head>blocks[smallestChildIndex].head) {

            nodeBlock tmp = blocks[index];
            blocks[index] = blocks[smallestChildIndex];
            blocks[smallestChildIndex] = tmp;

            index = smallestChildIndex; //must swap elements since swapped in heap
            bubbleDown(index);
        }
    }

    public nodeBlock extractMin(){
        if (blockCount==0) {//heap empty
            return null;
        }else {
            nodeBlock min = blocks[0];
            //replace first element with last and reduce block count to overwrite it to delete an element
            //then bubble it down
            blocks[0] = blocks[blockCount - 1];
            if (--blockCount > 0) {
                bubbleDown(0);
            }
            return min;
        }
    }


}
