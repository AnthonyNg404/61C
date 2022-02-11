package enigma;

import jdk.jshell.spi.ExecutionControl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Collection;

import static enigma.EnigmaException.*;

/** Class that represents a complete enigma machine.
 *  @author  Xinyu Fu
 */
class Machine {

    /** A new Enigma machine with alphabet ALPHA, 1 < NUMROTORS rotor slots,
     *  and 0 <= PAWLS < NUMROTORS pawls.  ALLROTORS contains all the
     *  available rotors. */
    Machine(Alphabet alpha, int numRotors, int pawls,
            Collection<Rotor> allRotors) {
        _alphabet = alpha;

        if (numRotors <=1){
            throw new EnigmaException("wrong num of rotors");

        }
        if ( pawls >= numRotors || pawls < 0){
            throw new EnigmaException("wrong pawl numbers");
        }


        _numRotors = numRotors;
        _pawls = pawls;
        _Rotors = new HashMap<>();
        for (Rotor each : allRotors){
            _Rotors.put(each.name(), each);
        }


    }

    /** Return the number of rotor slots I have. */
    int numRotors() {
        return _numRotors;
    }

    /** Return the number pawls (and thus rotating rotors) I have. */
    int numPawls() {
        return _pawls;
    }

    /** Set my rotor slots to the rotors named ROTORS from my set of
     *  available rotors (ROTORS[0] names the reflector).
     *  Initially, all rotors are set at their 0 setting. */
    void insertRotors(String[] rotors) {
        int numFix = _numRotors - _pawls -1;
        _selctedRoters = new Rotor[_numRotors];
        if (rotors.length != _numRotors){
            throw new EnigmaException(" wrong number of rotors");
        }
         /* * B Beta I II III AAAA */

        for  (int i =0; i < _numRotors; i ++){
            if (_Rotors.get (rotors[i])== null){
                throw new EnigmaException(" no such a rotor");
            }

            _selctedRoters [i] = _Rotors.get(rotors[i]);
        }
        if (!_selctedRoters [0].reflecting()){
            throw new EnigmaException("the first one should be a reflector");
        }
        for ( int i = 1; i< 1 +numFix; i ++){
            if (_selctedRoters[i].rotates()|| _selctedRoters[i].reflecting()){
                throw  new EnigmaException("this is not a fixed rotor");
            }
        }
        for (int i = numFix +1; i< _numRotors; i ++){
            if (!_selctedRoters[i].rotates()){
                throw new EnigmaException("this is not a moving rotor ");
            }
        }
    }

    /** Set my rotors according to SETTING, which must be a string of
     *  numRotors()-1 characters in my alphabet. The first letter refers
     *  to the leftmost rotor setting (not counting the reflector).  */
    void setRotors(String setting, String ring) {
        String aRing = ring;
        String newseting= "";
        if (setting.length() != numRotors() -1){
            throw new EnigmaException("the setting number is wrong");
        }
//        if (ring!=""){
//            for (int j=0; j< ring.length(); j++){
//                int a = _alphabet.toInt(setting.charAt(j));
//                int b = _alphabet.toInt(ring.charAt(j));
//                newseting += String.valueOf(_alphabet.toChar(wrap(a-b)));  ;
//
//            }
//            setting = newseting;
//        }

        for (int i =1; i< _numRotors; i++){
            _selctedRoters[i].set(setting.charAt(i-1));

        }
//        for (int i =0; i< _numRotors; i++) {
//            _selctedRoters[i].setNotch(_selctedRoters[i].aRing.substring(0,1));
//            aRing = aRing.substring(1);
//
//        }
    }

    final int wrap(int p) {
        int r = p % _alphabet.size();
        if (r < 0) {
            r += _alphabet.size();
        }
        return r;
    }


    /** Set the plugboard to PLUGBOARD. */
    void setPlugboard(Permutation plugboard) {
        _plugboard =  plugboard;

    }

    /** Returns the result of converting the input character C (as an
     *  index in the range 0..alphabet size - 1), after first advancing

     *  the machine. */
    int convert(int c) {
        ifRotate = new Boolean[_numRotors];
        Arrays.fill(ifRotate, false);
        for (int i =_numRotors -1 ; i >= 0; i--) {
            if (i == _numRotors - 1) {
                ifRotate[i] = true;
                if (_selctedRoters[i].atNotch()){
                    if (_selctedRoters[i-1].rotates()){
                        ifRotate[i - 1] = true;
                    }
                }
            } else if (i == 0) {
                ifRotate[i] = false;
            } else {
                if (_selctedRoters[i].rotates()) {   /* if I can rotate  */
                    if (_selctedRoters[i].atNotch()) {   /* if i'm at the notch */
                        if (_selctedRoters[i - 1].rotates()) {  /* if my left isn't a fix rotor */
                            ifRotate[i] = true;
                            ifRotate[i - 1] = true;
                        }
                    }
                }
            }
        }
            for (int j = 0; j< _numRotors; j++){   /* rotate rotors which should rotate */
                if(ifRotate[j]){
                    _selctedRoters[j].advance();
                }
            }
            c = _plugboard.permute(c);
            for (int k = _numRotors -1; k >=0; k--){
                c = _selctedRoters[k].convertForward(c);
            }

            for (int m = 1; m < _numRotors; m++){
                c = _selctedRoters[m].convertBackward(c);
            }
            c = _plugboard.invert(c);
            return c;




    }

    /** Returns the encoding/decoding of MSG, updating the state of
     *  the rotors accordingly. */
    String convert(String msg) {
        int converted;
        if (msg =="" || msg =="\n"){
           return "\n";
        }
        msg = msg.replaceAll("\\s", "");
        String covertedString ="";
            for (int i =0; i< msg.length(); i++){
                if (!_alphabet.contains(msg.charAt(i))){
                    throw new EnigmaException("this msg is not in alphabet");

                }
                converted =(convert(_alphabet.toInt(msg.charAt(i))));
                covertedString += String.valueOf(_alphabet.toChar(converted));
            }

        return covertedString;

    }

    /** Common alphabet of my rotors. */
    private final Alphabet _alphabet;
    private  int _numRotors ;
    private int _pawls;
    private  HashMap<String, Rotor> _Rotors;
    private  Rotor [] _selctedRoters;
    private  Permutation _plugboard;
    private Boolean [] ifRotate;



    // FIXME: ADDITIONAL FIELDS HERE, IF NEEDED.
}
