package uk.ac.liverpool.fab4.jreality;



import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.IntrospectionException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import de.jtem.beans.Inspector;


/** <p>This class is the entry point to the package <code>de.jtem.beans</code>. 
 * Instances are {@link JPanel}s.
 * 
 * <p>Call {@link #setObject(Object)} with the JavaBean&trade; 
 * conform object you like to inspect. The variants of 
 * <code>setObject()</code> allow to provide properties that should not appear in the GUI 
 * and the name of an 
 * update method. The update method is added as a special Change listener. 
 * 
 * <p>The displayed values of the objects properties are reread from the object every 500ms. If thats
 * to expensive you may turn of auto refresh via {@link #setAutoRefresh(boolean)}. 
 * 
 * <p>You may reuse an InspectorPanel by calling {@link #setObject(Object)} with other objects. That
 * also helps to keep the costs of auto refresh low.
 * 
 */
public class InspectorPanel extends JPanel  {
	
	private static final long serialVersionUID = 1L;
	
	public static final String DEFAULT_INFORM_LISTENERS_BUTTON_TEXT="auto update";
	public static final boolean DEFAULT_INFORM_LISTENERS_BUTTON_VISIBLE=false;
	public static final boolean DEFAULT_INFORM_LISTENERS=true;
	
	public static final String DEFAULT_REFESH_BUTTON_TEXT="refresh";
	public static final boolean DEFAULT_REFESH_BUTTON_VISIBLE=false;
	public static final boolean DEFAULT_AUTO_REFRESH_ENABLED=!DEFAULT_REFESH_BUTTON_VISIBLE;
	public static final int DEFAULT_AUTO_REFRESH_TIMER_DELAY_MS=500;

	private List<ChangeListener> changeListeners= new CopyOnWriteArrayList<ChangeListener>();
	
	private Inspector inspector;
	private Object object;
	private ChangeListener updateMethodListener;
	
	private final Box topBox = new Box(BoxLayout.LINE_AXIS);
	private final Component bottomBox=Box.createVerticalGlue();
	private final JButton refreshButton= new JButton(DEFAULT_REFESH_BUTTON_TEXT);
	private final JToggleButton informListenersButton=new JToggleButton(DEFAULT_INFORM_LISTENERS_BUTTON_TEXT);
//	private final Timer refreshTimer 
//	= new Timer(DEFAULT_AUTO_REFRESH_TIMER_DELAY_MS, new ActionListener() {
//		public void actionPerformed(ActionEvent e) {
//			refresh();
//		}
//	});
//	
	
	public InspectorPanel() {
		this(DEFAULT_REFESH_BUTTON_VISIBLE);
	}
  
	public InspectorPanel(boolean useRefreshButton) {
		super(new BorderLayout());
	
		refreshButton.setForeground(Color.red);
		refreshButton.setMaximumSize(new java.awt.Dimension(1234,20));
		refreshButton.setMargin(new Insets(0, 0, 0, 0));
		refreshButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				refresh();
			}
		});
		refreshButton.setVisible(DEFAULT_REFESH_BUTTON_VISIBLE);
		topBox.add(refreshButton);
		
		informListenersButton.setSelected(DEFAULT_INFORM_LISTENERS);
		informListenersButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (informListenersButton.isSelected())
					fireStateChanged();
			}
		});
		informListenersButton.setMaximumSize(new java.awt.Dimension(1234,20));
		informListenersButton.setMargin(new Insets(0, 0, 0, 0));
		informListenersButton.setVisible(DEFAULT_INFORM_LISTENERS_BUTTON_VISIBLE);
		topBox.add(informListenersButton);
		
		add(topBox,BorderLayout.NORTH);
		add(bottomBox,BorderLayout.SOUTH);
	
		//setAutoRefresh(DEFAULT_AUTO_REFRESH_ENABLED);
	}
	
	private void updateRefreshButtonTooltip() {
		if (refreshButton == null) 
			return;

		Class<?> clazz=object.getClass();
		String name = null;
		try {
			name = (String) clazz.getMethod("getDisplayName",(Class<Object>[]) null).invoke(object);
		} catch(Exception e) {
			try {
				name = (String) clazz.getMethod("getName").invoke(object);
			} catch(Exception f) {}
		}
		if (name == null) {
			String[] className = object.getClass().getName().split("[.]");
			name = className[className.length -1]+"@"+(object.hashCode());
		}
		refreshButton.setToolTipText("Reread values from: "+name);
	}
	

	private void fireStateChanged() {
		for (ChangeListener changeListener : changeListeners) {
			changeListener.stateChanged(new ChangeEvent(object));
		}
	}
	

	public void setInformListenersButtonText(String text) {
		if (text.equals(getInformListenersButtonText())) return;
		informListenersButton.setText(text);
		revalidate();
		doLayout();
		repaint();
	}
	
	public String getInformListenersButtonText() {
		return informListenersButton.getText();
	}
	
	public boolean isInformListenersButtonVisible() {
		return informListenersButton.isVisible();
	}

	public void setInformListenersButtonVisible(boolean b) {
		if (b == isInformListenersButtonVisible());
		informListenersButton.setVisible(b);
		revalidate();
		doLayout();
		repaint();
	}

	public boolean isInformListeners() {
		return informListenersButton.isSelected();
	}

	public void setInformListeners(boolean b) {
		if (b == isInformListeners()) return;
		informListenersButton.setSelected(b);
		if (b) fireStateChanged();
	}

	public String getRefeshButtonText() {
		return refreshButton.getText();
	}
	
	public void setRefeshButtonText(String text) {
		refreshButton.setText(text);
		revalidate();
		doLayout();
		repaint();
	}

	public boolean isRefeshButtonVisible() {
		return refreshButton.isVisible();
	}

	public void setRefeshButtonVisible(boolean b) {
		refreshButton.setVisible(b);
	}

//	public void setAutoRefresh(boolean b) {
//		if (refreshTimer.isRunning() == b) return;
//		if (b) refreshTimer.start();
//		else refreshTimer.stop();
//	}
//	
//	public boolean isAutoRefresh() {
//		return refreshTimer.isRunning();
//	}
	

	public void refresh() {
		if (inspector!=null) inspector.refresh();
	}

	public void setObject(Object o) {
		setObject(o, null, null);
	}
	
	public void setObject(Object o, String updateMethodName) {
		setObject(o, updateMethodName, null);
	}
	
	public void setObject(Object o, Collection<String> excludedPropertyNames) {
		setObject(o, null, excludedPropertyNames);
	}
	
	public void setObject(Object o, String updateMethodName, Collection<String> excludedPropertyNames) {
		object = o;
		
		if (inspector != null) {
			if (o==inspector.getObject() && (excludedPropertyNames == null ||
					(excludedPropertyNames != null &&
					excludedPropertyNames.equals(inspector.getExcludedPropertyNames())))
			) return;
		}
		
		Inspector oldInspector=inspector;
		if(o!=null) {
			try {
				inspector = new Inspector(o, excludedPropertyNames);
			} catch (IntrospectionException e) {
				e.printStackTrace();
				inspector=oldInspector;
				return;
			};
			inspector.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					fireStateChanged();
				}
			});
			
			if (oldInspector!=null) remove(oldInspector);
			add(inspector, BorderLayout.CENTER);
			
			setUpdateMethod(updateMethodName);
			updateRefreshButtonTooltip();
					
			revalidate();
			doLayout();
			repaint();
		}
	}

	public Object getObject() {
		return object;
	}
	
	
	public void addChangeListener(ChangeListener listener) {
		changeListeners.add(listener);
	}
	
	public void removeChangeListener(ChangeListener listener) {
		changeListeners.remove(listener);
	}
	
	public void setUpdateMethod(String updateMethodName) {
		if (updateMethodName==null) return;
		
		try {
			final Method updateMethod = object.getClass().getMethod(updateMethodName);
			if (updateMethod.getParameterTypes().length!=0)
				throw new IllegalArgumentException();
			removeChangeListener(updateMethodListener);
			updateMethodListener = new ChangeListener() {
				
				public void stateChanged(ChangeEvent e) {
					try {
						updateMethod.invoke(InspectorPanel.this.getObject());
					} catch (IllegalArgumentException e1) {
						e1.printStackTrace();
					} catch (IllegalAccessException e1) {
						e1.printStackTrace();
					} catch (InvocationTargetException e1) {
						e1.printStackTrace();
					}
				}
			};
			addChangeListener(updateMethodListener);
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			throw new IllegalArgumentException(
					"The class \""+object.getClass().getName()+"\"" +
					"does not declare a method \""+updateMethodName +"\".");
		}
	}
	
	public void removeUpdateMethod() {
		removeChangeListener(updateMethodListener);
	}
	
	
}

