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
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import java.io.*;
import java.util.Random;

public class RazorclawActivity extends Activity {
    /** Called when the activity is first created. */
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
        int rooted = checkRootFile("su");
        
        if (rooted==1) {
        	btn.setText("Exit");
        	output.setText("This device already has a correct SU binary (already rooted), unable to continue.\n\n" + credits);
        	btn.setOnClickListener(exitListener);
        } else { 
        	if (rooted==2) 
        		fullOutput = "Notice: This device has an incorrect SU binary. Be advised, we will overwrite this with a correct version.\n";

        	if (!assertVersion()) {
        		if (checkRootFile("asus-backup")==1)
        			fullOutput += "This device may have the exploitable file, but is untested.\n\n"+ credits + "\n\nWarning: by proceeding with this application, you accept that the creators are in no way responsible for any damage caused.";
        		else {
        		fullOutput = "This device lacks the needed file (bad luck).\n\n" + credits;
            	btn.setText("Exit");
        		btn.setOnClickListener(exitListener);
        		}
        		
        		output.setText(fullOutput);
        		
        	} else { 
        		fullOutput += "You are in luck. You are on a tested rom version!\n\n" + credits + "\n\nWarning: by proceeding with this application, you accept that the creators are in no way responsible for any damage caused.";
        
        		output.setText(fullOutput);
        	
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
        			throw new RuntimeException(e); 
        		}
        
        		//Chmod it so we can run it.
        		try {
        			Runtime.getRuntime().exec("/system/bin/chmod 755 /data/data/mobi.androidroot.razorclaw/razorclaw");
        		} catch (IOException e) {
        			e.printStackTrace();
        			System.exit(-1);
        		}
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
                e.printStackTrace();
                System.exit(-1);
            }
        	btn.setText("Exit");
        	btn.setEnabled(true);
    		btn.setOnClickListener(exitListener);
    	}
    	
    };
    
    private OnClickListener exitListener = new OnClickListener() {
    	public void onClick(View v) {
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
			e.printStackTrace();
			System.exit(-1);
		}
	
		return 0;
    }
    
    private boolean assertVersion(){
    	String incremental = android.os.Build.VERSION.INCREMENTAL;
    	incremental = incremental.replaceAll(".*_epad-", "");
    	incremental = incremental.replaceAll("-20[0-9]*","");
    	incremental = incremental.replace("8.6.", "");
    	String [] versionNumbers = incremental.split("\\.");
    	int MIN_FIRST_V_NUMBER = 5, ACTUAL_FIRST_V_NUMBER = Integer.parseInt(versionNumbers[0]);
    	int MIN_SECOND_V_NUMBER = 18, ACTUAL_SECOND_V_NUMBER = Integer.parseInt(versionNumbers[1]);
    	
    	return (ACTUAL_FIRST_V_NUMBER >= MIN_FIRST_V_NUMBER && ACTUAL_SECOND_V_NUMBER >= MIN_SECOND_V_NUMBER);
    }
}
