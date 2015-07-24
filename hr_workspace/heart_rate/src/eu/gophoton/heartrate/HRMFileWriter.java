package eu.gophoton.heartrate;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Environment;
import android.util.Log;

public class HRMFileWriter {
	
	String fileName;
	List<String> cacheTimes = new ArrayList<String>();
	List<String> cacheData = new ArrayList<String>();
	File writeFile;
	public boolean usingInternalStorage = false;
	private Context context;
	int valuesInArray = 0;
	int index = 0;
	
	// Constructor - Initialize file name with current time stamp 
	@SuppressLint("SimpleDateFormat") 
	public HRMFileWriter(Context context, int values)
	{
		this.context = context;
		fileName = new SimpleDateFormat("yyyy_MM_dd_hh_mm'.csv'").format(new Date());
		
		this.valuesInArray = values;
	}
	
	// Create HeartRateData directory is it is not already present and create time stamped CSV file
	// for storing the data in for this run of the application.
	public boolean createDirectoryAndFile()
	{
		// Need to decide whether we save the file on internal or external storage.
		// If external storage is available it will be used. This will enable the user 
		// easily view the file and move it around with their SD card.
		// If external storage is not available then user Internal Storage
		if (isExternalStorageWritable())
		{
			String root = Environment.getExternalStorageDirectory().toString();
		    File myDir = new File(root + "/HeartRateData");    
		    
		    if (!myDir.exists() && !myDir.mkdirs()) {
		        Log.e(getClass().getSimpleName(), "Directory not created");
		        return false;
		    }
		    else
		    {
		    	writeFile = new File(myDir.getPath() + File.separator + fileName);
			    try {
					writeFile.createNewFile();
					usingInternalStorage = false;
				} catch (IOException e) {
					Log.e(getClass().getSimpleName(), "File not created");
					return false;
				}
		    	return true;
		    } 
		}
		else
		{
			writeFile = new File(context.getCacheDir() + File.separator
	                + fileName);
			try {
				writeFile.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			usingInternalStorage = true; 
			return true;
		}
	}
	
	public void writeData(Double time, Double data)
	{
		// Add data to cache.
		cacheTimes.add(String.valueOf(time));
		cacheData.add(String.valueOf(data));
		index++;
		
		// Check if cache needs to be emptied. (written to file)
		if(index == (valuesInArray-1))
		{
			emptyCacheToFile();
		}

	}
	
	// Write the data in the cache to the file. This method is called whenever the cache is full
	// or when the application is exiting.
	public void emptyCacheToFile()
	{
			PrintWriter out = null;
			try {
			    out = new PrintWriter(new BufferedWriter(new FileWriter(writeFile, true)));
			    for(int i = 0; i < cacheData.size(); ++i)
			    {
			    	out.println(cacheTimes.get(i).toString() + "," + cacheData.get(i).toString());
			    }
			    cacheTimes.clear();
			    cacheData.clear();
	       
			} catch (IOException e) {
				Log.e(getClass().getSimpleName(), "Exception in emptyCacheToFile()", e);
			} finally {
			    if (out != null) {
			        out.close();
			    }
			}
	}
	
	public String getDataFilePath()
	{
		return writeFile.getAbsolutePath();
	}
	
	public String getDataFileName()
	{
		return writeFile.getName();
	}
	
	// Checks if external storage is available for read and write 
	private boolean isExternalStorageWritable() {
		
	    String state = Environment.getExternalStorageState();
	    if (Environment.MEDIA_MOUNTED.equals(state)) {
	        return true;
	    }
	    return false;
	}

}

