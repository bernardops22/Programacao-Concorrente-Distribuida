import java.io.Serializable;

/**
 * @author bernardosantos
 */
public class ByteBlockRequest implements Serializable {

    public final int startIndex, length;

    public ByteBlockRequest(int startIndex, int length){
        this.startIndex = startIndex;
        this.length = length;
    }

    public int getStartIndex() {
        return startIndex;
    }

    public int getLength() {
        return length;
    }

    @Override
    public String toString() {
        return "[" + startIndex + " " + length + "]";
    }
}
