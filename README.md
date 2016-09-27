# External_Sort

The External Sort algorithm is used to sort large files of numbers efficiently, where a normal sort algorithm cannot be applied due to a lack of memory, which is less than the size of the file itself. 

The recommended merge sort modification algorithm like in the image would be very slow since the disk reads utilised would be very numerous and so I had to create a custom algorithm. 

My external sort algorithm is more efficient than this as it sorts blocks of numbers and writes this into another file of the same size. Then it uses a merge algorithm to merge these blocks back into the original file. The multiple block merge utilises a Min Heap of these blocks objects which also contain a head variable that contains the first element of the block since it is the smallest as the block itself is sorted. Then these heads are used in the min heap to extract the minimum head in the file, which would be the smallest number in the file not already used. Then this is written to a buffer and written back into the original file once the buffer is filled further increases the efficiency. 

To sort the numbers into the blocks a standard O(nlgn) algorithm doesn't need to be used since the numbers are all bytes in my specific test so a O(n) radix sort can be used to further decrease the times, and this means a a large file of numbers can be sorted very quickly. It passes the tests set by the Uni in 11.5 seconds or second fastest. 
