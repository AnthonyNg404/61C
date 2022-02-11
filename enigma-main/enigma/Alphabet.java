package enigma;

/** An alphabet of encodable characters.  Provides a mapping from characters
 *  to and from indices into the alphabet.
 *  @author
 */
class Alphabet {
    private String _chars;

    /** A new alphabet containing CHARS.  Character number #k has index
     *  K (numbering from 0). No character may be duplicated. */
    Alphabet(String chars) {
        for (int i = 0; i < chars.length() - 1; i++) {
            for (int j = i + 1; j < chars.length(); j++) {
                if (chars.charAt(i) == chars.charAt(j)) {
                    throw new IllegalArgumentException(" duplicated chars");
                }
            }
        }
        for (int i =0; i< chars.length(); i++){
            if (chars.charAt(i) == '*' ||
                    chars.charAt(i) == ')' || chars.charAt(i) == '(' ||chars.charAt(i)==' '){
                throw new EnigmaException("invalid alphabet input");
            }
        }
        this._chars= chars;
    }

    /** A default alphabet of all upper-case characters. */
    Alphabet() {
       this("ABCDEFGHIJKLMNOPQRSTUVWXYZ");

    }

    /** Returns the size of the alphabet. */
    int size() {
        return _chars.length();
    }

    /** Returns true if CH is in this alphabet. */
    boolean contains(char ch) {
        return _chars.indexOf(ch) >=0;
    }

    /** Returns character number INDEX in the alphabet, where
     *  0 <= INDEX < size(). */
    char toChar(int index) {
        return _chars.charAt(index);
    }

    /** Returns the index of character CH which must be in
     *  the alphabet. This is the inverse of toChar(). */
    int toInt(char ch) {
        return _chars.indexOf(ch);
    }

}
