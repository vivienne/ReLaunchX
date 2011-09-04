package com.harasoft.relaunch;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

public class ReLaunchApp extends Application {
	final String                            TAG = "ReLaunchApp";
	private HashMap<String, List<String[]>> m = new HashMap<String, List<String[]>>();
	private HashMap<String, Drawable>       icons;
    private List<HashMap<String, String>>   readers;
    private List<String>                    apps;

	// Miscellaneous public flags
	public Boolean                          askIfAmbiguous;
	
	// Icons
    public HashMap<String, Drawable> getIcons()       { return icons; }
    public void setIcons(HashMap<String, Drawable> i) { icons = i;    }

    // Applications
    public List<String>  getApps()      { return apps; }
    public void setApps(List<String> a) { apps = a;    }

    // Readers
    public List<HashMap<String, String>>  getReaders()      { return readers; }
    public void setReaders(List<HashMap<String, String>> r) { readers = r;    }
    public String readerName(String file) {
    	for (HashMap<String, String> r : readers) {
    		for (String key : r.keySet()) {
    			if (file.endsWith(key))
    				return r.get(key);
    		} 		
    	}
    	return "Nope";
    }
    public List<String> readerNames(String file) {
    	List<String> rc = new ArrayList<String>();
    	for (HashMap<String, String> r : readers) {
    		for (String key : r.keySet()) {
    			if (file.endsWith(key)  &&  !rc.contains(r.get(key)))
    				rc.add(r.get(key));
    		} 		
    	}
    	return rc;
    }
    public List<String> extNames() {
    	List<String> rc = new ArrayList<String>();
    	for (HashMap<String, String> r : readers) {
    		for (String key : r.keySet()) {
    			if (!rc.contains(key))
    				rc.add(key);
    		} 		
    	}
    	return rc;
    }

    /*
     * Misc lists management (list is identified by name)
     * --------------------------------------------------
     */

    // get list by name
	public List<String[]> getList(String name) {
		if (m.containsKey(name))
			return m.get(name);
		else
			return new ArrayList<String[]>();
	}

	// set list by name
	public void setList(String name, List<String[]> l) {
		m.put(name, l);
	}

	// dump all lists to log (debug)
	public void dumpLists()
	{
		for (String k : m.keySet())
		{
			Log.d(TAG, "LIST: \"" + k + "\"");
			for (String[] n : m.get(k))
				Log.d(TAG, "    \"" + n[0] + "\"; \"" + n[1] + "\"");
		}
		
	}

    // Add to list
    public void addToList(String listName, String dr, String fn, Boolean addToEnd)
    {
    	Log.d(TAG, "addToList(" + listName + ", " + dr + ":" + fn + ", " + addToEnd + ")");
    	addToList_internal(listName, dr, fn, addToEnd);
    }
    public void addToList(String listName, String fullName, Boolean addToEnd)
    {
    	Log.d(TAG, "addToList(" + listName + ", " + fullName + ", " + addToEnd + ")");
    	File f = new File(fullName); 	
    	if (!f.exists())
    		return;
    	addToList_internal(listName, f.getParent(), f.getName(), addToEnd);

    }
    public void addToList_internal(String listName, String dr, String fn, Boolean addToEnd)
    {
    	Log.d(TAG, "addToList_internal(" + listName + ", " + dr + ":" + fn  + ", " + addToEnd + ")");

		if (!m.containsKey(listName))
			m.put(listName, new ArrayList<String[]>());
		List<String[]> resultList = m.get(listName);

    	String [] entry = new String[] {dr, fn};
    	for (int i=0; i<resultList.size(); i++)
    	{
    		if (resultList.get(i)[0].equals(dr)  &&  resultList.get(i)[1].equals(fn))
    		{
    			resultList.remove(i);
    			break;
    		}
    	}
    	if (addToEnd)
    		resultList.add(entry);
    	else
    		resultList.add(0, entry);
    }

    // Remove from list
    public void removeFromList(String listName, String dr, String fn)
    {
    	Log.d(TAG, "removeFromList(" + listName + ", " + dr + ":" + fn + ")");
    	removeFromList_internal(listName, dr, fn);
    }
    public void removeFromList(String listName, String fullName)
    {
    	Log.d(TAG, "removeFromList(" + listName + ", " + fullName + ")");
    	File f = new File(fullName); 	
    	if (!f.exists())
    		return;
    	removeFromList_internal(listName, f.getParent(), f.getName());

    }
    public void removeFromList_internal(String listName, String dr, String fn)
    {
    	Log.d(TAG, "removeFromList_internal(" + listName + ", " + dr + ":" + fn  + ")");

		if (!m.containsKey(listName))
			return;
		List<String[]>  resultList = m.get(listName);
    	for (int i=0; i<resultList.size(); i++)
    	{
    		if (resultList.get(i)[0].equals(dr)  &&  resultList.get(i)[1].equals(fn))
    		{
    			resultList.remove(i);
    			return;
    		}
    	}
    }

    // If list contains
    public boolean contains(String listName, String dr, String fn)
    {
		if (!m.containsKey(listName))
			return false;
		List<String[]> resultList = m.get(listName);

    	for (int i=0; i<resultList.size(); i++)
    	{
    		if (resultList.get(i)[0].equals(dr)  &&  resultList.get(i)[1].equals(fn))
	    		return true;
    	}
    	return false;
    }
    
    // Read misc. lists
    public void readFile(String listName, String fileName, Boolean reverseOrder)
    {
    	FileInputStream fis = null;
    	try {
    		fis = openFileInput(fileName);
    	} catch (FileNotFoundException e) { }
    	if (fis != null)
    	{
    		String l = new String();

    		while (true)
    		{				
    			int rch;
    			try {
    				rch = (char)fis.read();
    			} catch (IOException e) {
    				break;
    			}
    			byte ch = (byte)rch;
    			if (ch == '\n'  ||  ch == -1)
    			{
    				if (!l.equals(""))
    				{
    					Log.d(TAG, "ADD \"" + l + "\"");
    					addToList(listName, l, reverseOrder);
    					l = new String();
    				}
    				if (ch == -1)
    					break;
    			}
    			else
    				l = l + (char)ch;
    		}

    		try {
    			fis.close();
    		} catch (IOException e) { }
    	}
    }
    
    // Save to file miscellaneous lists
    public void writeFile(String listName, String fileName, int maxEntries)
    {
		if (!m.containsKey(listName))
			return;

		List<String[]> resultList = m.get(listName);
    	FileOutputStream fos = null;
		try {
			fos = openFileOutput(fileName, Context.MODE_PRIVATE);
		} catch (FileNotFoundException e) {
			return;
		}
		for (int i=0; i<resultList.size(); i++)
		{
			if (i >= maxEntries)
				break;
			String line = resultList.get(i)[0] + "/" + resultList.get(i)[1] + "\n";
			try {
				fos.write(line.getBytes());
			} catch (IOException e) { }		
		}
		try {
			fos.close();
		} catch (IOException e) { }
    }

    // Remove file
    public boolean removeFile(String fullname)
    {
    	File f = new File(fullname);
    	return removeFile(f.getParent(), f.getName());
    }
    public boolean removeFile(String dr, String fn)
    {
    	boolean rc = false;
    	removeFromList("lastOpened", dr, fn);
    	removeFromList("favorites", dr, fn);
    	File f = new File(dr + "/" + fn);
    	if (f.exists())
    	{
    		try {
    			rc = f.delete();
    		} catch (SecurityException e) { }		
    	}
    	return rc;
    }
    
    // Remove directory
    public boolean removeDirectory(String dr, String fn)
    {
    	boolean rc = false;
    	String dname = dr + "/" + fn;
    	File   d = new File(dname);
    	File[] allEntries = d.listFiles();
    	for (File f : allEntries)
    	{
    		if (f.isDirectory())
    		{
    			Log.d(TAG, "removeDirectory(" + dname + ", " + f.getName() + ")");
    		    if (!removeDirectory(dname, f.getName()))
    		    	return false;
    		}
    		else
    		{
    			Log.d(TAG, "removeFile(" + dname + ", " + f.getName() + ")");
    			if (!removeFile(dname, f.getName()))
    				return false;
    		}
    	}
		try {
			rc = d.delete();
		} catch (SecurityException e) { }
    	return rc;
    }

    // common utility - get intent by label, null if not found
    private Intent getIntentByLabel(String label)
    {
        PackageManager pm = getPackageManager();;

        for (ApplicationInfo packageInfo : pm.getInstalledApplications(PackageManager.GET_META_DATA))
        {
        	if (label.equals(pm.getApplicationLabel(packageInfo)))
        		return pm.getLaunchIntentForPackage(packageInfo.packageName);
        }
        return null;
    }
    
    // common utility - return intent to launch reader by reader name and full file name. Null if not found
    public Intent launchReader(String name, String file)
    {
    	if (name.equals("Nomad Reader"))
    	{
            Intent i = new Intent();
            i.setAction(Intent.ACTION_VIEW); 
            i.setDataAndType(Uri.parse("file://" + file), "application/fb2"); 
            addToList("lastOpened", file, false);
            return i;
    	}
    	else if (name.equals("EBookDroid"))
    	{ 
    		Intent i = new Intent();
    		i.setAction(Intent.ACTION_VIEW);
    		i.setDataAndType(Uri.parse("file://" + file), "application/djvu");
            addToList("lastOpened", file, false);
    		return i; 
    	}
    	else
    	{
            Intent i = getIntentByLabel(name);
            if (i == null)
            	Toast.makeText(this, "Activity \"" + name + "\" not found!", Toast.LENGTH_SHORT).show();
            else
            {
            	i.setAction(Intent.ACTION_VIEW); 
            	i.setData(Uri.parse("file://" + file));
                addToList("lastOpened", file, false);
            	return i;
            }
    	}
    	return null;
    }
}