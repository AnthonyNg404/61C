package enigma;

import static enigma.EnigmaException.*;

/** Class that represents a rotating rotor in the enigma machine.
 *  @author Xinyu Fu
 */
class MovingRotor extends Rotor {

    /** A rotor named NAME whose permutation in its default setting is
     *  PERM, and whose notches are at the positions indicated in NOTCHES.
     *  The Rotor is initally in its 0 setting (first character of its
     *  alphabet).
     */
    MovingRotor(String name, Permutation perm, String notches) {
        super(name, perm);
        _notches = notches;
        _perm = perm;
    }

     void setNotch (String notch, String string,int  _numMovRotors,int _numRotors,Alphabet _alphabet){
        int fixed = _numRotors - _numMovRotors;
        for (int j =0; j< _notches.length(); j++){
            if (times ==0){
                int a = _alphabet.toInt((_notches.charAt(j)));
                int b  = _alphabet.toInt(string.charAt(1+ fixed +times));
                int p = a-b;
                int r = p % _alphabet.size();
                if (r < 0) {
                    r += _alphabet.size();
                }
                newNotch += String.valueOf(_alphabet.toChar(r));
                times +=1;

            }
        }

    }


    @Override
    boolean rotates() {
        return true;


    }
    @Override
    boolean atNotch() {
        for ( int i =0; i< _notches.length(); i++ ){
            if (_notches.charAt(i) == _perm.alphabet().toChar(setting())){
                return true;
            }

        }
        return false;



    }
//    final static int wrap(int p) {
//        int r = p % _alphabet.size();
//        if (r < 0) {
//            r += _alphabet.size();
//        }
//        return r;
//    }

    @Override
    boolean reflecting() {
        return false;
    }

    @Override
    void advance() {
        set((setting() +1 ) % _perm.alphabet().size());

    }

     private String _notches;
    private Permutation _perm;
    static private String newNotch ="";
    static private int times =0;


}
