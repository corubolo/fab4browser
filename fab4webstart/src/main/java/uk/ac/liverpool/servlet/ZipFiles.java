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
package uk.ac.liverpool.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ZipFiles extends HttpServlet implements Servlet {


	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		process(request, response);
	}

	protected void process(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		writeZipFile(response);

	}

	private synchronized void writeZipFile(HttpServletResponse response) throws IOException {

		File zipFile = new File(getServletContext().getRealPath("/jars/fab4browser.zip"));
		if (!zipFile.exists()){
			File zipDir = new File(getServletContext().getRealPath("/jars/"));
			ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile));

			String[] dirList = zipDir.list();
			byte[] readBuffer = new byte[16*1024];
			int bytesIn = 0;
			for (String element : dirList) {
				File f = new File(zipDir, element);
				if (f.isDirectory() || f.compareTo(zipFile)==0) {
					continue;
				}
				FileInputStream fis = new FileInputStream(f);
				String en;
				en = f.getName();
				ZipEntry anEntry = new ZipEntry(en);
				zos.putNextEntry(anEntry);
				while ((bytesIn = fis.read(readBuffer)) != -1)
					zos.write(readBuffer, 0, bytesIn);
				fis.close();
				zos.closeEntry();
			}
			zos.close();
		}


		response.setContentType("text/plain");  
		response.setStatus(response.SC_MOVED_TEMPORARILY);
		response.setHeader("Location", "./jars/fab4browser.zip");

	}

	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		process(request, response);
	}

}