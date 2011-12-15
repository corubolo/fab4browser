/******************************************************************************
 *  
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU General Public License as published by the Free 
 * Software Foundation; either version 3 of the License, or (at your option) 
 * any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT 
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or 
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for 
 * more details.
 * 
 * You should have received a copy of the GNU General Public License along with 
 * this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Author     : Jerome Fuselier
 * Creation   : September 2011
 *  
 *****************************************************************************/

package uk.ac.liverpool.MXF;


public class Timer {
	
	long t;
	
	public Timer() {
		reset();
	}
	
	public void reset() {
		t = System.currentTimeMillis();
	}
	
	public long elapsed() {
		return System.currentTimeMillis() - t;
	}
	
	public void print(String s) {
		System.out.println(s + ": " + elapsed());
	}
}
