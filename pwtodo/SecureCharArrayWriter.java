import java.io.CharArrayWriter;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetEncoder;
import java.util.Arrays;

class SecureCharArrayWriter extends CharArrayWriter {
    
    public SecureCharArrayWriter(int n) {
        super(n);
    }
    
    @Override
    public void write(int c) {
        synchronized(lock) {
            char[] arr = buf;
            super.write(c);
            clean(arr);
        }
    }
    
    @Override
    public void write(char[] c, int off, int len) {
        synchronized(lock) {
            char[] arr = buf;
            super.write(c, off, len);
            clean(arr);
        }
    }
    
    @Override
    public void write(String str, int off, int len) {
        synchronized(lock) {
            char[] arr = buf;
            super.write(str, off, len);
            clean(arr);
        }
    }
    
    public void sub(int i, int j, char[] arr, char c) {
        synchronized(lock) {
            // TODO: move
            int k = 0;
            for(int l = i; i < j; i++) {
                if(buf[l] != c) continue;
                buf[l] = arr[k++];
            }
        }
    }
    
    public void encode(CharsetEncoder ce, ByteBuffer bb) {
        synchronized(lock) {
            ce.encode(CharBuffer.wrap(buf, 0, count), bb, true);
        }
    }
    
    public void clear() {
        synchronized(lock) {
            Arrays.fill(buf, '\0');
        }
    }
    
    private void clean(char[] c) {
        if(buf != c)
            Arrays.fill(c, '\0');
    }
    
}
