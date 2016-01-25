import java.io.*;

public class FileHeader implements Externalizable {
    private String fileName;
    private long length;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getLength() {
        return length;
    }

    public void setLength(long length) {
        this.length = length;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(fileName);
        out.writeLong(length);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        fileName = (String) in.readObject();
        length = in.readLong();
    }
}
