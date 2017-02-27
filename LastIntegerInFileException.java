/**
 * Created by miteyan on 27/02/2017.
 */

public class LastIntegerInFileException extends Exception {
    int lastIntegerInFile;
    public LastIntegerInFileException(int x) {
        this.lastIntegerInFile = x;
    }
}
