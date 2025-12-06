package com.groupunix.drivewireui;

import java.util.Hashtable;
import java.util.Map.Entry;
import java.util.concurrent.LinkedBlockingQueue;

import org.eclipse.swt.graphics.Image;

public class DiskTableUpdateThread implements Runnable
{
	private LinkedBlockingQueue<DiskTableUpdate> updates = new LinkedBlockingQueue<DiskTableUpdate>();
	private Hashtable<Integer, Hashtable<String, Object>> colval = new Hashtable<Integer, Hashtable<String, Object>>();
	
	@Override
	public void run()
	{
		Thread.currentThread().setName("dwuiDTUpdater-" + Thread.currentThread().getId());
		
		// Check if we're in JavaFX mode
		boolean isJavaFXMode = System.getProperty("drivewire.ui.mode") != null;
		
		DiskTableUpdate dtu;
		
		// Keep running: in JavaFX mode, shell is null but we still need updates
		// In SWT mode, check if shell is disposed
		while (isJavaFXMode || (MainWin.shell != null && !MainWin.shell.isDisposed()))
		{
			try
			{
				// get update
				dtu = this.updates.take();
				
				// apply to local cache
				if (!this.colval.containsKey(dtu.getDisk()))
					this.colval.put(dtu.getDisk(), new Hashtable<String, Object>());
					
				this.colval.get(dtu.getDisk()).put(dtu.getKey(), dtu.getValue());
				
				// no more updates, apply to table 
				if (this.updates.size() == 0)
				{
					System.out.println("Processing " + this.colval.size() + " disk updates...");
					for (Entry<Integer, Hashtable<String, Object>> diskentry :this.colval.entrySet())
					{
						for (Entry<String, Object> param : diskentry.getValue().entrySet())
						{
							// Use JavaFX Platform if available, otherwise SWT Display
							final int disk = diskentry.getKey();
							final String key = param.getKey();
							final Object value = param.getValue();
							
							System.out.println("Processing update: disk=" + disk + ", key=" + key + ", value=" + value);
							
							// Use reflection to avoid compile-time dependency on JavaFX
							try {
								Class<?> platformUtilsClass = Class.forName("com.groupunix.drivewireui.PlatformUtils");
								java.lang.reflect.Method runMethod = platformUtilsClass.getMethod("runOnFXThreadAsync", Runnable.class);
								final int finalDisk = disk;
								final String finalKey = key;
								final Object finalValue = value;
								runMethod.invoke(null, (Runnable)() -> {
									System.out.println("Calling updateDiskTableItem: disk=" + finalDisk + ", key=" + finalKey);
									MainWin.updateDiskTableItem(finalDisk, finalKey, finalValue);
								});
							} catch (Exception e) {
								System.err.println("Error calling PlatformUtils.runOnFXThreadAsync: " + e.getMessage());
								e.printStackTrace();
								// JavaFX not available, skip
							}
							
						}
						
						diskentry.getValue().clear();
					}
					
					// Use reflection to avoid compile-time dependency on JavaFX
					try {
						Class<?> platformUtilsClass = Class.forName("com.groupunix.drivewireui.PlatformUtils");
						java.lang.reflect.Method runMethod = platformUtilsClass.getMethod("runOnFXThreadAsync", Runnable.class);
						runMethod.invoke(null, (Runnable)() -> {
							MainWin.updateDiskTabs();
						});
					} catch (Exception e) {
						// JavaFX not available, skip
					}
					
					// clear cache
					colval.clear();
					
				}
				
			} 
			catch (InterruptedException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		
		System.out.println("dtu die");
	}

	public void addUpdate(int disk, String key, String val)
	{
		this.updates.add(new DiskTableUpdate(disk, key, val));
	}

	public void addUpdate(int disk, String key, Image val)
	{
		this.updates.add(new DiskTableUpdate(disk, key, val));
	}

	public void addUpdate(int disk, String key, Integer val)
	{
		this.updates.add(new DiskTableUpdate(disk, key, val));
	}

}
