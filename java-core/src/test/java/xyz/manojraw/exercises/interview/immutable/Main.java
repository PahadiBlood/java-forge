package xyz.manojraw.exercises.interview.immutable;

/**
 * Runs the class examples, then the record examples.
 * Each demo fixes the previous problem and leaves the next problem open.
 */
public class Main {

    public static void main(String[] args) throws Exception {
        ImmutableViaClassTest.SharesCallerList.demo();
        ImmutableViaClassTest.CopiesListInConstructor.demo();
        ImmutableViaClassTest.UnmodifiableGetterShallowClone.demo();
        ImmutableViaClassTest.CloneDisabledNotFinal.demo();
        ImmutableViaClassTest.FinalIntegerList.demo();
        ImmutableViaClassTest.SharesMutableElements.demo();
        ImmutableViaClassTest.DeepCopiedMarkers.demo();
        ImmutableViaClassTest.reflectionCeiling();

        ImmutableViaRecordTest.SharesCallerList.demo();
        ImmutableViaRecordTest.CopiesListInConstructor.demo();
        ImmutableViaRecordTest.UnmodifiableAccessorShallowClone.demo();
        ImmutableViaRecordTest.CopiedIntegerList.demo();
        ImmutableViaRecordTest.SharesMutableElements.demo();
        ImmutableViaRecordTest.DeepCopiedMarkers.demo();
        ImmutableViaRecordTest.reflectionCeiling();

        System.out.println();
        System.out.println("each situation behaved as these files describe");
    }
}
