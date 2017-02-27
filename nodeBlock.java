/**
 * Created by miteyan on 2/08/2016.
 */

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

public class nodeBlock {

    private RandomAccessFile file;
    private DataInputStream input;
    private int numInts;
    private int numIntsRead = 0;
    private int[] ints;
    private int intsIndex;
    private byte[] bytes ;
    int head;

    //startIndex = bytes index where block starts in the file, numInts is the number of ints the block holds
    public nodeBlock(int startIndex, int numInts, String fileName) throws IOException {
        this.file = new RandomAccessFile(fileName,"r");
        this.input = new DataInputStream(new BufferedInputStream(new FileInputStream(file.getFD())));
        this.numInts = numInts;
        //To get the integers from the file the startIndex in bytes must be multiplied by 4
        //the file is then skipped by that amount to read in the data into the array
        input.skipBytes(startIndex<<2);
        incrementHead();
    }


    private void incrementHead() throws IOException {
        //if all integers read, close the file and streams and signal done by throwing an exception;
        if (numIntsRead>=numInts) {
//            file.close();
//            input.close();
            throw new EOFException();
        }
        // increment pointer to the next smallest value
        //read ints into the bytes array and
        if (ints==null || intsIndex>=ints.length) {
            bytes = new byte[numInts>>2];
            int test = input.read(bytes, 0,numInts>>2);
            if (test==-1)
                throw new EOFException();
            //convert bytes into an int array
            intsIndex = 0;
            ints = ExternalSort.byteArrayToIntArray(bytes);
        }

        //increment and move the head pointer along the array
        head= ints[intsIndex++];
        numIntsRead++;

    }

    public int get() throws LastIntegerInFileException, IOException {
        int value = head;
        if (numIntsRead==numInts){
            throw new LastIntegerInFileException(value);
        }else {
            incrementHead();
            return value;
        }
    }



}
