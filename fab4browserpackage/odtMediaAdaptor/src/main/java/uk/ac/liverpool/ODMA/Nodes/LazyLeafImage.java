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
/**
 * Author: Fabio Corubolo - f.corubolo@liv.ac.uk
 * (c) 2005 University of Liverpool
 */
package uk.ac.liverpool.ODMA.Nodes;


import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

import javax.imageio.ImageIO;

import multivalent.Context;
import multivalent.INode;
import multivalent.node.LeafImage;
import uk.ac.liverpool.ODMA.EncrypredPackageException;
import uk.ac.liverpool.ODMA.ODFpackage;

/**
 * @author fabio
 * 
 */
public class LazyLeafImage extends LeafImage {

	boolean lazy;

	String address;

	private ODFpackage odfPkg;

	int lw, lh;

	static private ImLoadQueue lq;

	static final LazyLeafImage holdDummy = new LazyLeafImage("holdDummy");

	static final LazyLeafImage closeDummy = new LazyLeafImage("closeDummy");

	static final LazyLeafImage continueDummy = new LazyLeafImage(
	"continueDummy");

	private LazyLeafImage(String name) {
		super(name, null, null, (Image) null);
	}

	public LazyLeafImage(String name, Map<String, Object> attr, INode parent,
			final ODFpackage odfPack, final String address, boolean delayLoading) {
		super(name, attr, parent, (Image) null);
		if (LazyLeafImage.lq == null)
			LazyLeafImage.lq = ImLoadQueue.createImLoadQueue();
		lazy = true;
		this.address = address;
		odfPkg = odfPack;
		if (!delayLoading)
			try {
				Image i;

				i = ImageIO.read(odfPack.getFileIS(address));
				if (i != null)
					setImage(i.getScaledInstance(lw, lh, Image.SCALE_SMOOTH));
			} catch (IOException e) {
				e.printStackTrace();
			} catch (EncrypredPackageException e) {
				e.printStackTrace();
			} finally {
			}
			else
				LazyLeafImage.lq.add(this);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see multivalent.node.LeafImage#paintNodeContent(multivalent.Context,
	 *      int, int)
	 */
	@Override
	public boolean paintNodeContent(Context cx, int start, int end) {
		if (start == 0) {
			//long t = System.currentTimeMillis();
			Graphics2D g = cx.g;
			int w = getImage().getWidth(this), h = getImage().getHeight(this);
			if (w != -1 && h != -1)
				try {
					int x = 0, y = Math.max(baseline - h, 0);
					g.drawImage(getImage(), x, y, this);
				} catch (Exception shouldnthappen) {
					shouldnthappen.printStackTrace();
				}
				cx.x += bbox.width;
				//System.out.println("paint "+ address + " "+(System.currentTimeMillis()-t) );
		}
		return false;

	}



	public static void startLoading() {
		// lq.setDaemon(true);
		if (LazyLeafImage.lq != null)
			LazyLeafImage.lq.restart();
	}

	public static void stopLoading() {
		if (LazyLeafImage.lq != null)
			LazyLeafImage.lq.hold();
	}

	public static boolean hasQueue() {
		if (LazyLeafImage.lq != null && LazyLeafImage.lq.size() > 0)
			return true;
		return false;
	}

	/**
	 * 
	 */
	public void loadLazy() {
		try {
			BufferedImage i;

			{
				i = ImageIO.read(odfPkg.getFileIS(address));
				if (i != null) {
					Image ii = i.getScaledInstance(lw, lh, Image.SCALE_SMOOTH);
					setImage(ii);
				}
			}
			if (i!=null) {
				reformat(this);
				repaint();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (EncrypredPackageException e) {
			e.printStackTrace();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see multivalent.node.LeafImage#setSize(int, int)
	 */
	@Override
	public void setSize(int width, int height) {
		lw = width;
		lh = height;
		super.setSize(width, height);
	}

	public static class ImLoadQueue extends Thread {

		BlockingQueue<LazyLeafImage> bc = new LinkedBlockingQueue<LazyLeafImage>();

		Semaphore s = new Semaphore(1);

		boolean closing = false;

		public void add(LazyLeafImage l) {
			bc.add(l);
		}

		public void restart() {
			if (!isAlive())
				start();
			else if (s.availablePermits() <= 0)
				s.release();
			else
				System.out.println("boo");
		}

		public void hold() {
			try {

				// bc.put(holdDummy);
				bc.put(LazyLeafImage.closeDummy);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		public void clear() {
			bc.clear();
		}

		public int size() {
			return bc.size();
		}

		static ImLoadQueue createImLoadQueue() {
			ImLoadQueue im = new ImLoadQueue();
			im.setPriority(Thread.MIN_PRIORITY);
			im.s.acquireUninterruptibly();
			return im;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Thread#run()
		 */
		@Override
		public void run() {
			LazyLeafImage l;

			while (true)
				try {
					l = bc.take();
					if (l == LazyLeafImage.holdDummy)
						s.acquire();
					else if (l == LazyLeafImage.closeDummy) {
						closing = true;
						boolean cont = false;
						while (bc.size() > 0) {
							l = bc.take();
							if (l == LazyLeafImage.continueDummy)
								cont = true;
							if (l != LazyLeafImage.closeDummy && l != LazyLeafImage.holdDummy)
								l.loadLazy();
						}
						if (!cont) {
							LazyLeafImage.lq = null;
							System.out.println("done laoding images");
							break;
						}
						closing = false;

					} else
						l.loadLazy();
					// System.out.println("<i>");
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
		}

		public boolean isClosing() {
			return closing;
		}

	}

}
