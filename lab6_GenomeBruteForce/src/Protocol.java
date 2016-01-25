public class Protocol {
    static final byte NEW_CLIENT_MESSAGE = 0;
    static final byte GET_NEXT_RANGE_MESSAGE = 1;
    static final byte RESULTS_MESSAGE = 2;

    static final byte NEW_CLIENT_RESPONSE_MESSAGE = -1;
    static final byte NEXT_RANGE_RESPONSE_MESSAGE = -2;
    static final byte NO_RANGE_AVAILABLE_MESSAGE = -3;

    public static char[] unpackGenomeString(byte[] packedGenome, int count) {
        char[] genome = new char[count];
        for (int i = 0; i < count; i++) {
            int arrayPosition = i / 4;
            int offset = i % 4 * 2;
            byte current = (byte) (packedGenome[arrayPosition] >> offset & 3);
            char genomeChar = 0;
            switch (current) {
                case 0:
                    genomeChar = 'A';
                    break;
                case 1:
                    genomeChar = 'C';
                    break;
                case 2:
                    genomeChar = 'G';
                    break;
                case 3:
                    genomeChar = 'T';
                    break;
            }
            genome[i] = genomeChar;
        }
        return genome;
    }


    public static byte[] packGenomeString(char[] genome) {
        int len = genome.length / 4;
        len += genome.length % 8 == 0 ? 0 : 1;
        byte[] genomeBytes = new byte[len];
        for (int i = 0; i < genome.length; i++) {
            int arrayPosition = i / 4;
            int offset = i % 4 * 2;
            byte replace = 0;
            switch (genome[i]) {
                case 'A':
                    replace = 0;
                    break;
                case 'C':
                    replace = 1;
                    break;
                case 'G':
                    replace = 2;
                    break;
                case 'T':
                    replace = 3;
                    break;
            }
            genomeBytes[arrayPosition] |= replace << offset;
        }
        return genomeBytes;
    }

    public static void convertToNextSequence(char[] sequence) {
        boolean flag = true;
        int position = sequence.length - 1;
        while (flag) {
            if (sequence[position] != 'T') {
                flag = false;
            }
            switch (sequence[position]) {
                case 'A':
                    sequence[position] = 'C';
                    break;
                case 'C':
                    sequence[position] = 'G';
                    break;
                case 'G':
                    sequence[position] = 'T';
                    break;
                case 'T':
                    sequence[position] = 'A';
                    break;
            }
            position--;
        }
    }
}
