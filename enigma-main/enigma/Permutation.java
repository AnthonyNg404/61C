package enigma;

import java.awt.desktop.PreferencesEvent;
import java.util.ArrayList;

import static enigma.EnigmaException.*;

/** Represents a permutation of a range of integers starting at 0 corresponding
 *  to the characters of an alphabet.
 *  @author Xinyu Fu
 */
class Permutation {

    /** Set this Permutation to that specified by CYCLES, a string in the
     *  form "(cccc) (cc) ..." where the c's are characters in ALPHABET, which
     *  is interpreted as a permutation in cycle notation.  Characters in the
     *  alphabet that are not included in any cycle map to themselves.
     *  Whitespace is ignored. */
    private String [] _sperateCycle;

    Permutation(String cycles, Alphabet alphabet) {
        _alphabet = alphabet;
        cycles = cycles.replaceAll("\\s","");
        boolean isOpen = false;
        int lenOfPer = 0;


        if ( cycles.length() >0 && cycles.length() <3)
            throw new EnigmaException("in valid cycle input");
        else if (cycles.length() ==0){
            return;

        }else if (cycles.charAt(0)=='(' ){
            isOpen =true;
        }else{
            throw new EnigmaException("the first letter should be a (");
        }
        for (int i = 0; i< cycles.length() ; i ++){
            if (isOpen){/* if we are after (    */
                if (cycles.charAt(i) == ')'){
                    if (cycles.charAt(i-1)=='('){
                        throw new EnigmaException("you shouldn't have a () in cycles");
                    }
                    else{
                        if (lenOfPer == cycles.length()-1){
                            break;
                        }
                        isOpen = false;
                        lenOfPer = 0;
                    }
                } else if (lenOfPer == 0 && cycles.charAt(i)== '('){
                    lenOfPer += 1;

                }
                else if (  ! _alphabet.contains(cycles.charAt(i)) ){
                    throw new EnigmaException(" this letter in cycles is not in the alphabet" + cycles.charAt(i));
                }
                else {
                    lenOfPer += 1;
                }
            } else{
                if ( cycles.charAt(i ) !='(' ){
                    throw new EnigmaException("you should have a (");

                } else {
                    isOpen = true;
                }

            }
        }
        cycles = cycles.substring(1, cycles.length()-1);
        String temp_cycle = "";
        _sperateCycle = cycles.split("\\)\\(");
        for( String each: _sperateCycle) temp_cycle += each;
        for (int i = 0; i < temp_cycle.length() - 1; i++) {
            for (int j = i + 1; j < temp_cycle.length(); j++) {
                if (temp_cycle.charAt(i) == temp_cycle.charAt(j)) {
                    throw new IllegalArgumentException(" duplicated cycles");
                }
            }
        }
    }

    /** Add the cycle c0->c1->...->cm->c0 to the permutation, where CYCLE is
     *  c0c1...cm. */
    private void addCycle(String cycle) {
        cycle += cycle.charAt(0);
    }

    /** Return the value of P modulo the size of this permutation. */
    final int wrap(int p) {
        int r = p % size();
        if (r < 0) {
            r += size();
        }
        return r;
    }

    /** Returns the size of the alphabet I permute. */
    int size() {
        return _alphabet.size();

    }

    /** Return the result of applying this permutation to P modulo the
     *  alphabet size. */
    int permute(int p) {

        while ( !( p>= 0 && p < _alphabet.size())){
             p = wrap(p);

        }
        if ( !_alphabet.contains(_alphabet.toChar(p))){
            throw  new EnigmaException(" this letter is not in the alphabet ");

        }
        if (_sperateCycle == null || _sperateCycle.length==0){
            return p;
        }
       for (String each: _sperateCycle){

           char b = _alphabet.toChar(p);

           int index = each.indexOf(_alphabet.toChar(p));

           if (index>=0){
                return _alphabet.toInt(each.charAt((index +1) % each.length()));
           }
       }
       return p;
    }

    /** Return the result of applying the inverse of this permutation
     *  to  C modulo the alphabet size. */
    int invert(int c) {

        while ( !( c>= 0 && c < _alphabet.size())){
            c = wrap(c);

        }
        if ( !_alphabet.contains(_alphabet.toChar(c))){
            throw  new EnigmaException(" this letter is not in the alphabet ");

        }
        if (_sperateCycle == null || _sperateCycle.length==0){
            return c;
        }
        for (String each: _sperateCycle){
            int index = each.indexOf(_alphabet.toChar(c));
            if (index >= 0){
                if (index -1 < 0){
                    return _alphabet.toInt(each.charAt(each.length()-1));
                } else{
                    return _alphabet.toInt(each.charAt((index -1) % each.length()));
                }
            }
        }
        return c;
    }

    /** Return the result of applying this permutation to the index of P
     *  in ALPHABET, and converting the result to a character of ALPHABET. */
    char permute(char p) {
        if ( !_alphabet.contains(p)){
            throw  new EnigmaException(" this letter is not in the alphabet ");

        }
        if (_sperateCycle == null || _sperateCycle.length==0){
            return p;
        }
        for (String each: _sperateCycle){
            int index = each.indexOf(p);
            if (index >=0){
                return  each.charAt((index + 1 ) % each.length());
            }
        }
        return p;
    }

    /** Return the result of applying the inverse of this permutation to C. */
    char invert(char c) {
        if ( !_alphabet.contains(c)){
            throw  new EnigmaException(" this letter is not in the alphabet ");
        }
        if (_sperateCycle == null || _sperateCycle.length==0){
            return c;
        }
        for (String each : _sperateCycle){
            int index = each.indexOf(c);
            if (index >0){
                return each.charAt(index -1);
            } else if (index ==0){
                return each.charAt(each.length()-1);
            }
        }
        return c;
    }

    /** Return the alphabet used to initialize this Permutation. */
    Alphabet alphabet() {
        return _alphabet;
    }

    /** Return true iff this permutation is a derangement (i.e., a
     *  permutation for which no value maps to itself). */
    boolean derangement() {
        int len = 0;
        if (_sperateCycle == null || _sperateCycle.length ==0){
            return false;
        }

        for (String each : _sperateCycle){
            if (each.length() == 1){
                return false;
            }
            len += each.length();
        }
        return len == _alphabet.size();
    }

    /** Alphabet of this permutation. */
    private Alphabet _alphabet;

}
