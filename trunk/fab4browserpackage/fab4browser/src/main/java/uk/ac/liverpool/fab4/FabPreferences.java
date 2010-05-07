/*******************************************************************************
 *
 *  * Copyright (C) 2007, 2010 - The University of Liverpool
 *  * This program is free software; you can redistribute it and/or modify it under the terms
 *  * of the GNU General Public License as published by the Free Software Foundation; either version 3 of the License,
 *  * or (at your option) any later version.
 *  * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 *  * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *  * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *  *
 *  * Author: Fabio Corubolo
 *  * Email: corubolo@gmail.com
 * 
 *******************************************************************************/

package uk.ac.liverpool.fab4;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.InvalidPropertiesFormatException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;


/**
 * Simple class to store Fab4 preferences
 * @author fabio
 *
 */
public class FabPreferences {

	public static final String FAB4_ANNO_PREFS = ".fab4_anno_prefs";
	public static final File defprofile = new File(Fab4utils.USER_HOME_DIR,
			FabPreferences.FAB4_ANNO_PREFS);
	private Properties props = new Properties();
	private boolean firstRun = false;
	private List<Runnable> beforeSavePrefs = new LinkedList<Runnable>();

	// these properties are for web service interaction, so they are not
	// recorded.
	public String wsuser = null;
	public String wsserver = null;
	public String wspass = null;
	public String searchaddress = null;


	public FabPreferences() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				savePrefs();
			}
		});
		if (FabPreferences.defprofile.exists()) {
			FileInputStream fis;
			try {
				fis = new FileInputStream(FabPreferences.defprofile);
				props.loadFromXML(fis);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (InvalidPropertiesFormatException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else
			firstRun = true;
	}

	public void addBeforeSavePrefs(Runnable r) {
		beforeSavePrefs.add(r);
	}

	public void savePrefs() {
		for (Runnable r : beforeSavePrefs)
			r.run();

		try {
			FileOutputStream fos = new FileOutputStream(FabPreferences.defprofile);
			props
			.storeToXML(
					fos,
					"Fab4 annotation property file. This file conatins the preferences. please do not delete.");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public Properties getProps() {
		return props;
	}

	public boolean isFirstRun() {
		return firstRun;
	}

}
