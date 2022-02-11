package enigma;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import java.sql.SQLSyntaxErrorException;
import java.util.*;

import static enigma.EnigmaException.*;

/** Enigma simulator.
 *  @author  Xinyu Fu
 */
public final class Main {


    /** Process a sequence of encryptions and decryptions, as
     *  specified by ARGS, where 1 <= ARGS.length <= 3.
     *  ARGS[0] is the name of a configuration file.
     *  ARGS[1] is optional; when present, it names an input file
     *  containing messages.  Otherwise, input comes from the standard
     *  input.  ARGS[2] is optional; when present, it names an output
     *  file for processed messages.  Otherwise, output goes to the
     *  standard output. Exits normally if there are no errors in the input;
     *  otherwise with code 1. */
    public static void main(String... args) {
        try {
            new Main(args).process();
            return;
        } catch (EnigmaException excp) {
            System.err.printf("Error: %s%n", excp.getMessage());
        }
        System.exit(1);
    }

    /** Check ARGS and open the necessary files (see comment on main). */
    Main(String[] args) {
        if (args.length < 1 || args.length > 3) {
            throw error("Only 1, 2, or 3 command-line arguments allowed");
        }
        _config = getInput(args[0]);

        if (args.length > 1) {
            _input = getInput(args[1]);
        } else {
            _input = new Scanner(System.in);
        }

        if (args.length > 2) {
            _output = getOutput(args[2]);
        } else {
            _output = System.out;
        }
    }
    /** Return a Scanner reading from the file named NAME. */
    private Scanner getInput(String name) {
        try {
            return new Scanner(new File(name));
        } catch (IOException excp) {
            throw error("could not open %s", name);
        }
    }

    /** Return a PrintStream writing to the file named NAME. */
    private PrintStream getOutput(String name) {
        try {
            return new PrintStream(new File(name));
        } catch (IOException excp) {
            throw error("could not open %s", name);
        }
    }

    /** Configure an Enigma machine from the contents of configuration
     *  file _config and apply it to the messages in _input, sending the
     *  results to _output. */

    private void process() {
        Machine machine =  readConfig();
        String converted = "";
        message = "";
        Boolean firstTime = true;
        String thisLine;
        if (!_input.hasNext()){
            throw new EnigmaException("no input");
        }


        while (_input.hasNextLine()){
            thisLine = _input.nextLine();
            if (firstTime){
                if (!thisLine.contains("*")){
                    throw new EnigmaException(" wrong input");
                }
            }
            firstTime = false;
            if (thisLine.contains("*")){
                setUp(machine,thisLine);
//                if (ring!=""){
//                    MovingRotor.setNotch (notch,  ring, _numMovRotors,_numRotors,_alphabet);
//
//                }


            }
            else{
                converted = machine.convert(thisLine);
                printMessageLine(converted);
            }
        }
    }

    /** Return an Enigma machine configured from the contents of configuration
     *  file _config. */
    private Machine readConfig() {

        try {
            if (!_config.hasNextLine()) {
                throw new EnigmaException("empty config");
            }
            int _num =0;
            boolean hasCalled = false;
            while (_config.hasNextLine()){   /* get alphabet */
                if (!hasCalled){
                    thisLine = _config.nextLine();
                    thisLine = thisLine.trim();

                }
                if  (_num ==0){
                    _alphabet = new Alphabet(thisLine);
                    _num += 1;
                }else if (_num ==1) {

                    String [] numTotalMoving = thisLine.split("\\s");
                    if (numTotalMoving.length !=2 ){
                        throw  new EnigmaException( "you should have 2 numbers on the second line");
                    }
                    _numRotors= Integer.parseInt(numTotalMoving[0]);
                    _numMovRotors = Integer.parseInt(numTotalMoving[1]);
                    _num +=1;
                }
                else  {
                    _num += 1;
                    _cycle ="";
                    String [] rotorInfo= thisLine.split("\\s+");
                    hasCalled = false;
//                    if (rotorInfo[1].charAt(0)!= 'M' && rotorInfo[1].charAt(0)!='N' && rotorInfo[1].charAt(0)!= 'R'){
//                        throw new EnigmaException("the type of this rotor is incorrect");
//                    }
                    if (rotorInfo.length < 3){
                        throw new EnigmaException("there are not enough info of this rotor");
                    }
                    if (rotorInfo[1].length()==1){
                        if (rotorInfo[1].charAt(0)!='R' && rotorInfo[1].charAt(0)!='N'){
                            throw new EnigmaException("Moving rotor should have notches");
                        }
                    }
                    rotorName = rotorInfo[0];
                    rotorType = rotorInfo[1].charAt(0);
                    if (rotorType == 'M'){
                        notch = rotorInfo[1].substring(1);
                        for (int i =0; i< notch.length(); i++){
                            if (!_alphabet.contains(notch.charAt(i))){
                                throw new EnigmaException("alphabet doesn't have this notch");
                            }
                        }
                    }
                    for (int i = 2; i< rotorInfo.length ; i++){
                            _cycle += rotorInfo[i];
                    }
                    while (_config.hasNextLine()){
                        thisLine =  _config.nextLine().trim();
                        if (thisLine.length() ==0){
                            continue;
                        }
                        else if (thisLine.charAt(0)=='('){
                            _cycle += thisLine;
                        } else{
                            hasCalled =true;
                            thisLine= thisLine.trim();
                            break;
                        }
                    }
                    Rotor rotor = readRotor();
                    _allRotors.add(rotor);
                }
            }
            if (thisLine.length() !=0) {

                addLastRotor(thisLine);
            }
            if (_allRotors.isEmpty()){
                throw new EnigmaException("there is no rotors");
            }
            return new Machine(_alphabet, _numRotors, _numMovRotors, _allRotors);
        } catch (NoSuchElementException excp) {
            throw error("configuration file truncated");
        }
    }
    void addLastRotor (String thisLine) {
        String [] rotorInformation = thisLine.split("\\s+");
        boolean containsLastone =false;
        for (Rotor each: _allRotors){
            if (each.name()==rotorInformation[0]){
                containsLastone =true;
            }
        }
        if (!containsLastone) {
            _cycle ="";
            rotorName = rotorInformation[0];
            rotorType = rotorInformation[1].charAt(0);
            if (rotorType == 'M') {
                notch = rotorInformation[1].substring(1);
                for (int i = 0; i < notch.length(); i++) {
                    if (!_alphabet.contains(notch.charAt(i))) {
                        throw new EnigmaException("alphabet doesn't have this notch");
                    }
                }
                for (int i = 2; i< rotorInformation.length ; i++){
                    _cycle += rotorInformation[i];
                }
            }
            Rotor lastRotor = readRotor();
            _allRotors.add(lastRotor);
        }

    }
    /** Return a rotor, reading its description from _config. */
    private Rotor readRotor() {
        Rotor thisRotor;

        try {
            if (rotorType =='M'){
                thisRotor =new MovingRotor(rotorName, new Permutation(_cycle, _alphabet), notch);
            }
            else if (rotorType =='N'){
                thisRotor = new FixedRotor(rotorName, new Permutation(_cycle, _alphabet) );
            }
            else {
                thisRotor = new Reflector(rotorName, new Permutation(_cycle, _alphabet));
            }
            return thisRotor;

        } catch (NoSuchElementException excp) {
            throw error("bad rotor description");
        }
    }

    /** Set M according to the specification given on SETTINGS,
     *  which must have the format specified in the assignment. */
     /*
       * B Beta I II   AAAA (
       *  B Beta III IV I AXLE BEAR (BQ)


      */
    private void setUp(Machine M, String settings) {
        String newSetting="";
        settings = settings.substring(1);
        settings = settings.trim();
        String[] selectedRotor = settings.split("\\s+");

        String[] rotors = new String[_numRotors];
        _plugborad ="";
        for (int i= 0; i< selectedRotor.length; i++){
            if (i< _numRotors ){
                rotors[i] = selectedRotor[i];
            }
            else if (i == _numRotors){
                _setting = selectedRotor[i];
            }
            else if (i> _numRotors){
                if (_setting.contains("(")){
                    _plugborad += selectedRotor[i];
                }
                else {

                    ring = selectedRotor[i];
                }
            }
            else{
               _plugborad += selectedRotor[i];
            }
        }

        if (ring!=""){   /* change the setting */
            for (int j=0; j < ring.length(); j++) {
                newSetting += String.valueOf( _alphabet.toChar(wrap(_setting.charAt(j) - ring.charAt(j))));

            }
            _setting = newSetting;


        }

        M.insertRotors(rotors);
        M.setPlugboard(new Permutation(_plugborad,_alphabet));
        M.setRotors(_setting, ring);

    }
    /* 01234 567890 */
    final int wrap(int p) {
        int r = p % _alphabet.size();
        if (r < 0) {
            r += _alphabet.size();
        }
        return r;
    }

    /** Print MSG in groups of five (except that the last group may
     *  have fewer letters). */
    private void printMessageLine(String msg) {
        msg = msg.replaceAll("\\s+","");
        String string;
        while (msg.length() >=5){
            string = msg.substring(0, 5);
            _output.print(string+ " ");
            msg = msg.substring(5);
        }
        _output.println(msg);

    }


    /** Alphabet used in this machine. */
    private Alphabet _alphabet;
    private  int _numRotors;
    private int _numMovRotors;

    /** Source of input messages. */
    private Scanner _input;

    /** Source of machine configuration. */
    private Scanner _config;
    private String rotorName;
    private char rotorType;
    private String notch;
    private String _cycle;
    private String _setting;
    private String  _plugborad;
    private String message;
    private String convertedMessage;
    private String thisLine;
    private Collection<Rotor> _allRotors = new ArrayList<Rotor>();
    private int a =0;
    boolean firstTime = false;
    private   String ring="";


    /** File for encoded/decoded messages. */
    private PrintStream _output;
}
