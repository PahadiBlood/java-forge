package xyz.manojraw.exercises.interview.java8plusfeatures;

public record RecordTest() implements Cloneable{

    public static void main(String[] args) {
        RecordTest r = new RecordTest();
        try {
            RecordTest t= (RecordTest) r.clone();
            System.out.println(r==t);
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}


