public class Node extends Thread {
    private Node next;
    private String data;

    public Node getNext(){
        return next;
    }public void setNext(Node n){
        next = n;
    }
    public String getData(){
        return data;
    }public void setData(String d){
        data = d;
    }
}
