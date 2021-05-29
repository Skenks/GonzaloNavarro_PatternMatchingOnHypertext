public class Node {
    int id;
    boolean reversed;
    String sequence;

    public Node(int id, boolean reversed, String sequence) {
        this.id = id;
        this.reversed = reversed;
        this.sequence = sequence;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public boolean isReversed() {
        return reversed;
    }

    public void setReversed(boolean reversed) {
        this.reversed = reversed;
    }

    public String getSequence() {
        return sequence;
    }

    public void setSequence(String sequence) {
        this.sequence = sequence;
    }
}
