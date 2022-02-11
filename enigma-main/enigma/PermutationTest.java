package enigma;

import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.Timeout;
import static org.junit.Assert.*;

import static enigma.TestUtils.*;



/** The suite of all JUnit tests for the Permutation class.
 *  @author
 */
public class PermutationTest {

    /** Testing time limit. */
   @Rule
   public Timeout globalTimeout = Timeout.seconds(5);

    /* ***** TESTING UTILITIES ***** */

    private Permutation perm;
    private String alpha = UPPER_STRING;

    /** Check that perm has an alphabet whose size is that of
     *  FROMALPHA and TOALPHA and that maps each character of
     *  FROMALPHA to the corresponding character of FROMALPHA, and
     *  vice-versa. TESTID is used in error messages. */
    private void checkPerm(String testId,
                           String fromAlpha, String toAlpha) {
        int N = fromAlpha.length();
        assertEquals(testId + " (wrong length)", N, perm.size());
        for (int i = 0; i < N; i += 1) {
            char c = fromAlpha.charAt(i), e = toAlpha.charAt(i);
            assertEquals(msg(testId, "wrong translation of '%c'", c),
                    e, perm.permute(c));
            assertEquals(msg(testId, "wrong inverse of '%c'", e),
                    c, perm.invert(e));
            int ci = alpha.indexOf(c), ei = alpha.indexOf(e);
            assertEquals(msg(testId, "wrong translation of %d", ci),
                    ei, perm.permute(ci));
            assertEquals(msg(testId, "wrong inverse of %d", ei),
                    ci, perm.invert(ei));
        }
    }

    /* ***** TESTS ***** */
//    package enigma;
//
//import org.junit.Test;
//import org.junit.Rule;
//import org.junit.rules.Timeout;
//import static org.junit.Assert.*;
//
//import static enigma.TestUtils.*;
//
//    /**
//     * The suite of all JUnit tests for the Permutation class. For the purposes of
//     * this lab (in order to test) this is an abstract class, but in proj1, it will
//     * be a concrete class. If you want to copy your tests for proj1, you can make
//     * this class concrete by removing the 4 abstract keywords and implementing the
//     * 3 abstract methods.
//     *
//     *  @author
//     */
//    public  class PermutationTest {

    /**
     * For this lab, you must use this to get a new Permutation,
     * the equivalent to:
     * new Permutation(cycles, alphabet)
     * @return a Permutation with cycles as its cycles and alphabet as
     * its alphabet
     * @see Permutation for description of the Permutation conctructor
     */
    Permutation  getNewPermutation(String cycles, Alphabet alphabet){
        return new Permutation( cycles,  alphabet);

    }


    /**
     * For this lab, you must use this to get a new Alphabet,
     * the equivalent to:
     * new Alphabet(chars)
     * @return an Alphabet with chars as its characters
     * @see Alphabet for description of the Alphabet constructor
     */
    Alphabet getNewAlphabet(String chars){
        return new Alphabet(chars);
    }

    /**
     * For this lab, you must use this to get a new Alphabet,
     * the equivalent to:
     * new Alphabet()
     * @return a default Alphabet with characters ABCD...Z
     * @see Alphabet for description of the Alphabet constructor
     */
//     Alphabet getNewAlphabet();

    /** Testing time limit. */

//        public Timeout globalTimeout = Timeout.seconds(5);

    /** Check that PERM has an ALPHABET whose size is that of
     *  FROMALPHA and TOALPHA and that maps each character of
     *  FROMALPHA to the corresponding character of FROMALPHA, and
     *  vice-versa. TESTID is used in error messages. */
    private void checkPerm(String testId,
                           String fromAlpha, String toAlpha,
                           Permutation perm, Alphabet alpha) {
        int N = fromAlpha.length();
        assertEquals(testId + " (wrong length)", N, perm.size());
        for (int i = 0; i < N; i += 1) {
            char c = fromAlpha.charAt(i), e = toAlpha.charAt(i);
            assertEquals(msg(testId, "wrong translation of '%c'", c),
                    e, perm.permute(c));
            assertEquals(msg(testId, "wrong inverse of '%c'", e),
                    c, perm.invert(e));
            int ci = alpha.toInt(c), ei = alpha.toInt(e);
            assertEquals(msg(testId, "wrong translation of %d", ci),
                    ei, perm.permute(ci));
            assertEquals(msg(testId, "wrong inverse of %d", ei),
                    ci, perm.invert(ei));
        }
    }

    /* ***** TESTS ***** */

//        @Test
//        public void checkIdTransform() {
//            Alphabet alpha = getNewAlphabet();
//            Permutation perm = getNewPermutation("", alpha);
//            checkPerm("identity", UPPER_STRING, UPPER_STRING, perm, alpha);
//        }



    // FIXME: Add tests here that pass on a correct Permutation and fail on buggy Permutations.
    @Test (expected = EnigmaException.class)
    public void checkWhole() {
        Permutation perm3 = getNewPermutation("", getNewAlphabet("WYJSFLPC"));
        perm3.invert('A');

    }
    @Test (expected = EnigmaException.class)
    public void checkAlphabet() {
        Permutation perm3 = getNewPermutation("", getNewAlphabet(""));
        perm3.invert('A');
      Permutation per4 = getNewPermutation("()", getNewAlphabet("ABC"));
        assertEquals( 3, per4.size());
        Permutation per_6 = getNewPermutation("((ABA)", getNewAlphabet("ABC"));
        assertEquals( 3, per_6.size());

    }


    @Test public  void checksize () {
        Permutation perm = getNewPermutation(" (BACD)", getNewAlphabet("ABCD"));
        assertEquals(4, perm.size());
        Permutation perm1 = getNewPermutation(" (BACD) (EGHF) ", getNewAlphabet("ABCDEFGHI"));
        assertEquals(9, perm1.size());
        Permutation perm2 = getNewPermutation("(QWS) (XYZL) (EGHF)", getNewAlphabet("VQWSEMFGHILXYZ"));
        assertEquals(14, perm2.size());
        Permutation perm3 = getNewPermutation("", getNewAlphabet("WYJSFLPC"));
        assertEquals(8, perm3.size());
//        Permutation per4 = getNewPermutation("()", getNewAlphabet("ABC"));
//        assertEquals( 3, per4.size());
        Permutation per5 = getNewPermutation("(ABC)",getNewAlphabet("ABC" ));
        Permutation per_6 = getNewPermutation("(AB)", getNewAlphabet("ABC"));
        assertEquals( 3, per_6.size());


    }
    @Test public void  checkPermute () {
        Permutation perm = getNewPermutation("(BACD)", getNewAlphabet("ABCD"));
        assertEquals('D', perm.permute('C'));
        assertEquals('B', perm.permute('D'));
        assertEquals(2, perm.permute(0));


        Permutation perm1 = getNewPermutation("   (B   ACD)    (EGHF)", getNewAlphabet("ABCDEFGHI"));
        assertEquals(4, perm1.permute(-4));
        assertEquals(1, perm1.permute(12));

        assertEquals(8, perm1.permute(8));
        assertEquals(0, perm1.permute(1));
        assertEquals('B', perm1.permute('D'));
        assertEquals('G', perm1.permute('E'));
//        Permutation per4 = getNewPermutation("()", getNewAlphabet("ABC"));
//        assertEquals( 'C', per4.permute('C'));
        Permutation perm3 = getNewPermutation("", getNewAlphabet("WYJSFLPC"));
        assertEquals(1, perm3.permute(1));
        assertEquals(5, perm3.invert(5));
        assertEquals(7, perm3.permute(7));
        assertEquals('C', perm3.permute('C'));
        assertEquals('C', perm3.invert('C'));
    }
    @Test public  void checkinvert (){
            Permutation perm = getNewPermutation("(BACD)", getNewAlphabet("ABCD"));
            assertEquals(2, perm.invert(-1));
            assertEquals( 2, perm.invert(-5));
            assertEquals(3, perm.invert(5));
            assertEquals('B', perm.invert('A'));
            assertEquals('D', perm.invert('B'));
            assertEquals(1, perm.invert(0));
            assertEquals(3, perm.invert(1));


        Permutation perm1 = getNewPermutation("(BACD) (EGHF)", getNewAlphabet("ABCDEFGHI"));
        assertEquals('D', perm1.invert('B'));
        assertEquals('I', perm1.invert('I'));
        assertEquals( 5, perm1.invert(4));

        Permutation perm2 = getNewPermutation("(QWS) (XYZL) (EGHF)", getNewAlphabet("VQWSEMFGHILXYZ"));
        assertEquals(0, perm2.invert(0));
        assertEquals('V', perm2.invert('V'));
        assertEquals('L', perm2.invert('X'));
        assertEquals(5, perm2.invert(5));
        assertEquals('M', perm2.invert('M'));

        Permutation perm3 = getNewPermutation("", getNewAlphabet("WYJSFLPC"));
        assertEquals(0, perm3.invert(0));
        assertEquals(1, perm3.invert(1));
        assertEquals(5, perm3.invert(5));
        assertEquals('C', perm3.invert('C'));
    }
    @Test public void checkDerangement (){
        Permutation perm = getNewPermutation("(BACD)", getNewAlphabet("ABCD"));
          /*  "(AA  "

           */
        assertEquals(true, perm.derangement());
        Permutation perm1 = getNewPermutation("(BACD) (EGHF)", getNewAlphabet("ABCDEFGHI"));
        assertEquals(false, perm1.derangement());
        Permutation perm2 = getNewPermutation("  (QWS) (XYZL) (EGHF)  (V)", getNewAlphabet("VQWSEMFGHILXYZ"));
        assertEquals(false, perm2.derangement());
        Permutation per4 = getNewPermutation("(ABC)(DEF)(GHIJK)(LMNOPQR)(STUVWXYZ)", getNewAlphabet("ABCDEFGHIJKLMNOPQRSTUVWXYZ"));
        assertEquals( true, per4.derangement());
        Permutation perm3 = getNewPermutation("", getNewAlphabet("WYJSFLPC"));
        assertEquals(false, perm3.derangement());
        Permutation perm4 = getNewPermutation(" ", getNewAlphabet("WYJSFLPC"));
        assertEquals(false, perm3.derangement());
        Permutation perm5 = getNewPermutation("(K)(C)", getNewAlphabet("KC"));
        assertEquals(false, perm5.derangement());


    }



    @Test
    public void checkIdTransform() {
        perm = new Permutation("", UPPER);
        checkPerm("identity", UPPER_STRING, UPPER_STRING);
    }

}
