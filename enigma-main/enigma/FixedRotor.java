package enigma;

import static enigma.EnigmaException.*;

/** Class that represents a rotor that has no ratchet and does not advance.
 *  @author  Xinyu Fu
 */
class FixedRotor extends Rotor {

    /** A non-moving rotor named NAME whose permutation at the 0 setting
     * is given by PERM. */
    FixedRotor(String name, Permutation perm) {
        super(name, perm);
        _perm = perm;
    }
    @ Override
    boolean rotates() {
        return false;
    }
 /* fixed rotor doesn't have a notch*/
   @Override
    boolean reflecting() {
        return false;
    }


    private Permutation _perm;


}
