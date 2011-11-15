/* RazorclawActivity.java - asus local backup root.
 *
 * Copyright (C) 2011 androidroot.mobi
 *
 * This file is part of Razorclaw.
 * 
 * Razorclaw is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU General Public License v2 as published by the 
 * Free Software Foundation.
 *
 * Razorclaw is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY 
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for 
 * more details.

 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 
 * 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */
package mobi.androidroot.razorclaw;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import java.io.*;
import java.util.Random;

public class RazorclawActivity extends Activity {
    /** Called when the activity is first created. */
    private static final String TAG = "Razorclaw";
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        Button btn = (Button)findViewById(R.id.rootButton);
        btn.setOnClickListener(btnListener);
        
		EditText output = (EditText)findViewById(R.id.outputText);
		
		String credits = "Created by Androidroot.mobi:\n\n" ;
        String[] creditPeople={"Bumble-Bee","IEF","kmdm","RaYmAn","lilstevie"};
        
        Random rgen = new Random();
        
        for (int i=0; i<creditPeople.length; i++) {
            int randomPosition = rgen.nextInt(creditPeople.length);
            String swap = creditPeople[i];
            creditPeople[i] = creditPeople[randomPosition];
            creditPeople[randomPosition] = swap;
        }
        
        for (int i=0; i < creditPeople.length; i++) {
        	credits = credits + creditPeople[i] + "\n";
        }
        
        String fullOutput = "";
        
        //Check if we're running a vulnerable version of the TF101 ROM.
        String romversion = android.os.Build.VERSION.INCREMENTAL;
        Log.i(TAG, "Current ROM: "+romversion);
        
        int rooted = checkRootFile("su");
        
        if (rooted==1) {
        	Log.e(TAG,"Already rooted.");
        	btn.setText("Exit");
        	output.setText("This device already has a correct SU binary (already rooted), unable to continue.\n\n" + credits);
        	btn.setOnClickListener(exitListener);
        } else { 
        	if (rooted==2) 
        		fullOutput = "Notice: This device has an incorrect SU binary. Be advised, we will overwrite this with a correct version.\n";

        	if (romversion.contains("8.6.5.19") || romversion.contains("8.6.6.19")) {
        		Log.i(TAG,"Tested rom version.");
        		fullOutput += "You are in luck. You are on a tested rom version! (" + romversion + ")\n\n" + credits + "\n\nWarning: by proceeding with this application, you accept that the creators are in no way responsible for any damage caused.";
        		output.setText(fullOutput);
        	} else {
        		if (checkRootFile("asus-backup")==1) {
        			Log.w(TAG,"Not tested, but has asus-backup.");
        			fullOutput += "This device ("+romversion+") may have the exploitable file, but is untested.\n\n"+ credits + "\n\nWarning: by proceeding with this application, you accept that the creators are in no way responsible for any damage caused.";
        		} else {
        			Log.e(TAG,"Could not locate asus-backup, or has wrong permissions.");
        			fullOutput = "This device lacks the needed file (bad luck).\n\n" + credits;
        			btn.setText("Exit");
        			btn.setOnClickListener(exitListener);
        		}
        		output.setText(fullOutput);
        	}
        	
        	Log.i(TAG,"Extracting razorclaw.");
        		
        	//Extract razorclaw binary
        	try {
        		InputStream nativebin = getAssets().open("razorclaw");
        		OutputStream out = new FileOutputStream("/data/data/mobi.androidroot.razorclaw/razorclaw");
        		byte[] buf = new byte[1024];
       			int len;
       			while ((len = nativebin.read(buf)) > 0) {
       				out.write(buf, 0, len);
       			}

       			nativebin.close();
       			out.close();
        	
       		} catch (IOException e) {
       			Log.e(TAG,e.toString());
       		}
        
       		//Chmod it so we can run it.
       		try {
       			Runtime.getRuntime().exec("/system/bin/chmod 755 /data/data/mobi.androidroot.razorclaw/razorclaw");
       		} catch (IOException e) {
       			Log.e(TAG,e.toString());
       		}
        }
    }
    
    private OnClickListener btnListener = new OnClickListener() {
    	public void onClick(View v) {
    		String s = null;
    		String progress = null;
    		EditText output = (EditText)findViewById(R.id.outputText);
            Button btn = (Button)findViewById(R.id.rootButton);
            btn.setEnabled(false);
            Log.i(TAG,"Rooting!");
    		Toast.makeText(getBaseContext(),"Rooting...",Toast.LENGTH_LONG).show();
    		try {
    			Process p = Runtime.getRuntime().exec("/data/data/mobi.androidroot.razorclaw/razorclaw --install");
    			BufferedReader stdInput = new BufferedReader(new 
    	                 InputStreamReader(p.getInputStream()));
    			while ((s = stdInput.readLine()) != null) {
    				if (progress == null) {
    					progress = s;
    				} else {
    					progress = progress + '\n' + s;
    				}
    				
    				output.setText(progress);
    			}
    			
    		} catch (IOException e) {
    			Log.e(TAG,e.toString());
            }
        	btn.setText("Exit");
        	btn.setEnabled(true);
    		btn.setOnClickListener(exitListener);
    	}
    	
    };
    
    private OnClickListener exitListener = new OnClickListener() {
    	public void onClick(View v) {
    		Log.i(TAG,"Exiting.");
    		Toast.makeText(getBaseContext(),"Exiting!",Toast.LENGTH_LONG).show();
    		System.exit(-1);
    	}
    };
    
    private int checkRootFile(String file){
    	String b;

    	try {
			//ls for the setuid binary
			Process newp = Runtime.getRuntime().exec("ls -la /system/xbin/" + file);		
			DataInputStream inStream = new DataInputStream(newp.getInputStream());
			
			b = inStream.readLine();
			if (b==null) return 0;
			
			String[] partPerms = b.split(" ");

			if ((partPerms[1].contains("root")) && (partPerms[0].contains("rws"))) {
				return 1;
			} else {
				return 2;
			}

    	} catch (Exception e){
    		Log.e(TAG,e.toString());
		}
	
		return 0;
    }
}
