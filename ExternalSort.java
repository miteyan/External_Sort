/**
 * Created by miteyan on 2/08/2016.
 */

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class ExternalSort {
//    private static final String[] checksums = new String[]{"d41d8cd98f0b24e980998ecf8427e", "a54f041a9e15b5f25c463f1db7449","c2cb56f4c5bf656faca0986e7eba38","c1fa1f22fa36d331be4027e683baad6","8d79cbc9a4ecdde112fc91ba625b13c2","1e52ef3b2acef1f831f728dc2d16174d","6b15b255d36ae9c85ccd3475ec11c3","1484c15a27e48931297fb6682ff625","ad4f60f065174cf4f8b15cbb1b17a1bd","32446e5dd58ed5a5d7df2522f0240","435fe88036417d686ad8772c86622ab","c4dacdbc3c2e8ddbb94aac3115e25aa2","3d5293e89244d513abdf94be643c630","468c1c2b4c1b74ddd44ce2ce775fb35c","79d830e4c0efa93801b5d89437f9f3e","c7477d400c36fca5414e0674863ba91","cc80f01b7d2d26042f3286bdeff0d9"};

    //number of bytes to deem a file to be too small to apply merge algorithm
    private static final int SMALL_FILE =8000000;

    public static void sort(String fileA, String fileB) throws IOException {
        RandomAccessFile rafA = new RandomAccessFile(fileA, "r");
        int fileLength = (int) rafA.length();

        if (fileLength < 5) {
            return;
        }
        if (fileLength <= SMALL_FILE){
            sortSmall(fileA);
        } else {
            //initially sort blocks of size SMALL_SIZE/K then write into file B,
            int blockSizeSorted = sortBig(fileA, fileB, fileLength);
            //then merge the files back into the original reducing disk accesses
            RandomAccessFile rafInput = new RandomAccessFile(fileB, "r");
            int totalIntsInFile = (int) rafInput.length()>>2;//byte length/4 is the integer length
            int numberOfIntsInBlock = blockSizeSorted >>2;//value that works
            int numberOfBlocks = (totalIntsInFile/numberOfIntsInBlock);
            //create minHeap of the sorted blocks in file B
            BlockMinHeap minHeap = initialiseHeap(numberOfBlocks,numberOfIntsInBlock,fileB);
            //merge the sorted blocks back into file A
            mergeHeap(totalIntsInFile,minHeap,fileA);
        }
    }

    private static void sortSmall(String file1) throws IOException {
        //If file small = just sort it in place into the same file A
        RandomAccessFile raf = new RandomAccessFile(file1,"rw");
        DataOutputStream outputStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(raf.getFD())));
        //Get file A into an int array
        int[] array = fileToIntArray(raf, 0, (int) raf.length() >>2);
        //Sort file array
        radixSort(array);
        //Convert to bytes
        byte[] bytes = intArrayToByteArray(array);
        //Write bytes back into file A
        raf.write(bytes, 0, bytes.length);
        outputStream.flush();
//        raf.close();
    }

    private static int sortBig(String fileA, String fileB, int length) throws IOException {
        //Semi sort the file: Sort blocks of the file and write sorted blocks into file B
        //Afterwards merge these sorted blocks back into file A to give a complete sorted file
        //Using merge method
        int blockSizeSorted;
        RandomAccessFile rafA = new RandomAccessFile(fileA,"r");
        RandomAccessFile rafB = new RandomAccessFile(fileB, "rw");
        DataOutputStream dosB = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(rafB.getFD())));
        int intNum;
        int[] ints;
        intNum = SMALL_FILE>>2;

        for (int startIndex = 0; startIndex <= length>>2; startIndex += intNum){
            //Get blocks from file A as an int array
            ints = fileToIntArray(rafA, startIndex, intNum);
            //Sort block from file A
            radixSort(ints);
            //Write sorted block into file B
            dosB.write(intArrayToByteArray(ints));
        }
        dosB.flush();
        blockSizeSorted = intNum;
        return blockSizeSorted;
    }

    private static BlockMinHeap initialiseHeap(int numberOfBlocks, int numberOfIntsInBlock, String inputFileName){
        //Initialise minHeap:
        BlockMinHeap minHeap = new BlockMinHeap(numberOfBlocks+1);
        for (int blockIndex = 0; blockIndex <= numberOfBlocks; blockIndex++){
            try {
                //Initialise node blocks
                int startIndex = blockIndex*numberOfIntsInBlock;
                nodeBlock newnodeBlock = new nodeBlock( startIndex, numberOfIntsInBlock, inputFileName);
                //Insert blocks in order in file
                //Let insert method take care of order in the heap by doing the relevant bubbling up/down of nodeBlocks
                minHeap.insert(newnodeBlock);
            } catch (IOException e){
                //      e.printStackTrace();
            }
        }
        return minHeap;
    }

    private static void mergeHeap(int totalIntsInFile, BlockMinHeap minHeap, String outputFileName) throws IOException {
        DataOutputStream dataOutputStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(new RandomAccessFile(outputFileName,"rw").getFD())));
        //Smallest node block placement
        nodeBlock smallestBlock = null;
        int smallestHead = 0;
        //Buffers/tmp storage variables
        int bufferSize = SMALL_FILE/10;
        byte[] byteBuffer = new byte[bufferSize];

        int bytePosition = 0;
        for (int i = 0; i < totalIntsInFile; i++){
            try{
                smallestBlock = minHeap.extractMin();
                smallestHead = smallestBlock.get();
                minHeap.insert(smallestBlock);
            } catch (EOFException e) {
                // smaller than blockSize
                smallestHead = smallestBlock.head;
            } catch (LastIntegerInFileException e) {
                smallestHead = e.lastIntegerInFile;
            } finally {//once exception caught write to buffer
                //bufferSize/4 is equal to number of ints in buffer
                if (i % (bufferSize >>2) == 0 && i != 0){
                    //for non-first initialise new buffer.
                    dataOutputStream.write(byteBuffer);
                    byteBuffer = new byte[bufferSize];
                    bytePosition = 0;
                }
                //convert smallest head variable into a byte array then put into byteBuffer
                //byte array of length 4 = 1 integer
                int byteIndex = bytePosition<<2;

                writeBuffer(smallestHead,byteBuffer,byteIndex);
                bytePosition++;
            }
        }
        //write bytebuffer into the file
        dataOutputStream.write(byteBuffer);
        dataOutputStream.flush();
    }

    public static void writeBuffer(int smallestVal, byte[] buffer,int byteIndex) {
        System.arraycopy(intToByteArray(smallestVal), 0, buffer,  byteIndex,4);
    }

    public static void radixSort(int[] values) {
        int[] tmp = new int[values.length];

        final int byteRange = 256;
        final int halfByteRange = 128;
        int value;
        // each bytes is between 0 and 255 => R=256
        // mask = 0xFF leaving the smallest 8 bits and keeps the sign bit

        //counting stage- computing the frequencies of each byte for each digit
        for (int digit = 0 ; digit <4; digit++) {
            int[] counts = new int[byteRange + 1];
            // compute frequency counts -- bytes set has 256 elements
            // Bitmask (number >> 8*digit) & 0xFF
            // number is an integer composed of 4 bytes of 8 bit length.
            //We pick out each byte and preserve its sign from least to most significant by shifting appropriately anding it with 11111111
            //to get 8 bits only
            for (int i = 0; i < values.length; i++) {
                value = (values[i] >> (8 * digit)) & 0xFF;
                counts[value + 1]++;
            }
            //compute cumulative frequency of counts array
            for (int j = 0; j < byteRange; j++) {
                counts[j + 1] += counts[j];
            }

            if (digit == 3) {
                // for MSB(digit ==3) : shifting to preserve ordering
                int shift1 = counts[byteRange] - counts[byteRange / 2];
                int shift2 = counts[halfByteRange];
                for (int b = 0; b < halfByteRange; b++) {
                    counts[b] += shift1;
                }
                for (int b = byteRange / 2; b < byteRange; b++) {
                    counts[b] -= shift2;
                }
            }
            //copy the values in sorted order into the tmp array
            for (int i = 0; i < values.length; i++) {
                value = (values[i]>>8*digit) & 0xFF;
                tmp[counts[value] ++] = values[i];
            }
            //copy from tmp array to original
            for (int i=0 ; i<values.length; i++) {
                values[i] = tmp[i];
            }
        }
    }

    private static String byteToHex(byte b) {
        String r = Integer.toHexString(b);
        if (r.length() == 8) {
            return r.substring(6);
        }
        return r;
    }
    @SuppressWarnings("resource")

    public static String checkSum(String f) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            DigestInputStream ds = new DigestInputStream(
                    new FileInputStream(f), md);
            byte[] b = new byte[512];
            while (ds.read(b) != -1)
                ;

            String computed = "";
            for(byte v : md.digest())
                computed += byteToHex(v);

            return computed;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "<error computing checksum>";
    }

    private static byte[] intToByteArray(int a) {
        return ByteBuffer.allocate(4).putInt(a).array();
    }

    private static byte[] intArrayToByteArray(int[] intArray) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(intArray.length * 4);
        IntBuffer intBuffer = byteBuffer.asIntBuffer();
        intBuffer.put(intArray);
        return byteBuffer.array();
    }

    public static int[] byteArrayToIntArray(byte[] bytes) {
        IntBuffer intBuf = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).asIntBuffer();
        int[] array = new int[intBuf.remaining()];
        intBuf.get(array);
        return array;
    }

    private static int[] fileToIntArray(RandomAccessFile input, int startIntIndex, int lengthInInts) throws IOException {
        DataInputStream inputStream = new DataInputStream(new BufferedInputStream(new FileInputStream(input.getFD())));
        byte[] bytesArray = new byte[lengthInInts*4];
        int bytesRead = 0;
        // Skip the input stream to the correct point
        inputStream.skipBytes(4*startIntIndex);
        bytesRead = inputStream.read(bytesArray, 0, lengthInInts*4);
        if (bytesRead==-1) {
            return new int[0];
        }else {
            // Reset the file pointer
            input.seek(0);
            return byteArrayToIntArray(bytesArray);
        }
    }
//
//    public static void copyDirectory(File sourceLocation , File targetLocation) throws IOException {
//        if (sourceLocation.isDirectory()) {
//            if (!targetLocation.exists()) {
//                targetLocation.mkdir();
//            }
//            String[] children = sourceLocation.list();
//            for (int i = 0; i < children.length; i++) {
//                copyDirectory(new File(sourceLocation, children[i]),
//                        new File(targetLocation, children[i]));
//            }
//        } else {
//            InputStream in = new FileInputStream(sourceLocation);
//            OutputStream out = new FileOutputStream(targetLocation);
//            // Copy the bits from instream to outstream
//            byte[] buf = new byte[1024];
//            int len;
//            while ((len = in.read(buf)) > 0) {
//                out.write(buf, 0, len);
//            }
//            in.close();
//            out.close();
//        }
//    }
//
//    public static void sortx() throws IOException {
//        long startTime = System.nanoTime();
//        for (int i =1 ; i<18;i++) {
//            String f1 = "./test-suite/test"+Integer.toString(i)+"a.dat";
//            String f2 = "./test-suite/test"+Integer.toString(i)+"b.dat";
//            RandomAccessFile raf1 = new RandomAccessFile(f1,"r");
//            sort(f1, f2);
//            String checksum = checkSum(f1);
//            System.out.println(i + " " + checksum.equals(checksums[i - 1]) + " " + raf1.length() + " " + checksum);
//        }
//        long endTime = System.nanoTime();
//
//        long duration = (endTime - startTime);
//        System.out.println("Time: " +duration/1000);
//        copyAll();
//
//    }
//    public static void copyAll() throws IOException {
//        for (int num = 1; num<18; num++) {
//            String source = "./test-suite 2/test" + num + "a.dat";
//            String dest = "./test-suite/test" + num + "a.dat";
//            copyDirectory(new File(source), new File(dest));
//        }
//    }
//    public static void main(String[] args) throws Exception {
//        sortx();
//    }

}

// byte array to int array

//    public static int[] byteArrayToIntArray(byte[] bytes) {
//        int offset = 0;
//        int[] intArray = new int[bytes.length/4];
//
//        for (int i =0 ; i<intArray.length; i++) {
//            intArray[i] = (bytes[3 + offset] & 0xFF) |
//                    ((bytes[2 + offset] & 0xFF) << 8) |
//                    ((bytes[1 + offset] & 0xFF) << 16) |
//                    ((bytes[0 + offset] & 0xFF) << 24);
//            offset+=4;
//        }
//        return intArray;
//    }

//int array to byte array

//    private static byte[] intArrayToByteArray(int[] intArray) {
//        byte[] byteArray = new byte[intArray.length * 4];
//
//        for (int i = 0 ; i<intArray.length; i++) {
//            byteArray[4*i] = intToByteArray(intArray[i])[0];
//            byteArray[4*i+1] = intToByteArray(intArray[i])[1];
//            byteArray[4*i+2] = intToByteArray(intArray[i])[2];
//            byteArray[4*i+3] = intToByteArray(intArray[i])[3];
//        }
//        return byteArray;
//    }

//int to byte array -- using java byte buffer, could use shifting by 8i bits with anding with 0xFF
//    private static byte[] intToByteArray(int a) {
//        return new byte[]{
//                (byte) ((a >> 24) & 0xFF),
//                (byte) ((a >> 16) & 0xFF),
//                (byte) ((a >> 8) & 0xFF),
//                (byte) (a & 0xFF)
//        };
//    }