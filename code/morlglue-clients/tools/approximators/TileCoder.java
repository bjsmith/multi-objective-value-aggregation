/*
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package tools.approximators;

import java.util.Random;

/**
 * This code is a direct port from C to Java of the code at
 * <a target=_new href=http://rlai.cs.ualberta.ca/RLAI/RLtoolkit/tilecoding.html>the RLAI Tile Coding Page</a>.
 *
 * It was a very quick port.  It has been tested to some degree, but as the license says, it is provided
 * with no warranty.  And the license might be a lie here, because I copied it from
 * the group's web page.
 *
 * @author Brian Tanner
 * @author Richard Sutton
 * @author Mark Lee
 */
public class TileCoder {

    final static int MAX_NUM_VARS = 300;       // Maximum number of variables in a grid-tiling
    final static int MAX_NUM_COORDS = 100;     // Maximum number of hashing coordinates
    final static int MaxLONGINT = 2147483647;
    static int qstate[] = new int[TileCoder.MAX_NUM_VARS];
    static int base[] = new int[MAX_NUM_VARS];
    static int coordinates[] = new int[MAX_NUM_VARS * 2 + 1];   /* one interval number per relevant dimension */


    /**
     * Should this be a static method?
     * @param the_tiles Integer array to fill up with tile indices
     * @param tileStartOffset Where to start filling the array (used if calling multiple times with 1 array)
     * @param num_tilings   Number of tilings (number of array spots to fill)
     * @param memory_size   Maximum number for each array index
     * @param doubles       The array of double variables
     * @param ints          The array of int variables
     */
    public synchronized void tiles(
            int the_tiles[], // provided array contains returned tiles (tile indices)
            int tileStartOffset,
            int num_tilings, // number of tile indices to be returned in tiles
            int memory_size, // total number of possible tiles
            double doubles[], // array of doubleing point variables
            int ints[]) // array of integer variables
    {
        int num_doubles = doubles.length;
        int num_ints = ints.length;
        int num_coordinates = num_doubles + num_ints + 1;

        for (int i = 0; i < num_ints; i++) {
            coordinates[num_doubles + 1 + i] = ints[i];
        }

        /* quantize state to integers (henceforth, tile widths == num_tilings) */
        for (int i = 0; i < num_doubles; i++) {
            qstate[i] = (int) (doubles[i] * num_tilings); //This used to be math.floor be we can just cast
            base[i] = 0;
        }

        int i = 0;
        /*compute the tile numbers */
        for (int j = 0; j < num_tilings; j++) {

            /* loop over each relevant dimension */
            for (i = 0; i < num_doubles; i++) {

                /* find coordinates of activated tile in tiling space */
                if (qstate[i] >= base[i]) {
                    coordinates[i] = qstate[i] - ((qstate[i] - base[i]) % num_tilings);
                } else {
                    coordinates[i] = qstate[i] + 1 + ((base[i] - qstate[i] - 1) % num_tilings) - num_tilings;
                }

                /* compute displacement of next tiling in quantized space */
                base[i] += 1 + (2 * i);
            }
            /* add additional indices for tiling and hashing_set so they hash differently */
            coordinates[i] = j;

            the_tiles[tileStartOffset + j] = (int) hash_UNH(coordinates, num_coordinates, memory_size, 449);
        }
    }

    /* hash_UNH
    Takes an array of integers and returns the corresponding tile after hashing
     */
    static final int RNDSEQNUMBER = 16384;
    static Random theRand = new Random();
    static int rndseq[] = new int[RNDSEQNUMBER];
    static boolean first_call = true;

    long hash_UNH(int ints[], int num_ints, long m, int increment) {
        int i, k;
        long index = 0;
        long sum = 0;

        /* if first call to hashing, initialize table of random numbers */
        if (first_call) {
            for (k = 0; k < RNDSEQNUMBER; k++) {
                rndseq[k] = 0;
                for (i = 0; i < 4/*int(sizeof(int))*/; ++i) {
                    rndseq[k] = (rndseq[k] << 8) | (theRand.nextInt() & 0xff);
                }//do these need to change?
            }
            first_call = false;
        }

        for (i = 0; i < num_ints; i++) {
            /* add random table offset for this dimension and wrap around */
            index = ints[i];
            index += (increment * i);
            /* index %= RNDSEQNUMBER; */
            index = index & (RNDSEQNUMBER - 1);
            while (index < 0) {
                index += RNDSEQNUMBER;
            }

            /* add selected random number to sum */
//			System.out.println("Sum ("+sum+") += (long)rndseq["+(int)index+"] which is "+ (long)rndseq[(int)index]);
            sum += (long) rndseq[(int) index];
        }
        index = (int) (sum % m);
        while (index < 0) {
            index += m;
        }

        /* printf("index is %d \n", index); */

        return (index);
    }

}
